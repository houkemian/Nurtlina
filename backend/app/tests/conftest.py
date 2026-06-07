"""Test environment defaults.

These values are intentionally local/dummy so tests can import the FastAPI app
without production secrets. Tests that need real services should override them.
"""

import os

os.environ.setdefault("DATABASE_URL", "postgresql+asyncpg://test:test@localhost:5432/test")
os.environ.setdefault("DATABASE_SYNC_URL", "postgresql+psycopg2://test:test@localhost:5432/test")
os.environ.setdefault("FIREBASE_PROJECT_ID", "test-project")
os.environ.setdefault("FIREBASE_SERVICE_ACCOUNT_PATH", "/dev/null")
os.environ.setdefault("GOOGLE_PLAY_PACKAGE_NAME", "com.nurtlina.app")
