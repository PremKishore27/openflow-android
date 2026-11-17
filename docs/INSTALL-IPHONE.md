# Install OpenFlow on iPhone and iPad

OpenFlow for iPhone and iPad works like Wispr Flow: a **keyboard** you add in Settings, plus the **OpenFlow app**, which does the listening.

iPhone can't install apps straight from a download link. Instead you download an **unsigned** app file and use a free sideloading tool, which signs it with your own Apple ID and puts it on your phone. There are two ways:

- **Option A: Sideloadly or AltStore.** No Xcode. Works on Windows or Mac. About 10 minutes.
- **Option B: Build with Xcode** on a Mac. About 20 minutes. Use this if Option A doesn't work for you.

> **Free Apple ID:** the app stops opening after 7 days. Reinstall it the same way to refresh it. A paid Apple Developer account ($99/year) avoids this.

> **Honest note:** the keyboard extension needs your Apple ID to give it an App Group. The free-account tools do this for the app, but I haven't been able to test it on a real device. If the keyboard doesn't appear in Settings, use Option B.

---

## Option A: Sideloadly or AltStore

**Download:** https://github.com/PremKishore27/openflow-android/releases/latest/download/OpenFlow-iOS-unsigned.ipa

You need an iPhone or iPad on **iOS 17 or newer**, a computer (Windows or Mac), a cable, and an **Apple ID**.

1. On the computer, install **[Sideloadly](https://sideloadly.io)** (or **[AltStore](https://altstore.io)**). Follow its own installer.
2. Download the `.ipa` file from the link above.
3. Plug in the iPhone, unlock it, and tap **Trust** if asked.
4. In Sideloadly, drag the `.ipa` file into the window, choose your iPhone, and enter your Apple ID.
   - Keep **Remove app extensions** *off*. The keyboard is an extension.
5. Click **Start** and wait for **Done**.
6. On the iPhone: **Settings → Privacy & Security → Developer Mode → On.** The phone restarts. Confirm when asked.
7. On the iPhone: **Settings → General → VPN & Device Management → your Apple ID → Trust.**
8. Open **OpenFlow** from the Home Screen. Then continue with **Add the keyboard** below.

---

## Option B: Build with Xcode

### What you need

- A Mac with **Xcode 26** or newer (free from the Mac App Store).
- An iPhone or iPad on **iOS 17 or newer**, with a cable.
- An **Apple ID** added to Xcode (Xcode → Settings → Accounts → +). A free Apple ID is enough for your own device.

---

### 1. Get the code

```bash
git clone https://github.com/PremKishore27/openflow-android.git
cd openflow-android/ios
```

No Mac with git? Download the ZIP from the repository page (**Code → Download ZIP**) and unzip it.

### 2. Open the project

1. Double-click `ios/OpenFlow.xcodeproj`.
2. Click the **OpenFlow** project in the left sidebar.
3. For **both** targets, **OpenFlow** and **OpenFlowKeyboard**, open **Signing & Capabilities**:
   - Tick **Automatically manage signing**.
   - Under **Team**, choose your Apple ID (Personal Team).
   - If Xcode says the bundle ID is taken, change the prefix, for example `com.yourname.openflow` and `com.yourname.openflow.keyboard`.
   - Check that the **App Groups** capability shows `group.com.openflow.ios` (or `group.com.yourname.openflow`, if you changed the prefix). Keep the group name the same in both targets.

### 3. Connect your iPhone

1. Plug the iPhone into the Mac. Unlock it.
2. On the iPhone, tap **Trust** when asked, and enter your passcode.
3. Turn on **Developer Mode**: Settings → Privacy & Security → Developer Mode → On. The phone restarts and asks to confirm.
4. In Xcode, choose your iPhone in the device menu at the top.

### 4. Build and install

1. Press **Run** (▶).
2. The first build takes a few minutes.
3. If the iPhone says **Untrusted Developer**: Settings → General → VPN & Device Management → your Apple ID → **Trust**. Then press Run again.

OpenFlow opens on the phone.

---

## Add the keyboard

1. Open **Settings → General → Keyboard → Keyboards → Add New Keyboard**.
2. Tap **OpenFlow** under *Third-Party Keyboards*.
3. Tap **OpenFlow** again and switch on **Allow Full Access**.

Full Access lets the keyboard pass your dictated text back from the OpenFlow app. Apple shows a warning about it, and it's needed. OpenFlow doesn't send your text anywhere.

## Allow the microphone

1. Open the **OpenFlow** app and go to the **Settings** tab.
2. Tap **Allow microphone**, then **Allow**.
3. Also allow **Speech Recognition** when asked. Speech is recognised on the iPhone when iOS supports it.

## Dictate

1. Open any app with a text box, like Notes or Messages.
2. Tap the text box, then tap the **globe** key until the **OpenFlow** keyboard is showing.
3. Tap **🎙 Dictate**. The OpenFlow app opens and starts listening.
4. Speak. Tap the red button when you're done.
5. Swipe back to your app, or tap the **‹ back** link at the top left of the status bar. The keyboard inserts your text.

Tap **⌫** and **return** as usual. Your dictation is inserted after you come back, within two minutes.

---

## Troubleshooting

| What you see | What to do |
|---|---|
| The keyboard says "Turn on Allow Full Access" | Settings → General → Keyboard → Keyboards → OpenFlow → Allow Full Access. |
| Dictate does nothing | Open the OpenFlow app once, from the Home Screen. Then try again. |
| Nothing is inserted | Make sure you tapped stop in OpenFlow, then go back to the app. Text is kept for two minutes. |
| The app stops opening after a week | You're on a free Apple ID. Press Run in Xcode again. |
| Some banking apps show no keyboard | Apple blocks third-party keyboards in some secure fields. Use the OpenFlow app and copy the text. |

## Uninstall

Delete the OpenFlow app from the Home Screen. Then remove the keyboard under Settings → General → Keyboard → Keyboards.
