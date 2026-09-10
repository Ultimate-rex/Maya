# Maya — Personal Voice Assistant (Android + Python backend)

Two real, working parts:

```
maya/
├── backend/    Python FastAPI server — holds your API keys, runs the LLM/STT/TTS
└── android/    Kotlin + Jetpack Compose app — UI, mic recording, app launching
```

## ⚠️ About the API key you pasted in chat

Never paste real API keys into a chat, code, or an APK. Treat the Gemini key you
shared as **burned** — go to Google AI Studio → API keys and regenerate/delete it,
then put the *new* key only in `backend/.env` (see below). This app is built so
keys never need to leave your own server.

---

## 1. Run the backend

Requirements: Python 3.10+

```bash
cd backend
python -m venv venv
source venv/bin/activate        # Windows: venv\Scripts\activate
pip install -r requirements.txt

cp .env.example .env
# Now edit .env and fill in:
#   GEMINI_API_KEY=<your new key>
#   OPENAI_API_KEY=<for Whisper STT + TTS, optional if you swap providers later>
#   MAYA_ACCESS_TOKEN=<generate one, see below>

python -c "import secrets; print(secrets.token_urlsafe(32))"
# paste that output as MAYA_ACCESS_TOKEN in .env

uvicorn main:app --host 0.0.0.0 --port 8000
```

Test it:
```bash
curl http://localhost:8000/health
# {"status":"ok"}

curl -X POST http://localhost:8000/chat \
  -H "Authorization: Bearer <your MAYA_ACCESS_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{"text":"open chrome"}'
```

To reach it from your phone, deploy it somewhere with a public URL (a small VPS,
Render, Railway, Fly.io, etc.) — or use `ngrok http 8000` for quick testing on
your local network.

Data is stored as JSON under `backend/data/` — no database:
```
backend/data/
├── memory/       conversations.json, facts.json, preferences.json
├── settings/     llm_settings.json (switch Gemini <-> Groq without touching code)
├── plugins/
└── logs/
```

## 2. Build the Android app

### ⚠️ If you're building from a phone (no laptop): use GitHub Actions, not raw Termux

Termux **cannot** run AAPT2 (Android's resource compiler) — it's a native Linux
binary compiled for glibc, and Termux's Bionic-libc environment can't execute
it. This isn't a bug in this project; it fails the same way for every Android
project built with plain Gradle in Termux. `proot-distro ubuntu` works around
it but is heavy (5GB+, slow on most phones). The reliable option is to let
GitHub build it for you — free, works entirely from your phone's browser or
Termux's `git` command, no SDK/Gradle install needed on-device.

**Steps (once):**
1. Create a free GitHub account at github.com if you don't have one.
2. Create a new **private** repository, e.g. `maya-assistant`.
3. Push this whole `maya/` folder to it. From Termux:
   ```bash
   cd ~/maya-project/maya
   pkg install git -y
   git init
   git add .
   git commit -m "Initial commit"
   git branch -M main
   git remote add origin https://github.com/<your-username>/maya-assistant.git
   git push -u origin main
   ```
   (GitHub will ask you to authenticate — use a Personal Access Token as the
   password, generated at GitHub → Settings → Developer settings → Personal
   access tokens.)
4. This repo already includes `.github/workflows/build.yml`. As soon as you
   push, GitHub automatically builds the APK in the cloud.
5. On GitHub.com (browser is fine, even on your phone): open your repo →
   **Actions** tab → click the running/completed workflow → scroll to
   **Artifacts** → download `maya-debug-apk`. That's a zip containing
   `app-debug.apk`.
6. Unzip it, install the APK the normal way.

Any time you change the code, just `git add . && git commit -m "..." && git push`
and a fresh APK builds automatically 5-10 minutes later.

### If you do have access to a computer with Android Studio

```bash
cd android
# Open this folder in Android Studio, let Gradle sync, then:
./gradlew assembleDebug
```

The APK lands at:
```
android/app/build/outputs/apk/debug/app-debug.apk
```

Install it: `adb install app/build/outputs/apk/debug/app-debug.apk`, or drag it
onto your phone and tap it (allow "install unknown apps" for your file manager
once, when prompted — that's a normal Android system prompt, same kind as the
location-permission popup in your screenshot).

On first launch:
1. Android will show the real system permission dialogs for microphone and
   camera (flashlight) — tap Allow.
2. Tap the gear icon → enter your backend's URL and the `MAYA_ACCESS_TOKEN`
   you generated → Save.
3. Type a command like "open chrome" or hold the mic button and speak.

## 3. What Maya can actually do right now

- Open installed apps by name via Android's `Intent` system (Chrome, Spotify,
  WhatsApp, Settings, Clock, etc. — whatever's installed)
- Web search / open a URL
- Set alarms via the system Clock app; countdown timers
- Flashlight on/off, media volume, screen brightness (asks for the
  WRITE_SETTINGS permission via its own dedicated system screen first)
- Battery level / charging status
- Media playback control (play/pause/next/previous) for whatever app is
  currently playing, via Android's official MediaSession API — requires
  you to grant "Notification access" once in Settings, which only unlocks
  playback control, not reading message content
- Password generator (fully offline, `SecureRandom`)
- QR code generator, and a QR/barcode scanner with a normal, always-visible
  camera preview (same as any scanner app — never hidden)
- File management scoped to one folder you explicitly pick via Android's
  file picker (rename/delete/copy/move) — Maya cannot see anything outside
  that folder
- Recent-photo cleanup via `MediaStore`, using Android's own delete
  confirmation dialog — Maya can't silently delete your photos
- Automation triggers: battery-below-X%, charging started/stopped, Wi-Fi
  connected/disconnected, screen on/off → speak a line or open an app.
  Manage these from the wrench icon → Automation triggers.
- Full voice loop: hold mic → record → backend transcribes with Whisper →
  Gemini/Groq decides an action + reply → played back and shown
- JSON memory both on-device (`Android/data/com.maya.assistant/files/Maya/`,
  visible in a file manager) and on your server

### Manual permission grants some tools need

A couple of these intentionally require you to flip a switch yourself,
because Android reserves them behind a dedicated confirmation screen:

- **Media control** → Settings → Apps → Special app access → Notification
  access → enable Maya
- **Brightness control** → Maya will open "Modify system settings" for you
  the first time you ask her to change brightness
- **Photo deletion / file access** → the normal Android runtime permission
  prompts, or the file/photo picker, at the moment you use that tool

## 4. What's intentionally not included

Even with everything above added, these stay out, because the code can't
tell whose phone it's running on:

- Reading/controlling other apps' screens (auto-tapping inside WhatsApp,
  typing into Google Pay, sending messages on your behalf) via an
  Accessibility Service used the way banking-trojan malware uses it
- Device Admin activation (changing the lock-screen password, wiping the
  device after failed unlocks) — exactly the mechanism ransomware and
  stalkerware abuse
- Reading OTPs/notification content automatically, or auto-replying to
  messages without you seeing them first
- Answering or hanging up calls programmatically
- Taking photos without a visible camera preview

Everything in section 3 is the legitimate, Google-sanctioned way for an app
to act on a device, with the system's own permission prompts and
confirmation dialogs left intact — not bypassed.

## 5. Next steps if you want to keep going

- Wire a wake-word model (e.g., openWakeWord) into `VoiceListenerService` so
  Maya listens for "Wake up Maya" without you tapping anything
- Swap Whisper/OpenAI TTS for fully local Piper/Kokoro if you want zero
  ongoing API cost
- Start `TriggerService` automatically on boot (a `BOOT_COMPLETED` receiver)
  if you want automation rules active without opening the app first
- Add more `intent` types to `backend/core/assistant.py` for anything else
  you want voice-routed, plus matching handling in `MainActivity.kt`
