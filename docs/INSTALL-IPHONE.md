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

## Follow the in-app setup guide

The first time you open OpenFlow, a short guide walks you through everything. It works like Wispr Flow's setup. You can replay it any time from **Settings → Show the setup guide again** inside the app.

1. **Where do you spend your time typing?** Pick what applies: messages, emails, AI chats, notes, something else.
2. **Three quick pages** show what OpenFlow does: dictating notes, working in any app, and turning messy speech into clean text.
3. **Add the OpenFlow keyboard.** Tap **Open Settings**. In **OpenFlow → Keyboards**, switch on **OpenFlow**, then switch on **Allow Full Access**. Come back to the app. A green ✓ shows when the keyboard has been added.
   - Full Access lets the keyboard receive your dictated text from the app. OpenFlow doesn't send anything over the internet.
4. **Allow the microphone.** Tap **Allow microphone**, then allow speech recognition too. Speech is recognised on the iPhone when iOS allows it.
5. **Try it.** Tap the text box, then tap and hold the **globe** key and choose **OpenFlow**. Tap **🎙 Tap to dictate**. OpenFlow opens and starts listening.

## Dictate in any app

1. Open any app with a text box, like Notes or Messages, and tap the box.
2. Tap and hold the **globe** key and choose **OpenFlow**.
3. Tap **🎙 Tap to dictate**. The OpenFlow app opens and starts listening straight away.
4. Speak. Tap the red button when you're done.
5. Swipe back to your app, or tap **‹ back** at the top left of the status bar. The keyboard inserts your text.

iOS doesn't let keyboards use the microphone, so recording happens in the OpenFlow app. That is why the app opens, and why Wispr Flow's keyboard does the same in other apps. Your text is inserted after you come back, within two minutes.

---

## Troubleshooting

| What you see | What to do |
|---|---|
| The keyboard says "Turn on Allow Full Access" | Settings → OpenFlow → Keyboards → Allow Full Access. |
| Tap to dictate does nothing | Open the OpenFlow app once from the Home Screen, finish the setup guide, then try again. |
| Nothing is inserted | Make sure you tapped stop in OpenFlow, then go back to the app. Text is kept for two minutes. |
| The app stops opening after a week | You're on a free Apple ID. Press Run in Xcode again. |
| Some banking apps show no keyboard | Apple blocks third-party keyboards in some secure fields. Use the OpenFlow app and copy the text. |

## Uninstall

Delete the OpenFlow app from the Home Screen. Then remove the keyboard under Settings → General → Keyboard → Keyboards.
