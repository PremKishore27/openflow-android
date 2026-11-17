import SwiftUI

@main
struct OpenFlowApp: App {
    @StateObject private var session = DictationSession()
    @AppStorage("onboarded") private var onboarded = false
    @State private var showRecorder = false

    var body: some Scene {
        WindowGroup {
            Group {
                if onboarded {
                    TabView {
                        RecordView(session: session)
                            .tabItem { Label("Dictate", systemImage: "mic.fill") }
                        SettingsView()
                            .tabItem { Label("Settings", systemImage: "gearshape.fill") }
                    }
                    .tint(.blue)
                } else {
                    OnboardingView(session: session) { onboarded = true }
                }
            }
            // The keyboard's mic key opens openflow://record: show the recorder over whatever is on screen.
            .fullScreenCover(isPresented: $showRecorder) {
                RecordView(session: session, onClose: { showRecorder = false })
            }
            .onOpenURL { url in
                if url.host == "record" {
                    session.requestStart()
                    showRecorder = true
                }
            }
        }
    }
}
