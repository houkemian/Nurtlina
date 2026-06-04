"""Google Play Developer API client for purchase and subscription verification."""

import threading
from typing import Any

import httpx
from google.auth.transport.requests import Request as GoogleAuthRequest
from google.oauth2 import service_account

_SCOPE = "https://www.googleapis.com/auth/androidpublisher"
_BASE = "https://androidpublisher.googleapis.com/androidpublisher/v3/applications"

_creds_lock = threading.Lock()
_creds: service_account.Credentials | None = None


class GooglePlayError(Exception):
    pass


def _get_credentials() -> service_account.Credentials:
    global _creds
    if _creds is not None:
        return _creds
    with _creds_lock:
        if _creds is not None:
            return _creds
        from app.core.config import settings

        if not settings.google_play_service_account_path:
            raise GooglePlayError("GOOGLE_PLAY_SERVICE_ACCOUNT_PATH not configured.")
        _creds = service_account.Credentials.from_service_account_file(
            settings.google_play_service_account_path,
            scopes=[_SCOPE],
        )
    return _creds


def _get_access_token() -> str:
    creds = _get_credentials()
    if not creds.valid:
        creds.refresh(GoogleAuthRequest())
    return creds.token  # type: ignore[return-value]


async def verify_subscription_purchase(
    package_name: str,
    subscription_id: str,
    purchase_token: str,
) -> dict[str, Any]:
    """Verify a subscription purchase via Google Play Developer API v3."""
    url = f"{_BASE}/{package_name}/purchases/subscriptionsv2/tokens/{purchase_token}"
    token = _get_access_token()
    async with httpx.AsyncClient(timeout=10.0) as client:
        resp = await client.get(url, headers={"Authorization": f"Bearer {token}"})
    if resp.status_code != 200:
        raise GooglePlayError(f"Google Play API returned {resp.status_code}: {resp.text}")
    return resp.json()


async def verify_one_time_purchase(
    package_name: str,
    product_id: str,
    purchase_token: str,
) -> dict[str, Any]:
    """Verify a one-time product purchase (e.g. lifetime) via Google Play Developer API."""
    url = f"{_BASE}/{package_name}/purchases/products/{product_id}/tokens/{purchase_token}"
    token = _get_access_token()
    async with httpx.AsyncClient(timeout=10.0) as client:
        resp = await client.get(url, headers={"Authorization": f"Bearer {token}"})
    if resp.status_code != 200:
        raise GooglePlayError(f"Google Play API returned {resp.status_code}: {resp.text}")
    return resp.json()
