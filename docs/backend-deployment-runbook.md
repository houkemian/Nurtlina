# Backend Deployment Runbook

Date: 2026-06-07

This runbook covers the MVP FastAPI/Postgres backend path for Nurtlina.

## Required Runtime Services

- PostgreSQL 15+ or Cloud SQL for PostgreSQL.
- Redis only if enabled by `docker-compose.yml` for deployment support tasks.
- Firebase project for Auth token verification.
- Google Play service account with Android Publisher API access.
- HTTPS ingress in front of the FastAPI app.

## Required Secrets

Store these outside git, preferably in the host secret manager or deployment platform secret store:

- `DATABASE_URL`: async SQLAlchemy URL, for example `postgresql+asyncpg://...`.
- `DATABASE_SYNC_URL`: sync SQLAlchemy URL for Alembic, for example `postgresql+psycopg2://...`.
- `FIREBASE_PROJECT_ID`.
- `FIREBASE_SERVICE_ACCOUNT_PATH`: mounted service account JSON path.
- `GOOGLE_PLAY_SERVICE_ACCOUNT_PATH`: mounted Android Publisher service account JSON path.
- `GOOGLE_PLAY_PACKAGE_NAME`: `com.nurtlina.app` unless the package changes.
- `ALLOWED_ORIGINS`: production domains only. Do not use `*` in production.

## Minimum Service Account Permissions

Firebase service account:

- Verify Firebase Auth ID tokens.
- Revoke/delete the authenticated Firebase user during account deletion.

Google Play service account:

- Android Publisher API read access for subscriptions and one-time products.
- Pub/Sub push should be restricted at the ingress or platform edge to the expected service account/audience.

## Deployment Steps

1. Build the backend image from `backend/Dockerfile`.
2. Provide all required secrets as environment variables or mounted secret files.
3. Run database migrations before replacing the serving container:

```bash
uv run alembic upgrade head
```

4. Start or roll the backend service.
5. Check `/health` and confirm the deployed `version` and UTC `time` fields.
6. Trigger one authenticated `/api/v1/me/init` request from a staging user.
7. Verify logs do not contain service account content, purchase tokens, baby notes, or raw personal records.

## Rollback

1. Roll back the container image to the previous known-good tag.
2. Only run Alembic downgrade when the failed migration is known to be reversible and data-safe.
3. If rollback follows a partial migration, take a database snapshot first.

## Backup And Restore

- Enable managed Postgres automated backups before launch.
- Take a manual snapshot before each production migration.
- Test restore into a staging database at least once before public launch.

## Health And Observability

- `/health` must not require database or Firebase access.
- Request logs include `X-Request-Id`.
- Alert on sustained 5xx responses, failed migrations, and database connection failures.
- Billing RTDN failures should be logged without raw purchase tokens.

## Local Verification

```bash
cd backend
uv run pytest
uv run ruff check app
uv run ruff format --check app
```

Tests use dummy values from `app/tests/conftest.py`; real production secrets are not required for unit test import.
