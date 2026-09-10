"""
Central configuration for Maya's backend.
Secrets (API keys) are ALWAYS loaded from environment variables, never from
source code or JSON files that ship with the app. The Android app never sees
these keys - it only ever talks to this backend over HTTPS with its own
short-lived access token.
"""
import os
import json
import secrets
from pathlib import Path
from dotenv import load_dotenv

load_dotenv()

BASE_DIR = Path(__file__).resolve().parent.parent
DATA_DIR = BASE_DIR / "data"
MEMORY_DIR = DATA_DIR / "memory"
SETTINGS_DIR = DATA_DIR / "settings"
PLUGINS_DIR = DATA_DIR / "plugins"
LOGS_DIR = DATA_DIR / "logs"

for d in (MEMORY_DIR, SETTINGS_DIR, PLUGINS_DIR, LOGS_DIR):
    d.mkdir(parents=True, exist_ok=True)

# --- Secrets: environment variables only ---
GEMINI_API_KEY = os.getenv("GEMINI_API_KEY", "")
GROQ_API_KEY = os.getenv("GROQ_API_KEY", "")
OPENAI_API_KEY = os.getenv("OPENAI_API_KEY", "")  # used for Whisper STT / TTS if selected

# --- Access control ---
# The Android app authenticates to this backend with a bearer token.
# Generate one with: python -c "import secrets; print(secrets.token_urlsafe(32))"
# and put it in your .env as MAYA_ACCESS_TOKEN=...
MAYA_ACCESS_TOKEN = os.getenv("MAYA_ACCESS_TOKEN", "")

if not MAYA_ACCESS_TOKEN:
    # Fail loudly rather than run an unauthenticated server.
    raise RuntimeError(
        "MAYA_ACCESS_TOKEN is not set. Create a .env file with "
        "MAYA_ACCESS_TOKEN=<a long random string> before starting the server."
    )


def default_llm_settings_path() -> Path:
    return SETTINGS_DIR / "llm_settings.json"


def load_llm_settings() -> dict:
    path = default_llm_settings_path()
    if not path.exists():
        default = {"provider": "gemini", "model": "gemini-2.0-flash"}
        path.write_text(json.dumps(default, indent=2))
        return default
    return json.loads(path.read_text())


def save_llm_settings(data: dict) -> None:
    default_llm_settings_path().write_text(json.dumps(data, indent=2))
