# Nurtlina Backend

FastAPI + PostgreSQL backend for Nurtlina: Baby Feeding Tracker.

## Stack

| Layer | Technology |
|---|---|
| Framework | FastAPI + Uvicorn |
| Validation | Pydantic v2 |
| ORM | SQLAlchemy 2.0 (async) |
| Driver | asyncpg |
| Migrations | Alembic |
| Auth | Firebase Admin SDK (ID token verification) |
| Billing | Google Play Developer API |
| Deploy | Google Cloud Run |
| Package manager | uv |

## Local Development

### Prerequisites

- Python 3.12+
- [uv](https://docs.astral.sh/uv/) (`pip install uv`)
- PostgreSQL 15+ running locally (or Docker)
- Firebase service account JSON
- Google Play service account JSON (for billing)

### Setup

```bash
cd backend

# Install dependencies
uv sync

# Copy and fill in environment variables
cp .env.example .env

# Run database migrations
uv run alembic upgrade head

# Start dev server with reload
uv run uvicorn app.main:app --reload --port 8000
```

API docs available at: http://localhost:8000/docs

### Running Tests

```bash
uv run pytest --cov=app --cov-report=term-missing
```

### Linting & Type Checking

```bash
uv run ruff check app
uv run ruff format app
uv run mypy app
```

## Database Migrations

```bash
# Create a new migration
uv run alembic revision --autogenerate -m "describe your change"

# Apply all pending migrations
uv run alembic upgrade head

# Roll back one step
uv run alembic downgrade -1
```

## Deployment (Cloud Run)

```bash
# Build and push Docker image
docker build -t gcr.io/PROJECT_ID/nurtlina-backend .
docker push gcr.io/PROJECT_ID/nurtlina-backend

# Deploy to Cloud Run
gcloud run deploy nurtlina-backend \
  --image gcr.io/PROJECT_ID/nurtlina-backend \
  --region us-central1 \
  --platform managed \
  --memory 512Mi \
  --cpu 1 \
  --concurrency 40 \
  --max-instances 5 \
  --set-secrets DATABASE_URL=nurtlina-db-url:latest \
  --set-secrets FIREBASE_SERVICE_ACCOUNT_PATH=firebase-sa:latest
```

## Architecture Notes

- All feeding records are created **on the Android client**, never dependent on the server.
- The server provides backup/sync, billing verification, and account management.
- All writes are **local-first** on Android; the backend is an eventual sync target.
- Backend failures must **never** block feeding log creation or notification scheduling.
- See `Tech_Architecture.md` in the root for the full architecture specification.

> **v2.0**: Bottle entity, `bottles` table, and `/sync/bottles` endpoint have been removed. The data model now uses FeedLog as the primary feeding record.
