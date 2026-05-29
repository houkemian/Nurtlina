"""Firebase ID token verification and CurrentUser extraction."""

from dataclasses import dataclass

from fastapi import Header, HTTPException, status

from app.integrations.firebase_admin import verify_id_token


@dataclass(frozen=True)
class CurrentUser:
    user_id: str  # internal DB id (populated after upsert in deps.py)
    firebase_uid: str
    email: str | None
    default_family_id: str | None


def _extract_bearer(authorization: str) -> str:
    parts = authorization.split(" ", 1)
    if len(parts) != 2 or parts[0].lower() != "bearer":
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Authorization header must be 'Bearer <token>'.",
        )
    return parts[1]


async def verify_firebase_token(
    authorization: str = Header(...),
) -> dict[str, object]:
    """Low-level dep: verify Firebase ID token, return decoded claims."""
    token = _extract_bearer(authorization)
    try:
        decoded = verify_id_token(token)
    except Exception:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Invalid or expired Firebase ID token.",
        )
    return decoded  # type: ignore[return-value]
