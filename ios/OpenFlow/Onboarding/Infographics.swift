import SwiftUI

/// "Dictation is great for notes": a mock Notes page that was typed by voice.
struct NotesMock: View {
    var body: some View {
        Card {
            VStack(alignment: .leading, spacing: 12) {
                HStack {
                    Image(systemName: "note.text").foregroundStyle(.yellow)
                    Text("Notes").font(.subheadline.weight(.semibold))
                    Spacer()
                    Image(systemName: "mic.fill").font(.caption.bold()).foregroundStyle(.white)
                        .padding(7).background(Theme.gradient, in: Circle())
                }
                Divider()
                Text("Project kickoff").font(.title3.bold())
                VStack(alignment: .leading, spacing: 8) {
                    bullet("Launch the beta on Friday")
                    bullet("Maya sends the budget by Monday")
                    bullet("Book the venue before the 12th")
                }
                HStack(spacing: 6) {
                    Image(systemName: "waveform").foregroundStyle(Theme.blue)
                    Text("Typed by voice, no keyboard needed").font(.caption).foregroundStyle(.secondary)
                }
            }
        }
    }

    private func bullet(_ s: String) -> some View {
        HStack(alignment: .top, spacing: 8) {
            Circle().fill(Theme.blue).frame(width: 6, height: 6).padding(.top, 7)
            Text(s).font(.body)
        }
    }
}

/// "Works in any app": app tiles flowing into the OpenFlow keyboard.
struct AnyAppMock: View {
    private let apps: [(String, String, Color)] = [
        ("Messages", "message.fill", .green),
        ("Mail", "envelope.fill", .blue),
        ("Notes", "note.text", .yellow),
        ("AI chat", "sparkles", .purple),
    ]

    var body: some View {
        VStack(spacing: 18) {
            HStack(spacing: 14) {
                ForEach(apps, id: \.0) { app in
                    VStack(spacing: 6) {
                        Image(systemName: app.1)
                            .font(.title2).foregroundStyle(.white)
                            .frame(width: 58, height: 58)
                            .background(app.2.gradient, in: RoundedRectangle(cornerRadius: 14, style: .continuous))
                        Text(app.0).font(.caption2).foregroundStyle(.secondary)
                    }
                }
            }
            Image(systemName: "arrow.down").font(.title3.bold()).foregroundStyle(.secondary)
            Card {
                HStack(spacing: 12) {
                    LogoMark(size: 44)
                    VStack(alignment: .leading, spacing: 2) {
                        Text("OpenFlow keyboard").font(.headline)
                        Text("Tap the mic. Speak. Done.").font(.subheadline).foregroundStyle(.secondary)
                    }
                    Spacer()
                }
            }
        }
    }
}

/// "Perfectly formatted text": raw speech on top, the cleaned text below.
struct BeforeAfterMock: View {
    var body: some View {
        VStack(spacing: 14) {
            Card {
                VStack(alignment: .leading, spacing: 8) {
                    Label("You say", systemImage: "mic.fill").font(.caption.weight(.semibold)).foregroundStyle(.secondary)
                    Text("“um so I was thinking we should uh meet at 3, no wait, 4 period”")
                        .font(.body).italic().foregroundStyle(.secondary)
                }
            }
            Image(systemName: "sparkles").font(.title2).foregroundStyle(Theme.gradient)
            Card {
                VStack(alignment: .leading, spacing: 8) {
                    Label("OpenFlow types", systemImage: "checkmark.circle.fill").font(.caption.weight(.semibold)).foregroundStyle(.green)
                    Text("So I was thinking we should meet at 4.").font(.body.weight(.medium))
                }
            }
            Text("Fillers, repeats and corrections are removed. Say “period”, “comma” or “new line” to add punctuation.")
                .font(.footnote).foregroundStyle(.secondary).multilineTextAlignment(.center)
        }
    }
}

/// "Add the keyboard": the two switches to turn on, drawn like iOS Settings.
struct KeyboardStepsMock: View {
    var body: some View {
        VStack(spacing: 14) {
            HStack(spacing: 8) {
                Text("Settings").foregroundStyle(Theme.blue)
                Image(systemName: "chevron.right").font(.caption2).foregroundStyle(.secondary)
                Text("OpenFlow").foregroundStyle(Theme.blue)
                Image(systemName: "chevron.right").font(.caption2).foregroundStyle(.secondary)
                Text("Keyboards").bold()
            }
            .font(.subheadline)

            Card {
                VStack(spacing: 0) {
                    row(number: 1, title: "OpenFlow")
                    Divider().padding(.leading, 44)
                    row(number: 2, title: "Allow Full Access")
                }
                .padding(.vertical, -6)
            }
            Text("Full Access lets the keyboard receive your dictated text from the app. OpenFlow sends nothing over the internet.")
                .font(.footnote).foregroundStyle(.secondary).multilineTextAlignment(.center)
        }
    }

    private func row(number: Int, title: String) -> some View {
        HStack(spacing: 12) {
            Text("\(number)").font(.caption.bold()).foregroundStyle(.white)
                .frame(width: 24, height: 24).background(Theme.gradient, in: Circle())
            Text(title).font(.body)
            Spacer()
            SwitchOn()
        }
        .padding(.vertical, 10)
    }
}

/// "Allow the microphone": a gentle preview of the permission prompt.
struct MicPermissionMock: View {
    var body: some View {
        VStack(spacing: 20) {
            Image(systemName: "mic.fill").font(.system(size: 44, weight: .semibold)).foregroundStyle(.white)
                .frame(width: 96, height: 96).background(Theme.gradient, in: Circle())
            Card {
                VStack(spacing: 12) {
                    Text("“OpenFlow” would like to access the microphone")
                        .font(.headline).multilineTextAlignment(.center)
                    Text("OpenFlow listens only while you dictate.")
                        .font(.subheadline).foregroundStyle(.secondary)
                    Divider()
                    HStack {
                        Text("Don't Allow").frame(maxWidth: .infinity).foregroundStyle(Theme.blue)
                        Divider().frame(height: 22)
                        Text("OK").bold().frame(maxWidth: .infinity).foregroundStyle(Theme.blue)
                    }
                    .font(.body)
                }
                .frame(maxWidth: .infinity)
            }
            .padding(.horizontal, 20)
        }
    }
}

/// "Try it": the bottom keyboard row with the globe key pulsing.
struct GlobeKeyMock: View {
    @State private var pulse = false

    var body: some View {
        VStack(spacing: 14) {
            HStack(spacing: 8) {
                key("123", width: 54)
                ZStack {
                    Circle().stroke(Theme.blue.opacity(0.5), lineWidth: 3)
                        .frame(width: 46, height: 46).scaleEffect(pulse ? 1.35 : 1).opacity(pulse ? 0 : 1)
                    Image(systemName: "globe").font(.title3).foregroundStyle(.white)
                        .frame(width: 46, height: 40).background(Theme.gradient, in: RoundedRectangle(cornerRadius: 8))
                }
                key("space", width: nil)
                key("return", width: 70)
            }
            .padding(10)
            .background(Color(.tertiarySystemFill), in: RoundedRectangle(cornerRadius: 14, style: .continuous))
            Label("Tap and hold the globe, then choose OpenFlow", systemImage: "hand.tap.fill")
                .font(.subheadline).foregroundStyle(.secondary)
        }
        .onAppear { withAnimation(.easeOut(duration: 1.3).repeatForever(autoreverses: false)) { pulse = true } }
    }

    private func key(_ label: String, width: CGFloat?) -> some View {
        Text(label).font(.subheadline)
            .frame(maxWidth: width == nil ? .infinity : nil).frame(width: width, height: 40)
            .background(Color(.systemBackground), in: RoundedRectangle(cornerRadius: 8))
    }
}

/// A drawn "on" switch, like the iOS toggle. Purely illustrative.
struct SwitchOn: View {
    var body: some View {
        ZStack(alignment: .trailing) {
            Capsule().fill(Color.green).frame(width: 51, height: 31)
            Circle().fill(.white).frame(width: 27, height: 27).padding(.trailing, 2).shadow(color: .black.opacity(0.2), radius: 2, y: 1)
        }
        .accessibilityHidden(true)
    }
}
