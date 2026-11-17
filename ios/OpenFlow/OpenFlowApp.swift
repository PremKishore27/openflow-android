import SwiftUI

@main
struct OpenFlowApp: App {
    @StateObject private var session = DictationSession()

    var body: some Scene {
        WindowGroup {
            TabView {
                RecordView(session: session)
                    .tabItem { Label("Dictate", systemImage: "mic.fill") }
                SettingsView()
                    .tabItem { Label("Settings", systemImage: "gearshape.fill") }
            }
            .tint(.blue)
            .onOpenURL { url in
                // openflow://record is sent by the keyboard's mic key: start listening straight away.
                if url.host == "record" { session.requestStart() }
            }
        }
    }
}
