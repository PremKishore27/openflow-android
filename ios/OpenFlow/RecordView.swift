import SwiftUI
import UIKit

struct RecordView: View {
    @ObservedObject var session: DictationSession
    var onClose: (() -> Void)? = nil
    @State private var copied = false

    var body: some View {
        NavigationStack {
            VStack(spacing: 28) {
                Spacer()
                Text(statusLine)
                    .font(.headline)
                    .foregroundStyle(.secondary)
                    .multilineTextAlignment(.center)
                    .padding(.horizontal, 32)
                    .frame(minHeight: 60)

                Button {
                    session.toggle()
                } label: {
                    ZStack {
                        Circle()
                            .fill(session.isRecording ? Color.red.opacity(0.15) : Color.blue.opacity(0.12))
                            .frame(width: 200 + session.level * 40, height: 200 + session.level * 40)
                            .animation(.easeOut(duration: 0.1), value: session.level)
                        Circle()
                            .fill(session.isRecording ? Color.red : Color.blue)
                            .frame(width: 130, height: 130)
                        Image(systemName: session.isRecording ? "stop.fill" : "mic.fill")
                            .font(.system(size: 46, weight: .semibold))
                            .foregroundStyle(.white)
                    }
                }
                .buttonStyle(.plain)
                .accessibilityLabel(session.isRecording ? "Stop dictation" : "Start dictation")

                if !session.transcript.isEmpty {
                    Text(session.transcript)
                        .font(.body)
                        .padding()
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .background(.regularMaterial, in: RoundedRectangle(cornerRadius: 16))
                        .padding(.horizontal)
                }

                if let finished = session.finished {
                    VStack(spacing: 12) {
                        Text(finished)
                            .font(.body)
                            .padding()
                            .frame(maxWidth: .infinity, alignment: .leading)
                            .background(.regularMaterial, in: RoundedRectangle(cornerRadius: 16))
                        Button(copied ? "Copied" : "Copy") {
                            UIPasteboard.general.string = finished
                            copied = true
                        }
                        .buttonStyle(.borderedProminent)
                    }
                    .padding(.horizontal)
                }

                Spacer()
                if let message = session.message {
                    Text(message).font(.footnote).foregroundStyle(.orange).padding(.bottom)
                }
            }
            .navigationTitle("OpenFlow")
            .toolbar {
                if let onClose {
                    ToolbarItem(placement: .topBarTrailing) { Button("Done", action: onClose).bold() }
                }
            }
        }
    }

    private var statusLine: String {
        if session.isRecording { return "Listening… tap the red button when you're done." }
        if session.finished != nil {
            return "Done. Switch back to your app. Your text will appear in the keyboard."
        }
        return "Tap the mic, speak, then tap stop."
    }
}
