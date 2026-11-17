import AVFoundation
import Speech
import SwiftUI
import UIKit

struct SettingsView: View {
    @State private var spoken = SharedStore.spoken
    @State private var dictionary = SharedStore.dictionaryText
    @State private var micOn = AVAudioApplication.shared.recordPermission == .granted
    @Environment(\.openURL) private var openURL

    var body: some View {
        NavigationStack {
            Form {
                Section {
                    LabeledContent("Microphone", value: micOn ? "On" : "Off")
                    Button("Allow microphone") {
                        AVAudioApplication.requestRecordPermission { _ in
                            Task { @MainActor in micOn = AVAudioApplication.shared.recordPermission == .granted }
                        }
                    }
                    Button("Open iOS Settings") {
                        if let url = URL(string: UIApplication.openSettingsURLString) { openURL(url) }
                    }
                } header: {
                    Text("Setup")
                } footer: {
                    Text("Add the keyboard: Settings → General → Keyboard → Keyboards → Add New Keyboard → OpenFlow, then turn on Allow Full Access.")
                }

                Section {
                    Toggle("Spoken punctuation", isOn: $spoken)
                } footer: {
                    Text("Say “comma”, “full stop” or “new line”.")
                }

                Section {
                    TextEditor(text: $dictionary)
                        .frame(minHeight: 120)
                        .font(.body.monospaced())
                } header: {
                    Text("Dictionary")
                } footer: {
                    Text("One “heard = written” pair per line, for example: open flow = OpenFlow")
                }
            }
            .navigationTitle("Settings")
            .onChange(of: spoken) { _, new in SharedStore.spoken = new }
            .onChange(of: dictionary) { _, new in SharedStore.dictionaryText = new }
        }
    }
}
