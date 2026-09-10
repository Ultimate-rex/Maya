from abc import ABC, abstractmethod
from typing import List, Dict


class LLMProvider(ABC):
    """Every LLM provider (Gemini, Groq, custom) implements this interface
    so the rest of Maya never needs to know which one is active."""

    @abstractmethod
    async def chat(self, messages: List[Dict[str, str]], system_prompt: str) -> str:
        """messages: [{"role": "user"|"assistant", "content": "..."}]
        Returns the assistant's reply text (which may be raw JSON if the
        system prompt asked for a structured command)."""
        raise NotImplementedError
