from ai.llm.base import LLMProvider
from ai.llm.gemini_provider import GeminiProvider
from ai.llm.groq_provider import GroqProvider
from core.config import load_llm_settings


def get_llm_provider() -> LLMProvider:
    settings = load_llm_settings()
    provider = settings.get("provider", "gemini")
    model = settings.get("model")

    if provider == "gemini":
        return GeminiProvider(model=model or "gemini-2.0-flash")
    if provider == "groq":
        return GroqProvider(model=model or "llama-3.3-70b-versatile")

    raise ValueError(f"Unknown LLM provider in settings: {provider}")
