from typing import List, Dict
import httpx
from ai.llm.base import LLMProvider
from core.config import GROQ_API_KEY

GROQ_URL = "https://api.groq.com/openai/v1/chat/completions"


class GroqProvider(LLMProvider):
    def __init__(self, model: str = "llama-3.3-70b-versatile"):
        if not GROQ_API_KEY:
            raise RuntimeError("GROQ_API_KEY is not set in the environment (.env).")
        self.model = model

    async def chat(self, messages: List[Dict[str, str]], system_prompt: str) -> str:
        payload_messages = [{"role": "system", "content": system_prompt}] + messages
        payload = {"model": self.model, "messages": payload_messages, "temperature": 0.7}
        headers = {"Authorization": f"Bearer {GROQ_API_KEY}"}

        async with httpx.AsyncClient(timeout=30) as client:
            resp = await client.post(GROQ_URL, json=payload, headers=headers)
            resp.raise_for_status()
            data = resp.json()

        return data["choices"][0]["message"]["content"]
