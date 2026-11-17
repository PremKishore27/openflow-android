# OpenFlow for Android: a voice keyboard (Wispr Flow–style)

Hold the mic key in any app, talk, let go: cleaned-up text is typed for you.
This is the Android sibling of the Mac app in `openflow/`.

## Build and install

1. Build with `./gradlew assembleDebug` (JDK 17, Android SDK 35) or press **Run** in Android Studio.
2. Install `app/build/outputs/apk/debug/app-debug.apk` on your phone.
3. Open **OpenFlow**: allow the microphone, then turn on the overlay under Accessibility → Installed apps → OpenFlow.
   (If Android blocks it as a "restricted setting": Settings → Apps → OpenFlow → ⋮ → Allow restricted settings.)
4. Tap any text field. A small dark pill appears above your normal keyboard. Hold it and talk.

Unit tests (cleanup + context logic): `./gradlew test`.

## Using the overlay

| Action | How |
|---|---|
| Dictate | Hold the pill, talk, release |
| Hands-free | Tap the pill quickly, talk, tap ✓ (✕ cancels) |
| Edit selected text by voice | Select text, tap the ✎ chip, say "make this more formal" (needs API key) |
| Undo last dictation | Tap the ↶ chip (shown for a few seconds after inserting) |

Your own keyboard is never replaced. OpenFlow is an accessibility service that draws the pill as an
overlay and writes text into the focused field. It ignores password fields.

## What it does

Same pipeline as the Mac app: spoken punctuation, "scratch that", self-corrections ("3, no wait, 4"),
filler and stutter removal, your dictionary and snippets, tone per app (by package name), and
**context awareness**: it reads the text before the cursor so a dictation continuing a sentence
starts in lower case with the right spacing. Password fields are never read.
Optional AI polish and edit-by-voice use the Anthropic API with your own key.

## How it works

- `OpenFlowService.kt` the accessibility service: floating pill, mic gestures, inserting text, undo
- `Dictation.kt` Android `SpeechRecognizer` wrapper (restarts itself so pauses don't end your dictation)
- `Cleanup.kt`, `ContextFit.kt` ports of the Mac app's text logic (same tests)
- `Llm.kt` optional Claude calls, `Prefs.kt` settings, `SettingsActivity.kt` setup + settings screen

## Honest limits

- Speech-to-text uses Android's built-in recognizer (on-device where your phone supports it, otherwise
  Google's service), not Whisper. Quality and privacy depend on the phone's recognizer.
  Swapping in on-device Whisper (whisper.cpp) is the natural next step.
- Some recognizers play a start sound each time they restart.
- Text is inserted with accessibility actions; a few apps with custom text views (some games, canvas editors) ignore them.
