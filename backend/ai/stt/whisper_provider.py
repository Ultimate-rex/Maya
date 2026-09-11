"""
Speech-to-text using OpenAI's Whisper API (works with any Whisper-compatible
endpoint, including self-hosted faster-whisper servers - just change
OPENAI_BASE_URL). If you want a fully local/offline option later, swap this
class out for one that calls a local faster-whisper or whisper.cpp process;
nothing else in the app needs to change because everything talks to
STTProvider, not to Whisper directly.
"""
from openai import OpenAI
from core.config import OPENAI_API_KEY


class WhisperSTTProvider:
    def __init__(self):
        if not OPENAI_API_KEY:
            raise RuntimeError("OPENAI_API_KEY is not set in the environment (.env).")
        self.client = OpenAI(api_key=OPENAI_API_KEY)

    def transcribe(self, audio_file_path: str) -> str:
        with open(audio_file_path, "rb") as f:
            result = self.client.audio.transcriptions.create(
                model="whisper-1",
                file=f,
            )
        return result.text
