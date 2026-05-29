"""Client-safe ULID generation for all entity IDs."""

from ulid import ULID


def new_id() -> str:
    """Generate a new ULID string (time-sortable, URL-safe, 26 chars)."""
    return str(ULID())
