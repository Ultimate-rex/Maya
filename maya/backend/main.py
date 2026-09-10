from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from api import chat, voice, settings as settings_api

app = FastAPI(title="Maya Assistant Backend", version="0.1.0")

# Restrict this to your own domains/apps once you deploy for real.
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["*"],
    allow_headers=["*"],
)

app.include_router(chat.router)
app.include_router(voice.router)
app.include_router(settings_api.router)


@app.get("/health")
async def health():
    return {"status": "ok"}
