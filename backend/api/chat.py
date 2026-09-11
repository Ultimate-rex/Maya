from fastapi import APIRouter, Depends
from pydantic import BaseModel
from typing import List, Dict, Optional
from core.security import require_auth
from core.assistant import handle_user_message

router = APIRouter(prefix="/chat", tags=["chat"])


class ChatRequest(BaseModel):
    text: str
    history: Optional[List[Dict[str, str]]] = None


class ChatResponse(BaseModel):
    intent: str
    parameters: Dict = {}
    say: str


@router.post("", response_model=ChatResponse, dependencies=[Depends(require_auth)])
async def chat(req: ChatRequest):
    result = await handle_user_message(req.text, req.history or [])
    return ChatResponse(
        intent=result.get("intent", "NONE"),
        parameters=result.get("parameters", {}),
        say=result.get("say", ""),
    )
