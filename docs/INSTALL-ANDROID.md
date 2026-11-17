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

## 4. Allow the microphone

On the OpenFlow screen, tap **Microphone** and choose **While using the app** (or **Allow**).

## 5. Turn on the overlay

This is the step that trips most people up. Android blocks apps installed outside the Play Store from using accessibility features until you unlock them. OpenFlow uses the accessibility service to draw the pill above your keyboard and type for you. Nothing else is read.

1. On the OpenFlow screen, tap **Overlay**. This opens **Settings → Accessibility**.
2. Look for **OpenFlow** under **Installed apps** (or **Downloaded apps**).
   - If it's there and the switch is **greyed out**, go to step 3.
   - If you see **"Restricted setting"** when you tap it, go to step 3.
   - If the switch works, skip to step 4.
3. **Unlock restricted settings:**
   1. Go back to the OpenFlow screen and tap **Allow restricted settings**. This opens **App info** for OpenFlow.
   2. Tap the **⋮** (three dots) in the top-right corner.
   3. Tap **Allow restricted settings** and confirm with your PIN, pattern or fingerprint.
   4. Go back to **Settings → Accessibility → Installed apps → OpenFlow**.

   Menu names vary by phone maker (Samsung, Pixel, Xiaomi and others). If you can't find them, search Settings for **restricted** and pick the result for OpenFlow.
4. Tap **OpenFlow**, switch **Use OpenFlow** on, and tap **Allow**.
5. Press back. The OpenFlow screen should now show **✓ On** next to Overlay.

## 6. Try it

1. Open **Notes** or any app with a text box.
2. Tap the text box. The keyboard opens and a dark pill appears just above it.
3. **Hold** the centre of the pill, say "Hello comma this is a test full stop", then let go.
4. Your words appear in the text box, cleaned up.

Tap **↶** within 8 seconds to undo a dictation. Tap the pill quickly to go hands-free, then **✓** to finish or **✕** to cancel.

---

## Troubleshooting

| What you see | What to do |
|---|---|
| The pill never appears | Check that the Overlay switch is on in Accessibility. Tap into a normal text box, not a password field. |
| "Microphone not allowed" | Allow the microphone on the OpenFlow screen, then try again. |
| Nothing gets typed | Open the OpenFlow screen and check both rows show ✓. Some apps with custom text boxes ignore the insert. |
| The pill covers the keyboard | Hide the keyboard and show it again so the pill moves up. |
| Overlay switch greyed out | Follow step 5.3 to allow restricted settings. |
| Updating to a new version | Download the new APK and tap Install. Your settings are kept. |

## Uninstall

Settings → Apps → OpenFlow → Uninstall. Android also removes the overlay permission then.
