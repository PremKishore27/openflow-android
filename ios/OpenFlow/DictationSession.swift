import AVFoundation
import Combine
import Foundation
import Speech

/// Records speech, transcribes it on the device when iOS allows, and hands the cleaned text to the keyboard.
/// Uses only Apple frameworks: no third-party code.
@MainActor
final class DictationSession: ObservableObject {
    @Published private(set) var isRecording = false
    @Published private(set) var transcript = ""
    @Published private(set) var finished: String?
    @Published private(set) var level: CGFloat = 0
    @Published var message: String?

    private let audio = AVAudioEngine()
    private var request: SFSpeechAudioBufferRecognitionRequest?
    private var task: SFSpeechRecognitionTask?
    private let recognizer = SFSpeechRecognizer(locale: Locale.current)
    private var delivered = false

    /// Called when the keyboard opens OpenFlow with openflow://record.
    func requestStart() {
        start()
    }

    func toggle() {
        isRecording ? stop() : start()
    }

    func start() {
        AVAudioApplication.requestRecordPermission { [weak self] granted in
            Task { @MainActor in
                guard let self else { return }
                guard granted else { self.message = "Microphone is off. Turn it on in Settings."; return }
                SFSpeechRecognizer.requestAuthorization { status in
                    Task { @MainActor in
                        guard status == .authorized else { self.message = "Speech recognition is off. Turn it on in Settings."; return }
                        self.beginRecording()
                    }
                }
            }
        }
    }

    private func beginRecording() {
        guard let recognizer, recognizer.isAvailable else { message = "Speech recognition isn't available right now."; return }
        do {
            let session = AVAudioSession.sharedInstance()
            try session.setCategory(.record, mode: .measurement, options: .duckOthers)
            try session.setActive(true, options: .notifyOthersOnDeactivation)

            let req = SFSpeechAudioBufferRecognitionRequest()
            req.shouldReportPartialResults = true
            if recognizer.supportsOnDeviceRecognition { req.requiresOnDeviceRecognition = true }

            let input = audio.inputNode
            let format = input.outputFormat(forBus: 0)
            input.removeTap(onBus: 0)
            input.installTap(onBus: 0, bufferSize: 1024, format: format) { [weak self] buffer, _ in
                req.append(buffer)
                let level = DictationSession.rmsLevel(buffer)
                Task { @MainActor in self?.level = level }
            }
            audio.prepare()
            try audio.start()

            delivered = false
            transcript = ""
            finished = nil
            message = nil
            request = req
            isRecording = true
            task = recognizer.recognitionTask(with: req) { [weak self] result, error in
                Task { @MainActor in
                    guard let self else { return }
                    if let result {
                        self.transcript = result.bestTranscription.formattedString
                        if result.isFinal { self.deliver() }
                    }
                    if error != nil { self.deliver() }
                }
            }
        } catch {
            message = "Couldn't start recording. Try again."
            cleanupAudio()
        }
    }

    func stop() {
        guard isRecording else { return }
        isRecording = false
        cleanupAudio()
        request?.endAudio()
        // Safety net: if no final result arrives, deliver what we have.
        Task { @MainActor in
            try? await Task.sleep(for: .seconds(2.5))
            self.deliver()
        }
    }

    private func cleanupAudio() {
        audio.stop()
        audio.inputNode.removeTap(onBus: 0)
        level = 0
        try? AVAudioSession.sharedInstance().setActive(false, options: .notifyOthersOnDeactivation)
    }

    private func deliver() {
        guard !delivered else { return }
        delivered = true
        task?.cancel()
        task = nil
        request = nil
        isRecording = false
        let raw = transcript.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !raw.isEmpty else { message = "Didn't catch that. Try again."; return }
        let cleaned = Cleanup.clean(raw, dictionary: SharedStore.dictionary, spoken: SharedStore.spoken)
        SharedStore.saveResult(cleaned)
        finished = cleaned
    }

    private nonisolated static func rmsLevel(_ buffer: AVAudioPCMBuffer) -> CGFloat {
        guard let data = buffer.floatChannelData?[0], buffer.frameLength > 0 else { return 0 }
        var sum: Float = 0
        for i in 0..<Int(buffer.frameLength) { sum += data[i] * data[i] }
        let rms = (sum / Float(buffer.frameLength)).squareRoot()
        return CGFloat(min(1, max(0, (20 * log10(max(rms, 1e-6)) + 50) / 50)))
    }
}
