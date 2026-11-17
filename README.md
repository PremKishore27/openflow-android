# OpenFlow

**Talk, let go, and the words appear where you were typing.**

OpenFlow is a voice layer for Android in the spirit of Wispr Flow. It floats one small pill above your
normal keyboard. Hold it, speak naturally, and it types clean text into whatever app you are in:
punctuation added, filler words and false starts removed, your names spelled your way, and the tone
matched to the app you are in. Your keyboard is never replaced.

## Why it exists

Typing on a phone is slow, and dictation tools usually dump raw, messy transcripts. OpenFlow keeps the
keyboard you already like and adds a single gesture for speaking. What comes out reads like you wrote it.

## Download and install

| Device | Get it | Guide |
|---|---|---|
| Android 8.0+ | [**Download OpenFlow-Android.apk**](https://github.com/PremKishore27/openflow-android/releases/latest/download/OpenFlow-Android.apk) | [Install on Android](docs/INSTALL-ANDROID.md) |
| iPhone and iPad | Coming soon | [Install on iPhone](docs/INSTALL-IPHONE.md) |

All releases: [github.com/PremKishore27/openflow-android/releases](https://github.com/PremKishore27/openflow-android/releases)

**Heads-up for Android:** the overlay needs *restricted settings* unlocked first. The guide walks you through it.

## Using it

| To do this | Do this |
|---|---|
| Dictate | Hold the centre of the pill, speak, release |
| Go hands-free | Tap the pill quickly, speak, tap ✓ (or ✕ to cancel) |
| Rewrite selected text by voice | Select text, tap ✎, say "make this more formal" (needs your API key) |
| Undo the last dictation | Tap ↶ within 8 seconds |

Dictated words also appear live in the bubble above the pill while you speak.

## What it does

- **Spoken punctuation:** "comma", "full stop", "new line", "new paragraph".
- **Self-corrections:** "Meet at 3, no wait, 4" becomes "Meet at 4". "Scratch that" removes the last sentence.
- **Cleanup:** removes "um", "uh" and stutters; fixes capitals and spacing.
- **Context:** reads the text before the cursor, so a continuation starts in lower case with the right spacing.
- **Your dictionary and snippets:** always spell "open flow" as "OpenFlow"; say "my email" to insert your address.
- **Tone per app:** casual in chat apps, formal in mail apps. You choose the mapping.
- **Optional AI polish:** send the dictation to Claude for grammar and flow, using your own Anthropic key.

## Privacy

- Password fields are never read or written.
- Speech recognition uses Android's built-in recognizer. On recent phones it runs on-device when available.
- The only network call is the optional Claude request, and only if you add a key and turn it on.
- Your API key and settings are stored in the app's private storage on your phone.

## How it works

| File | What it does |
|---|---|
| `OpenFlowService.kt` | Accessibility service: draws the overlay, handles gestures, writes text into the focused field |
| `Dictation.kt` | Wraps Android's speech recognizer and keeps listening through pauses |
| `Cleanup.kt`, `ContextFit.kt` | Rule-based text cleanup and capitalisation/spacing from context |
| `Llm.kt` | Optional Claude polish and edit-by-voice; falls back to the rule-based text on any failure |
| `Prefs.kt`, `SettingsActivity.kt` | Settings and the setup screen |
| `WaveformView.kt` | Live level bars while you speak |

The app uses no third-party libraries. Everything runs on the Android SDK.

## Build from source

Pushes to `main` build the APK in GitHub Actions. Version tags (`v*`) also publish it as a release.

Needs JDK 17 and the Android SDK (platform 35).

```bash
./gradlew test          # unit tests
./gradlew assembleDebug # app/build/outputs/apk/debug/app-debug.apk
```

## Limits

- Dictation quality depends on the phone's speech recognizer. Swapping in on-device Whisper is the next big step.
- Some apps with custom text views (canvas editors, some games) ignore text inserted by accessibility tools.
- Debug builds only for now. There is no Play Store release.

## License

MIT. See [LICENSE](LICENSE).
