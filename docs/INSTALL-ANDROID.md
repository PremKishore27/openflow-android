# Install OpenFlow on Android

About 5 minutes. You need an Android phone running Android 8.0 or newer.

**Download:** https://github.com/PremKishore27/openflow-android/releases/latest/download/OpenFlow-Android.apk

---

## 1. Download the app

1. Open the link above in **Chrome** on your phone.
2. Tap **Download anyway** if Chrome warns about the file. It's the app you're installing, built from this public source code.
3. When it finishes, tap **Open** in the download bar, or find `OpenFlow-Android.apk` in your **Downloads** folder.

## 2. Allow installing from Chrome (one time)

Android asks you to allow this the first time.

1. Tap **Settings** on the prompt, or go to **Settings → Apps → Special app access → Install unknown apps**.
2. Pick **Chrome** (or **Files** if you opened the file there).
3. Switch on **Allow from this source**.
4. Go back and tap the APK again.

## 3. Install

1. Tap **Install**.
2. If **Play Protect** says the app is unsafe or unrecognised, tap **Install anyway**. Play Protect does this for any app not from the Play Store.
3. Tap **Open** when it finishes.

## 4. Set up OpenFlow: 4 steps, in this order

Open OpenFlow. The **Set up** card lists the four steps below. Each one opens the right screen for you. The green ✓ on the app tells you when a step is done.

### Step 1: Allow the microphone
Tap **1. Allow the microphone** and choose **While using the app** (or **Allow**).

### Step 2: Open Accessibility
Tap **2. Open Accessibility**. Under **Installed apps** (or **Downloaded apps**), tap **OpenFlow**.

Android will say it's a **restricted setting**, or the switch will be greyed out. **This is expected.** Android blocks apps installed outside the Play Store from using accessibility until you allow it. Don't give up here: go back to OpenFlow and do step 3.

### Step 3: Allow restricted settings
1. Back in OpenFlow, tap **3. Allow restricted settings**. This opens **App info** for OpenFlow.
2. Tap the **⋮** (three dots) in the top-right corner.
3. Tap **Allow restricted settings** and confirm with your PIN, pattern or fingerprint.

Menu names vary by phone maker (Samsung, Pixel, Xiaomi and others). If you can't find it, search Settings for **restricted** and pick the result for OpenFlow.

> If the ⋮ menu has no "Allow restricted settings" entry, go back to step 2 and tap OpenFlow once more. Android only shows the option after it has blocked the switch once.

### Step 4: Turn on the overlay
1. Tap **4. Turn on the overlay**, then open **OpenFlow** under **Installed apps**.
2. Switch **Use OpenFlow** on, and tap **Allow**.
3. Press back. Step 4 on the OpenFlow screen now shows **✓ On**.

OpenFlow uses the accessibility service only to show its icon above your keyboard and to type your words. Nothing else is read, and password fields are skipped.

## 5. Download the Whisper model (recommended: much better accuracy)

Without this step, OpenFlow uses your phone's built-in speech recognizer. It works, but it is less accurate. The
on-device **Whisper** model is the same kind of engine behind the best dictation apps. It is free, private and
works offline.

1. In the OpenFlow app, scroll to **Speech recognition**.
2. Pick **Whisper Small** (best accuracy, 375 MB) or **Whisper Base** (faster, 160 MB). Both are English-only.
3. Tap **Download**. Use **Wi-Fi**. You can leave the screen while it downloads. The progress shows when you come back.
4. When it says **✓ is ready**, you're done. Dictation now uses Whisper, with no internet needed.

Notes:
- Whisper transcribes after you tap ✓, so you won't see your words appear while you speak. The waveform shows you're being heard.
- The first dictation after a restart can take a few seconds longer while the model loads.
- If dictation feels slow on an older phone, switch to **Whisper Base**.
- You can delete the model from the same screen to free the space.

## 6. Try it

1. Open **Notes** or any app with a text box.
2. Tap the text box. The keyboard opens and a small round **OpenFlow icon** (the blue circle with white sound bars) appears just above it, on the right.
3. **Tap the OpenFlow icon.** One tap and it starts listening. The icon itself grows into a small bar with **✕**, a live waveform and **✓**.
4. Speak. Your words show above the bar as you talk. Say "comma", "full stop" or "new line" to add punctuation.
5. Tap the green **✓**. Your cleaned-up words are typed where your cursor is, and the bar shrinks back to the icon.

| To do this | Do this |
|---|---|
| Cancel without typing anything | Tap **✕** |
| Move the icon | Drag it anywhere. It stays where you leave it (one spot with the keyboard open, one with it closed) |
| Put the icon back by the keyboard | OpenFlow app → Behaviour → **Reset icon position** |
| Undo what was just typed | Tap **↶ Tap to undo** above the icon (shown for 6 seconds) |
| Rewrite selected text | Select text, tap the icon, say "make this more formal" (needs an API key) |

---

## Troubleshooting

| What you see | What to do |
|---|---|
| The icon never appears | Check that step 4 shows ✓ On. Tap into a normal text box, not a password field. |
| "Microphone not allowed" | Do step 1 in the OpenFlow app, then try again. |
| Words are often wrong | Do step 5: download the Whisper model. It is far more accurate than the built-in recognizer. |
| Whisper is slow | Choose Whisper Base in the Speech recognition section. |
| Nothing gets typed | Open the OpenFlow screen and check both rows show ✓. Some apps with custom text boxes ignore the insert. |
| The icon is in the way | Hide the keyboard and show it again so the icon moves above it. |
| OpenFlow is greyed out or says "Restricted setting" | Do step 3: App info → ⋮ → Allow restricted settings. Then redo step 4. |
| Updating to a new version | Download the new APK and tap Install. Your settings are kept. |

## Uninstall

Settings → Apps → OpenFlow → Uninstall. Android also removes the overlay permission then.
