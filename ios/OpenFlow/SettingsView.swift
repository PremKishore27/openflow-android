import AVFoundation
import Speech
import SwiftUI
import UIKit

struct SettingsView: View {
    @State private var spoken = SharedStore.spoken
    @State private var dictionary = SharedStore.dictionaryText
    @State private var micOn = AVAudioApplication.shared.recordPermission == .granted
    @Environment(\.openURL) private var openURL
    @AppStorage("onboarded") private var onboarded = true
    @State private var updateStatus = "Version \(Updates.installedVersion)"
    @State private var updateInfo: Updates.Info?
    @State private var showUpdate = false

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
                    Button("Show the setup guide again") { onboarded = false }
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
                Section {
                    Button {
                        updateStatus = "Checking GitHub…"
                        Task {
                            switch await Updates.check() {
                            case .upToDate: updateStatus = "Version \(Updates.installedVersion) · you're up to date ✓"
                            case .failed(let reason): updateStatus = "Couldn't check: \(reason)"
                            case .newer(let info):
                                updateStatus = "Version \(Updates.display(info.tag)) is available"
                                updateInfo = info
                                showUpdate = true
                            }
                        }
                    } label: {
                        VStack(alignment: .leading, spacing: 2) {
                            Text("Check for updates")
                            Text(updateStatus).font(.footnote).foregroundStyle(.secondary)
                        }
                    }
                    Button("Source code and releases") { openURL(Updates.releasesPage) }
                } header: {
                    Text("About")
                }
            }
            .alert("Update available", isPresented: $showUpdate, presenting: updateInfo) { info in
                Button("Open download page") { openURL(info.pageURL) }
                Button("Later", role: .cancel) {}
            } message: { info in
                Text("Version \(Updates.display(info.tag)) is out. You have \(Updates.installedVersion). Download OpenFlow-iOS-unsigned.ipa from the page, then install it with Sideloadly or AltStore, the same way as before.")
            }
            .navigationTitle("Settings")
            .onChange(of: spoken) { _, new in SharedStore.spoken = new }
            .onChange(of: dictionary) { _, new in SharedStore.dictionaryText = new }
        }
    }
}
