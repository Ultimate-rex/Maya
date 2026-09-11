from fastapi import APIRouter, Depends
from pydantic import BaseModel
from core.security import require_auth
from core.config import load_llm_settings, save_llm_settings

router = APIRouter(prefix="/settings", tags=["settings"])


class LLMSettings(BaseModel):
    provider: str  # "gemini" | "groq"
    model: str


@router.get("/llm", response_model=LLMSettings, dependencies=[Depends(require_auth)])
async def get_llm_settings():
    return load_llm_settings()


@router.put("/llm", response_model=LLMSettings, dependencies=[Depends(require_auth)])
async def update_llm_settings(settings: LLMSettings):
    save_llm_settings(settings.dict())
    return settings
