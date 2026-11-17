import AVFoundation
import Speech
import UIKit

/// Checks and requests everything OpenFlow needs, so the onboarding can show live ✓ marks.
enum Setup {
    static var micGranted: Bool { AVAudioApplication.shared.recordPermission == .granted }
    static var speechGranted: Bool { SFSpeechRecognizer.authorizationStatus() == .authorized }

    /// Asks for the microphone, then speech recognition. Calls `done` on the main queue.
    static func requestPermissions(_ done: @escaping () -> Void) {
        AVAudioApplication.requestRecordPermission { _ in
            SFSpeechRecognizer.requestAuthorization { _ in
                DispatchQueue.main.async(execute: done)
            }
        }
    }

    /// True when the OpenFlow keyboard has been added in Settings.
    static var keyboardAdded: Bool {
        UITextInputMode.activeInputModes.contains { mode in
            let sel = NSSelectorFromString("identifier")
            guard mode.responds(to: sel), let id = mode.value(forKey: "identifier") as? String else { return false }
            return id.hasPrefix("com.openflow.ios")
        }
    }

    static func openSettings() {
        if let url = URL(string: UIApplication.openSettingsURLString) { UIApplication.shared.open(url) }
    }
}

enum UseCase: String, CaseIterable, Identifiable {
    case messages, email, ai, notes, other
    var id: String { rawValue }
    var title: String {
        switch self {
        case .messages: return "Sending messages"
        case .email: return "Writing emails"
        case .ai: return "Chatting to AI tools"
        case .notes: return "Taking notes"
        case .other: return "Something else"
        }
    }
    var symbol: String {
        switch self {
        case .messages: return "message.fill"
        case .email: return "envelope.fill"
        case .ai: return "sparkles"
        case .notes: return "note.text"
        case .other: return "ellipsis.circle.fill"
        }
    }
}
