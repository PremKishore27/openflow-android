import UIKit

/// The OpenFlow keyboard. iOS doesn't let keyboards use the microphone, so the mic key opens the
/// OpenFlow app, which records. When you come back, the text is inserted here.
final class KeyboardViewController: UIInputViewController {
    private let status = UILabel()
    private var micButton: UIButton!

    override func viewDidLoad() {
        super.viewDidLoad()
        build()
    }

    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        insertPendingDictation()
    }

    private func build() {
        view.backgroundColor = .secondarySystemBackground

        status.text = hasFullAccess ? "Tap the mic to dictate" : "Turn on Allow Full Access for OpenFlow in Settings"
        status.font = .preferredFont(forTextStyle: .footnote)
        status.textColor = .secondaryLabel
        status.textAlignment = .center
        status.numberOfLines = 2

        micButton = makeKey(title: "🎙 Dictate", background: .systemBlue, foreground: .white, action: #selector(dictateTapped))
        let globe = makeKey(title: "🌐", background: .tertiarySystemFill, foreground: .label, action: nil)
        globe.addTarget(self, action: #selector(nextKeyboardTapped), for: .touchUpInside)
        globe.isHidden = !needsInputModeSwitchKey
        let space = makeKey(title: "space", background: .systemBackground, foreground: .label, action: #selector(spaceTapped))
        let delete = makeKey(title: "⌫", background: .tertiarySystemFill, foreground: .label, action: #selector(deleteTapped))
        let enter = makeKey(title: "return", background: .tertiarySystemFill, foreground: .label, action: #selector(returnTapped))

        let keys = UIStackView(arrangedSubviews: [globe, micButton, space, delete, enter])
        keys.axis = .horizontal
        keys.spacing = 6
        keys.distribution = .fill
        micButton.widthAnchor.constraint(equalTo: space.widthAnchor, multiplier: 1.2).isActive = true
        globe.widthAnchor.constraint(equalToConstant: 44).isActive = true
        delete.widthAnchor.constraint(equalToConstant: 56).isActive = true

        let stack = UIStackView(arrangedSubviews: [status, keys])
        stack.axis = .vertical
        stack.spacing = 8
        stack.translatesAutoresizingMaskIntoConstraints = false
        view.addSubview(stack)
        NSLayoutConstraint.activate([
            stack.leadingAnchor.constraint(equalTo: view.leadingAnchor, constant: 8),
            stack.trailingAnchor.constraint(equalTo: view.trailingAnchor, constant: -8),
            stack.topAnchor.constraint(equalTo: view.topAnchor, constant: 8),
            stack.bottomAnchor.constraint(equalTo: view.bottomAnchor, constant: -8),
            view.heightAnchor.constraint(equalToConstant: 216),
        ])
    }

    private func makeKey(title: String, background: UIColor, foreground: UIColor, action: Selector?) -> UIButton {
        let b = UIButton(type: .system)
        b.setTitle(title, for: .normal)
        b.setTitleColor(foreground, for: .normal)
        b.titleLabel?.font = .preferredFont(forTextStyle: .body)
        b.backgroundColor = background
        b.layer.cornerRadius = 8
        b.heightAnchor.constraint(equalToConstant: 46).isActive = true
        if let action { b.addTarget(self, action: action, for: .touchUpInside) }
        return b
    }

    // MARK: - Actions

    @objc private func dictateTapped() {
        guard hasFullAccess else {
            status.text = "Turn on Allow Full Access for OpenFlow in Settings"
            return
        }
        guard let url = URL(string: "openflow://record") else { return }
        if !openApp(url) {
            status.text = "Open the OpenFlow app from your Home Screen, tap the mic, then come back."
        } else {
            status.text = "Speak in OpenFlow, then switch back to this app."
        }
    }

    /// Keyboards can't call UIApplication.open directly. Walk up the responder chain to the host app instead.
    private func openApp(_ url: URL) -> Bool {
        let selector = sel_registerName("openURL:")
        var responder: UIResponder? = self
        while let r = responder {
            if r.responds(to: selector) {
                _ = r.perform(selector, with: url)
                return true
            }
            responder = r.next
        }
        return false
    }

    private func insertPendingDictation() {
        guard let text = SharedStore.takeResult() else { return }
        let before = textDocumentProxy.documentContextBeforeInput
        let fitted = ContextFit.fit(text, before: before)
        textDocumentProxy.insertText(fitted)
        status.text = "Inserted your dictation"
    }

    @objc private func nextKeyboardTapped() { advanceToNextInputMode() }
    @objc private func spaceTapped() { textDocumentProxy.insertText(" ") }
    @objc private func deleteTapped() { textDocumentProxy.deleteBackward() }
    @objc private func returnTapped() { textDocumentProxy.insertText("\n") }
}
