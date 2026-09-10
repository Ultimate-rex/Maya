"""
Simple bearer-token auth. The Android app sends:
    Authorization: Bearer <MAYA_ACCESS_TOKEN>
This keeps randoms who find your server URL from using it - they'd also
need your token, which never ships inside the APK's normal resources; you
paste it into the app's settings screen once, and it's stored in Android's
EncryptedSharedPreferences (see network/ApiClient.kt on the Android side).
"""
from fastapi import Header, HTTPException, status
from core.config import MAYA_ACCESS_TOKEN


async def require_auth(authorization: str = Header(default="")):
    if not authorization.startswith("Bearer "):
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="Missing bearer token")
    token = authorization.removeprefix("Bearer ").strip()
    if not secure_compare(token, MAYA_ACCESS_TOKEN):
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="Invalid token")
    return True


def secure_compare(a: str, b: str) -> bool:
    import hmac
    return hmac.compare_digest(a, b)
