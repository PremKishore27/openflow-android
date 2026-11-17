# OpenFlow – working guidelines for Claude

Voice typing for Android (Kotlin accessibility-service overlay, on-device Whisper via sherpa-onnx) and iPhone/iPad (SwiftUI app + keyboard extension).
Public repo: `PremKishore27/openflow-android` (MIT, Ko-fi sponsor link). Read `README.md` and `CONTRIBUTING.md` first; this file is only the rules of work.

## Environment facts
- Android builds locally with JDK 17 and the Android SDK; the Gradle build downloads sherpa-onnx (pinned, SHA-256 checked) into `app/libs` (git-ignored).
- iPhone: only `swiftc -typecheck` against the iPhone SDK works locally (no simulator platform installed). The real Xcode build runs on GitHub's macOS runner.
- macOS: BSD `sed -i ''`, no `timeout`. Run `bash` scripts rather than relying on zsh globs.
- Scratch files, downloaded models and render previews go in the session scratchpad, not the repo.

## Releases
- Version tags `v*` build both apps and publish a Release (signed APK + unsigned `.ipa`). Keep `versionName` in `app/build.gradle.kts` and `MARKETING_VERSION` in `ios/project.yml` in sync with the tag (run `xcodegen generate` after editing).
- Ask before anything outward-facing or hard to reverse: tagging a release, force-pushes, deleting things, changing repo visibility or secrets, accounts, payouts.
- The signing key lives in `~/openflow-signing/` and in GitHub secrets only. Never commit a keystore or print its passwords.
- Commit messages and PR text carry NO Claude / Co-Authored-By trailer: the user wants only their own name in the history. AI use is disclosed in the README ("About this project").

## How it is built (keep it this way)
- Logic out of views: `Cleanup`, `ContextFit`, `Updates` are plain code with unit tests. The Swift versions in `ios/Shared` must match the Kotlin ones.
- The accessibility service reads only the focused text box, skips password fields, and stores or sends nothing it reads.
- No analytics, ads or accounts. Network calls are limited to: model download, GitHub update check, optional Anthropic request.
- Keep `README.md`, `CHANGELOG.md`, `.github/release-notes.md`, `docs/INSTALL-*.md` and the in-app setup text consistent when behaviour changes (setup steps, button names).

## Testing rules
- Do not claim something works unless it is proven by a test, a log, or the user says they ran it on a phone. Say plainly what is proven, what is a hypothesis, and what has not run on a device.
- If a cause is unknown, ask for details instead of guessing twice.

## Privacy and public-repo hygiene
- No emails, real names, phone locations or secrets in the repo, tests, issues or releases. Git author is the GitHub noreply address.
- Never invent donation links, store listings or screenshots. Label renders as renders.

## Communication
- The user is not a developer: plain language, short, no jargon. State results, what is unverified, and the one decision needed.
