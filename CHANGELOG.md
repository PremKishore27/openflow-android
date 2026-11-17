# Changelog

Newest first. The latest build is always on the [Releases](https://github.com/PremKishore27/openflow-android/releases) page.

## 0.12.0

### Added
- **Minimal monochrome icon.** Same five voice bars, now flat white on near-black, on Android (including the themed icon on Android 13+), iPhone and the floating overlay icon.
- A proper GitHub page: new README, community files (contributing guide, code of conduct, security policy, issue and pull request templates), release notes on every release, and a social preview image.
- `scripts/make-icons.swift` draws the icon art, so the colours are easy to change.

## 0.11.0

### Changed
- **Releases are signed with one permanent key**, so updates install over the previous version. Android builds are now proper release builds, not debuggable ones. **If you have 0.10 or older, uninstall once before installing 0.11.**

## 0.10.0

### Added
- **Silence microphone beeps** (Android): mutes the phone recognizer's start and stop beeps while you dictate, then restores your volume. The overlay's own click sounds are off.
- **Check for updates** on Android and iPhone: asks GitHub for the newest release.

## 0.9.0

### Added
- **iPhone and iPad guided setup**, in the style of Wispr Flow's: what you type, three infographic pages, add the keyboard and Full Access, allow the microphone, try it.
- A big **🎙 Tap to dictate** key on the iPhone keyboard, and a full-screen recorder when the keyboard opens the app.

## 0.8.0
### Added
- **Drag the floating icon anywhere.** It remembers its spot for keyboard open and closed. *Reset icon position* is in settings.

## 0.7.0
### Added
- **On-device Whisper (English)** on Android: Whisper Small (375 MB) or Base (160 MB), free and offline, with a verified download. Option to use Google's online recognizer as a fallback.

## 0.6.0
### Changed
- The floating icon is the OpenFlow logo itself. One tap starts listening; ✕ and ✓ appear on the same icon.

## 0.5.0
### Changed
- One small overlay instead of two widgets. Guided four-step setup (microphone, Accessibility, restricted settings, overlay) in the app and on GitHub.

## 0.4.0
### Added
- iPhone and iPad app: keyboard and app, built as an unsigned `.ipa` on GitHub for Sideloadly or AltStore.

## 0.3.0
### Added
- Apple-style settings screen, new icon, step-by-step Android install guide, releases published from version tags.
