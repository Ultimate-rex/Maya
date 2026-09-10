import tempfile
from fastapi import APIRouter, UploadFile, File, Depends
from fastapi.responses import Response
from pydantic import BaseModel
from core.security import require_auth
from ai.stt.whisper_provider import WhisperSTTProvider
from ai.tts.tts_provider import TTSProvider

router = APIRouter(prefix="/voice", tags=["voice"])


class TranscribeResponse(BaseModel):
    text: str


class SpeakRequest(BaseModel):
    text: str


@router.post("/transcribe", response_model=TranscribeResponse, dependencies=[Depends(require_auth)])
async def transcribe(audio: UploadFile = File(...)):
    stt = WhisperSTTProvider()
    with tempfile.NamedTemporaryFile(suffix=".m4a", delete=True) as tmp:
        tmp.write(await audio.read())
        tmp.flush()
        text = stt.transcribe(tmp.name)
    return TranscribeResponse(text=text)


@router.post("/speak", dependencies=[Depends(require_auth)])
async def speak(req: SpeakRequest):
    tts = TTSProvider()
    audio_bytes = tts.synthesize(req.text)
    return Response(content=audio_bytes, media_type="audio/mpeg")
