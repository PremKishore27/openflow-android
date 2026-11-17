# Install OpenFlow on iPhone and iPad

OpenFlow for iPhone and iPad works like Wispr Flow: a **keyboard** you add in Settings, plus the **OpenFlow app**, which does the listening.

iPhone can't install apps from a link the way Android can. Each person builds the app once on a Mac with Xcode and installs it on their own device. This takes about 20 minutes the first time.

## What you need

- A Mac with **Xcode 26** or newer (free from the Mac App Store).
- An iPhone or iPad on **iOS 17 or newer**, with a cable.
- An **Apple ID** added to Xcode (Xcode → Settings → Accounts → +). A free Apple ID is enough for your own device.

> **Free Apple ID:** the app stops opening after 7 days. Open this project in Xcode and press Run again to refresh it. A paid Apple Developer account ($99/year) avoids this.

---

## 1. Get the code

```bash
git clone https://github.com/PremKishore27/openflow-android.git
cd openflow-android/ios
```

No Mac with git? Download the ZIP from the repository page (**Code → Download ZIP**) and unzip it.

## 2. Open the project

1. Double-click `ios/OpenFlow.xcodeproj`.
2. Click the **OpenFlow** project in the left sidebar.
3. For **both** targets, **OpenFlow** and **OpenFlowKeyboard**, open **Signing & Capabilities**:
   - Tick **Automatically manage signing**.
   - Under **Team**, choose your Apple ID (Personal Team).
   - If Xcode says the bundle ID is taken, change the prefix, for example `com.yourname.openflow` and `com.yourname.openflow.keyboard`.
   - Check that the **App Groups** capability shows `group.com.openflow.ios` (or `group.com.yourname.openflow`, if you changed the prefix). Keep the group name the same in both targets.

## 3. Connect your iPhone

1. Plug the iPhone into the Mac. Unlock it.
2. On the iPhone, tap **Trust** when asked, and enter your passcode.
3. Turn on **Developer Mode**: Settings → Privacy & Security → Developer Mode → On. The phone restarts and asks to confirm.
4. In Xcode, choose your iPhone in the device menu at the top.

## 4. Build and install

1. Press **Run** (▶).
2. The first build takes a few minutes.
3. If the iPhone says **Untrusted Developer**: Settings → General → VPN & Device Management → your Apple ID → **Trust**. Then press Run again.

OpenFlow opens on the phone.

## 5. Add the keyboard

1. Open **Settings → General → Keyboard → Keyboards → Add New Keyboard**.
2. Tap **OpenFlow** under *Third-Party Keyboards*.
3. Tap **OpenFlow** again and switch on **Allow Full Access**.

Full Access lets the keyboard pass your dictated text back from the OpenFlow app. Apple shows a warning about it, and it's needed. OpenFlow doesn't send your text anywhere.

## 6. Allow the microphone

1. Open the **OpenFlow** app and go to the **Settings** tab.
2. Tap **Allow microphone**, then **Allow**.
3. Also allow **Speech Recognition** when asked. Speech is recognised on the iPhone when iOS supports it.

## 7. Dictate

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
