# OpenFlow

**Talk, let go, and the words appear where you were typing.**

OpenFlow is a voice layer for Android in the spirit of Wispr Flow. It floats one small icon above your
normal keyboard. Tap it, speak naturally, tap ✓, and it types clean text into whatever app you are in:
punctuation added, filler words and false starts removed, your names spelled your way, and the tone
matched to the app you are in. Your keyboard is never replaced.

## Why it exists

Typing on a phone is slow, and dictation tools usually dump raw, messy transcripts. OpenFlow keeps the
keyboard you already like and adds a single gesture for speaking. What comes out reads like you wrote it.

## Download and install

| Device | Get it | Guide |
|---|---|---|
| Android 8.0+ | [**Download OpenFlow-Android.apk**](https://github.com/PremKishore27/openflow-android/releases/latest/download/OpenFlow-Android.apk) | [Install on Android](docs/INSTALL-ANDROID.md) |
| iPhone and iPad (iOS 17+) | [**Download OpenFlow-iOS-unsigned.ipa**](https://github.com/PremKishore27/openflow-android/releases/latest/download/OpenFlow-iOS-unsigned.ipa) (signed with your Apple ID by Sideloadly or AltStore) | [Install on iPhone](docs/INSTALL-IPHONE.md) |

All releases: [github.com/PremKishore27/openflow-android/releases](https://github.com/PremKishore27/openflow-android/releases)

**Verify the Android file:** from version 0.11, releases are signed with one permanent key. The certificate SHA-256 is
`12:FC:F9:4B:20:B8:A9:13:83:98:B1:66:B3:7B:71:BD:D8:D2:49:7D:5E:A4:45:CC:84:2E:8E:F9:B2:37:A7:26`.
Check a download with `apksigner verify --print-certs OpenFlow-Android.apk`.

**Heads-up for Android:** Android blocks the overlay until you allow *restricted settings*. The four-step setup below, and the app itself, walk you through it.

## Set up (Android)

1. **Allow the microphone** in the app.
2. **Open Accessibility** and find OpenFlow. Android says it's a *restricted setting*. That's expected.
3. **Allow restricted settings:** back in the app, tap it, then ⋮ → *Allow restricted settings*.
4. **Turn on the overlay:** open Accessibility again, open OpenFlow, switch it on.

The app walks you through these four steps in order. The full guide with troubleshooting is in [docs/INSTALL-ANDROID.md](docs/INSTALL-ANDROID.md).

## Using it

| To do this | Do this |
|---|---|
| Dictate | Tap the OpenFlow icon above your keyboard, speak, tap ✓ |
| Cancel | Tap ✕ |
| Move the icon | Drag it anywhere on screen. It remembers the spot, separately for when the keyboard is open and closed |
| Undo the last dictation | Tap "↶ Tap to undo" within 6 seconds |
| Rewrite selected text by voice | Select text, tap the icon, say "make this more formal" (needs your API key) |

One small OpenFlow logo floats above your keyboard. Tap it once and it starts listening, growing into a bar
with ✕, a live waveform and ✓. Your words show above it as you speak. Tap ✓ and they're typed where your cursor is.

Your own keyboard is never replaced. OpenFlow is an accessibility service that draws the icon as an
overlay and writes text into the focused field. It ignores password fields.

## What it does

- **Spoken punctuation:** "comma", "full stop", "new line", "new paragraph".
- **Self-corrections:** "Meet at 3, no wait, 4" becomes "Meet at 4". "Scratch that" removes the last sentence.
- **Cleanup:** removes "um", "uh" and stutters; fixes capitals and spacing.
- **Context:** reads the text before the cursor, so a continuation starts in lower case with the right spacing.
- **Your dictionary and snippets:** always spell "open flow" as "OpenFlow"; say "my email" to insert your address.
- **Tone per app:** casual in chat apps, formal in mail apps. You choose the mapping.
- **On-device Whisper (English):** a free, offline speech model that runs on your phone for much better accuracy than the built-in recognizer.
- **Update checks:** in Settings → About, the app asks GitHub if a newer release exists (Android and iPhone).
- **Silent dictation (Android):** an option to mute the phone recognizer's start and stop beeps.
- **Optional AI polish:** send the dictation to Claude for grammar and flow, using your own Anthropic key. This needs a key; without one, the polish switches do nothing.

## Privacy

- Password fields are never read or written.
- With the Whisper model downloaded, your audio never leaves the phone. Without it, OpenFlow can use the phone's built-in recognizer, which may send audio to Google (you can turn that off in Settings).
- Network calls: the one-time model download from Hugging Face, and the optional Claude request if you add a key and turn it on.
- Your API key and settings are stored in the app's private storage on your phone.

## How it works

| File | What it does |
|---|---|
| `OpenFlowService.kt` | Accessibility service: draws the overlay, handles gestures, writes text into the focused field |
| `Dictation.kt` | Wraps Android's built-in speech recognizer (the fallback engine) |
| `LocalDictation.kt`, `LocalWhisper.kt`, `Models.kt` | On-device Whisper: records audio, downloads and verifies the model, transcribes with sherpa-onnx |
| `Cleanup.kt`, `ContextFit.kt` | Rule-based text cleanup and capitalisation/spacing from context |
| `Llm.kt` | Optional Claude polish and edit-by-voice; falls back to the rule-based text on any failure |
| `Prefs.kt`, `SettingsActivity.kt` | Settings and the setup screen |
| `WaveformView.kt` | Live level bars while you speak |

The only third-party library is [sherpa-onnx](https://github.com/k2-fsa/sherpa-onnx) (Apache-2.0), which runs Whisper on the phone. See [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md).

## Build from source

**iPhone and iPad:** open `ios/OpenFlow.xcodeproj` in Xcode 26. Full steps are in [the iPhone guide](docs/INSTALL-IPHONE.md).

**Android:**

Pushes to `main` build the APK in GitHub Actions. Version tags (`v*`) also publish it as a release.

Needs JDK 17, the Android SDK (platform 35), and `bash` with `curl`. The first build downloads the pinned sherpa-onnx library automatically (`scripts/fetch-sherpa-onnx.sh`).

```bash
./gradlew test          # unit tests
./gradlew assembleDebug # app/build/outputs/apk/debug/app-debug.apk
```

## Limits

- Whisper needs a 64-bit phone and a one-time download (160–375 MB). It transcribes after you tap ✓, so there is no live text. English only for now. Whisper is Android-only so far: the iPhone and iPad app uses Apple's built-in recognizer.
- Some apps with custom text views (canvas editors, some games) ignore text inserted by accessibility tools.
- Debug builds only for now. There is no Play Store release.

## License

MIT. See [LICENSE](LICENSE).
