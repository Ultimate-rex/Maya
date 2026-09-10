"""
Text-to-speech. Ships with OpenAI's TTS (a paid API but cheap and reliable,
with a natural female voice option) since it works out of the box with no
extra install. To go fully free/offline, install Piper or Kokoro on your
server and swap the implementation of `synthesize` - the API contract
(text in, mp3 bytes out) stays identical so nothing else changes.
"""
from openai import OpenAI
from core.config import OPENAI_API_KEY


class TTSProvider:
    FEMALE_VOICE = "nova"  # OpenAI voice with a natural female timbre

    def __init__(self):
        if not OPENAI_API_KEY:
            raise RuntimeError("OPENAI_API_KEY is not set in the environment (.env).")
        self.client = OpenAI(api_key=OPENAI_API_KEY)

    def synthesize(self, text: str) -> bytes:
        response = self.client.audio.speech.create(
            model="tts-1",
            voice=self.FEMALE_VOICE,
            input=text,
        )
        return response.read()
