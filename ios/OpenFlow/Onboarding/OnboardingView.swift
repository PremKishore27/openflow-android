import SwiftUI
import UIKit

/// First-run guide: a few questions, a few infographics, then keyboard, microphone and a live try-it page.
struct OnboardingView: View {
    @ObservedObject var session: DictationSession
    var onFinish: () -> Void

    private enum Step: Int, CaseIterable { case typing, notes, anyApp, cleanup, keyboard, mic, tryIt }

    @State private var step: Step = .typing
    @State private var forward = true
    @State private var picked: Set<UseCase> = Set(SharedStore.useCases.compactMap(UseCase.init(rawValue:)))
    @State private var keyboardAdded = Setup.keyboardAdded
    @State private var micOK = Setup.micGranted && Setup.speechGranted
    @State private var keyboardSeen = SharedStore.keyboardSeen
    @State private var fullAccess = SharedStore.keyboardFullAccess
    @State private var tryText = ""
    @FocusState private var tryFocused: Bool
    @Environment(\.scenePhase) private var scenePhase

    var body: some View {
        VStack(spacing: 0) {
            progress
            ScrollView {
                page
                    .id(step)
                    .transition(.asymmetric(
                        insertion: .move(edge: forward ? .trailing : .leading).combined(with: .opacity),
                        removal: .move(edge: forward ? .leading : .trailing).combined(with: .opacity)))
                    .padding(.horizontal, 24)
                    .padding(.top, 8)
                    .padding(.bottom, 24)
            }
            .scrollDismissesKeyboard(.interactively)
            footer
        }
        .background(Color(.systemGroupedBackground).ignoresSafeArea())
        .animation(.snappy(duration: 0.35), value: step)
        .onChange(of: scenePhase) { _, phase in if phase == .active { refresh() } }
        .onReceive(Timer.publish(every: 1, on: .main, in: .common).autoconnect()) { _ in refresh() }
    }

    // MARK: - Pieces

    private var progress: some View {
        HStack(spacing: 6) {
            ForEach(Step.allCases, id: \.rawValue) { s in
                Capsule()
                    .fill(s.rawValue <= step.rawValue ? AnyShapeStyle(Theme.gradient) : AnyShapeStyle(Color.gray.opacity(0.25)))
                    .frame(height: 5)
            }
        }
        .padding(.horizontal, 24)
        .padding(.top, 14)
        .padding(.bottom, 8)
        .accessibilityHidden(true)
    }

    @ViewBuilder private var page: some View {
        switch step {
        case .typing:
            header("Where do you spend your time typing?", "Pick all that apply.")
            VStack(spacing: 12) {
                ForEach(UseCase.allCases) { use in
                    chip(use)
                }
            }
            .padding(.top, 12)
        case .notes:
            header("Dictation is great for notes", "Speak naturally. OpenFlow does the typing.")
            NotesMock().padding(.top, 12)
        case .anyApp:
            header("Works in any app", "Messages, mail, notes, AI chats: anywhere you can type.")
            AnyAppMock().padding(.top, 20)
        case .cleanup:
            header("Speak messy. Get clean text.", "OpenFlow tidies what you say as it types.")
            BeforeAfterMock().padding(.top, 12)
        case .keyboard:
            header("Add the OpenFlow keyboard", "Two quick switches in Settings.")
            KeyboardStepsMock().padding(.top, 12)
            status(keyboardAdded ? "Keyboard added" : "Waiting for the keyboard…", done: keyboardAdded)
        case .mic:
            header("Allow the microphone", "OpenFlow needs it to hear what you say. Speech is recognised on your iPhone when iOS allows.")
            MicPermissionMock().padding(.top, 12)
            status(micOK ? "Microphone and speech allowed" : "Not allowed yet", done: micOK)
        case .tryIt:
            header("Try it", "Tap the box, switch to the OpenFlow keyboard, then tap Dictate.")
            GlobeKeyMock().padding(.top, 12)
            TextField("Tap here, then speak…", text: $tryText, axis: .vertical)
                .lineLimit(3...6)
                .padding(16)
                .background(Color(.secondarySystemGroupedBackground), in: RoundedRectangle(cornerRadius: 18, style: .continuous))
                .focused($tryFocused)
                .padding(.top, 14)
            VStack(alignment: .leading, spacing: 8) {
                status(keyboardSeen ? "OpenFlow keyboard is showing" : "Switch to the OpenFlow keyboard", done: keyboardSeen)
                if keyboardSeen { status(fullAccess ? "Full Access is on" : "Turn on Allow Full Access", done: fullAccess) }
                Text("Tap 🎙 Dictate. OpenFlow opens and listens. Tap the red button when you're done, then swipe back. Your text appears in the box.")
                    .font(.footnote).foregroundStyle(.secondary)
            }
            .padding(.top, 10)
        }
    }

    private func header(_ title: String, _ subtitle: String) -> some View {
        VStack(spacing: 10) {
            LogoMark(size: 56).padding(.top, 8)
            Text(title).font(.system(.largeTitle, design: .rounded).bold()).multilineTextAlignment(.center)
            Text(subtitle).font(.body).foregroundStyle(.secondary).multilineTextAlignment(.center)
        }
        .frame(maxWidth: .infinity)
    }

    private func chip(_ use: UseCase) -> some View {
        let on = picked.contains(use)
        return Button {
            UIImpactFeedbackGenerator(style: .light).impactOccurred()
            if on { picked.remove(use) } else { picked.insert(use) }
        } label: {
            HStack(spacing: 14) {
                Image(systemName: use.symbol).font(.title3).foregroundStyle(on ? .white : Theme.blue)
                    .frame(width: 40, height: 40)
                    .background(on ? AnyShapeStyle(Color.white.opacity(0.2)) : AnyShapeStyle(Theme.blue.opacity(0.12)), in: Circle())
                Text(use.title).font(.headline)
                Spacer()
                Image(systemName: on ? "checkmark.circle.fill" : "circle").font(.title3)
            }
            .padding(14)
            .foregroundStyle(on ? Color.white : Color.primary)
            .background(on ? AnyShapeStyle(Theme.gradient) : AnyShapeStyle(Color(.secondarySystemGroupedBackground)),
                        in: RoundedRectangle(cornerRadius: 18, style: .continuous))
        }
        .buttonStyle(.plain)
        .accessibilityAddTraits(on ? .isSelected : [])
    }

    private func status(_ text: String, done: Bool) -> some View {
        HStack(spacing: 8) {
            Image(systemName: done ? "checkmark.circle.fill" : "circle.dotted")
                .foregroundStyle(done ? Color.green : Color.secondary)
            Text(text).font(.subheadline.weight(.medium)).foregroundStyle(done ? Color.primary : Color.secondary)
        }
        .frame(maxWidth: .infinity, alignment: .center)
        .padding(.top, 14)
    }

    // MARK: - Footer buttons

    private var footer: some View {
        VStack(spacing: 8) {
            switch step {
            case .keyboard:
                Button(keyboardAdded ? "Continue" : "Open Settings") {
                    if keyboardAdded { go(.next) } else { Setup.openSettings() }
                }
                .buttonStyle(PrimaryButtonStyle())
                if !keyboardAdded { skip("Skip for now") }
            case .mic:
                Button(micOK ? "Continue" : "Allow microphone") {
                    if micOK { go(.next) } else { Setup.requestPermissions { refresh() } }
                }
                .buttonStyle(PrimaryButtonStyle())
            case .tryIt:
                Button(tryText.isEmpty ? "Skip, I'll try later" : "Finish") { finish() }
                    .buttonStyle(PrimaryButtonStyle(enabled: true))
            default:
                Button("Continue") { go(.next) }.buttonStyle(PrimaryButtonStyle())
            }
            if step != .typing {
                Button("Back") { go(.back) }
                    .font(.subheadline).foregroundStyle(.secondary).padding(.top, 2)
            } else {
                Color.clear.frame(height: 22)
            }
        }
        .padding(.horizontal, 24)
        .padding(.bottom, 12)
        .padding(.top, 8)
    }

    private func skip(_ title: String) -> some View {
        Button(title) { go(.next) }.font(.subheadline).foregroundStyle(.secondary)
    }

    // MARK: - Logic

    private enum Direction { case next, back }

    private func go(_ dir: Direction) {
        UIImpactFeedbackGenerator(style: .light).impactOccurred()
        tryFocused = false
        if step == .typing { SharedStore.useCases = picked.map(\.rawValue).sorted() }
        forward = dir == .next
        let raw = step.rawValue + (dir == .next ? 1 : -1)
        if let s = Step(rawValue: raw) { step = s }
    }

    private func finish() {
        UINotificationFeedbackGenerator().notificationOccurred(.success)
        onFinish()
    }

    private func refresh() {
        keyboardAdded = Setup.keyboardAdded
        micOK = Setup.micGranted && Setup.speechGranted
        keyboardSeen = SharedStore.keyboardSeen
        fullAccess = SharedStore.keyboardFullAccess
        // Text from the keyboard arrives in the shared store; show it here if the try-it box is focused.
        if step == .tryIt, let text = SharedStore.takeResult() {
            tryText += (tryText.isEmpty ? "" : " ") + text
        }
    }
}
