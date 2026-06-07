"""Firebase Admin SDK initialisation and token verification."""

import threading

import firebase_admin
from firebase_admin import auth as firebase_auth
from firebase_admin import credentials

_lock = threading.Lock()
_app: firebase_admin.App | None = None


def _get_app() -> firebase_admin.App:
    global _app
    if _app is not None:
        return _app
    with _lock:
        if _app is not None:
            return _app
        from app.core.config import settings

        cred = credentials.Certificate(settings.firebase_service_account_path)
        _app = firebase_admin.initialize_app(cred)
    return _app


def verify_id_token(id_token: str) -> dict:
    """Verify a Firebase ID token and return the decoded claims.

    Raises firebase_admin.auth.InvalidIdTokenError (or similar) on failure.
    The caller is responsible for mapping this to an HTTP 401.
    """
    _get_app()
    return firebase_auth.verify_id_token(id_token)  # type: ignore[return-value]
