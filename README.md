# Maya — Personal Voice Assistant (Android, standalone)

One real, working Android app. No backend server, no separate deployment —
Maya calls Google's Gemini API directly from your phone using your own key.

```
maya/
├── android/    Kotlin + Jetpack Compose app
└── backend/    (optional, unused by default — see note at the bottom)
```

## ⚠️ About the key

Your Gemini API key lives **only** on your phone, encrypted via Android's
Keystore (EncryptedSharedPreferences) — it's never uploaded anywhere except
directly to Google in your own requests. This means:

- This APK is for **your personal use only**. Anyone else who gets a copy
  of your built APK would still need their own key pasted into Settings —
  the key isn't baked into the APK file itself, it's entered at runtime and
  stored per-device.
- If your phone is compromised or someone extracts your app's private
  storage, they could recover the key. That's an acceptable tradeoff for a
  personal single-device assistant, same as any app that stores your own
  API credentials for you to use (e.g. a personal Notion or OpenAI client).
- Get a free key at **aistudio.google.com/apikey**. Free tier has generous
  daily limits for personal use.

## How to build the APK (GitHub Actions — recommended, works from your phone)

1. Push this `maya/` folder to a GitHub repo (see the git commands you've
   already been using).
2. `.github/workflows/build.yml` builds it automatically on every push.
3. GitHub repo → **Actions** tab → open the run → **Artifacts** →
   download `maya-debug-apk` → install it.

## First run

1. Install the APK, open Maya.
2. Allow the microphone and camera permissions when Android asks.
3. Tap the gear (⚙) icon → paste your Gemini API key → Save.
4. Tap "Tap to talk to Maya" and speak, or type a command in the text box.

## What Maya can actually do right now (v3.2)

- Talk to Maya by voice with **no typing required**: tap "Tap to talk", or
  enable "Always listen for Hey Maya" in Settings for hands-free wake-word
  style activation (a real continuous-listening loop using Android's
  built-in speech recognizer)
- Conversation shown as chat bubbles, plus a full **History** screen (📜 icon)
  that reads your saved conversations straight from on-device JSON storage
- Open installed apps by name via Android's `Intent` system
- Web search / open a URL
- Set alarms via the system Clock app; countdown timers
- Flashlight on/off, media volume, screen brightness
- Battery level / charging status
- Media playback control (play/pause/next/previous) — requires "Notification
  access" granted once in Settings
- Password generator, QR code generator, QR/barcode scanner (visible camera
  preview), scoped file management, photo cleanup, automation triggers
- Spoken replies via Android's built-in offline-capable TextToSpeech engine
- JSON memory on-device at `Android/data/com.maya.assistant/files/Maya/`

No backend server, no access tokens — your Gemini key lives only in this
app's encrypted local storage, entered once in Settings.

## What's intentionally not included

- Reading/controlling other apps' screens (Accessibility-service automation
  into WhatsApp, Google Pay, etc.) — the mechanism banking-trojan malware uses
- Device Admin activation (lock-screen password changes, wipe-on-fail) —
  the mechanism ransomware/stalkerware uses
- Reading OTPs/notification content automatically, or auto-replying to
  messages without you seeing them first
- Answering or hanging up calls programmatically
- Taking photos without a visible camera preview

## About the `backend/` folder

Earlier in this build we used a separate Python FastAPI backend that held
the API key server-side instead of on the phone — more secure if you ever
wanted to share the app with other people, since each user wouldn't need
their own key and a compromised phone couldn't leak your key. The Android
app no longer calls it. It's left in the zip in case you want to go back to
that architecture later (better for anything beyond strictly personal,
single-device use); it isn't required for the app to build or run now.

## Next steps if you want to keep going

- Wire a wake-word model into `VoiceListenerService` for hands-free "Wake up Maya"
- Add conversation memory persistence across app restarts (currently
  in-memory per session, plus the JSON activity log)
- Add more `intent` types in `GeminiClient.kt`'s system prompt and matching
  handling in `MainActivity.kt`
