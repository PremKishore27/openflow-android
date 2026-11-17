# Contributing to OpenFlow

Thanks for helping! Bug reports, ideas and pull requests are all welcome.

## Reporting a bug

Open an [issue](https://github.com/PremKishore27/openflow-android/issues/new/choose) and include:

1. The app version (**Settings → About**), your phone model and Android or iOS version.
2. Which speech engine you used on Android: **Whisper Small**, **Whisper Base** or the phone's built-in recognizer.
3. What you did, what you expected, and what happened. A screenshot helps.

> 🔒 **Privacy:** don't paste anything personal (names, messages, API keys). Describe the problem instead.

## Suggesting a feature

Open an issue with the *Feature request* template. Say what you were trying to do, not only the solution you have in mind.

## Building and testing

**Android:** JDK 17, Android SDK 35, and `bash` with `curl`. The first build downloads the pinned sherpa-onnx library.

```bash
./gradlew test            # unit tests
./gradlew assembleDebug   # builds the APK
```

**iPhone and iPad:** a Mac with Xcode 26. Edit `ios/project.yml`, run `xcodegen generate`, open `ios/OpenFlow.xcodeproj`.

GitHub Actions builds both on every push and pull request. A pull request needs a green build.

## Code guidelines

- **Keep logic out of views.** Cleanup, capitals/spacing, update checks and model handling are plain code with unit tests. Views only draw and call them.
- **Keep Android and iPhone in sync.** `Cleanup` and `ContextFit` exist in Kotlin and Swift and must behave the same; port changes to both.
- **Privacy first.** The accessibility service reads only the focused text box and never password fields. No analytics, trackers or accounts.
- **No new libraries without a good reason.** sherpa-onnx is the one exception, for on-device Whisper. Open an issue to discuss before adding another.
- **Never commit** signing keys, model files, API keys or the downloaded `app/libs` folder.
- Keep user-facing text short and in plain language.

## Pull requests

1. Fork, create a branch, make your change with tests.
2. Describe what changed and why. Use the pull request template.
3. Be kind in reviews: see the [Code of Conduct](CODE_OF_CONDUCT.md).
