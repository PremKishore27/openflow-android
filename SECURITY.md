# Security policy

## Supported versions

Only the [latest release](https://github.com/PremKishore27/openflow-android/releases/latest) is supported. Please update before reporting.

## Reporting a vulnerability

Please **do not** open a public issue for a security problem.
Use GitHub's private reporting instead: **Security → Report a vulnerability** on this repository.
Include what you found, how to reproduce it, and which version you tested. You will get an answer as soon as the maintainer can; this is a hobby project, so please allow a few days.

## What the app does and does not do

- On Android it uses the accessibility service to read **only the focused text box**, to type your dictation and match capitals. It skips password fields and stores or sends nothing it reads.
- With the Whisper model, speech is recognised on the phone. Audio is not uploaded.
- Network requests: a one-time Whisper model download from Hugging Face, an anonymous update check to GitHub, and the optional Anthropic request if you add your own key.
- No accounts, analytics or ads.

## Verifying a download

Releases are built by [GitHub Actions](.github/workflows/build-apk.yml) from the source in this repository.
Android releases are signed with one permanent key. The certificate SHA-256 is
`12:FC:F9:4B:20:B8:A9:13:83:98:B1:66:B3:7B:71:BD:D8:D2:49:7D:5E:A4:45:CC:84:2E:8E:F9:B2:37:A7:26`.
Check with `apksigner verify --print-certs OpenFlow-Android.apk`. Only download from this repository's **Releases** page.
