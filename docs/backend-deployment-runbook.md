# Backend Deployment Runbook

Date: 2026-06-07

## English

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


## 中文

本 runbook 覆盖 Nurtlina MVP 的 FastAPI/Postgres 后端部署路径。

## 必需运行服务

- PostgreSQL 15+ 或 Cloud SQL for PostgreSQL。
- Redis 仅在 `docker-compose.yml` 启用部署辅助任务时需要。
- 用于 Auth token 验证的 Firebase project。
- 具备 Android Publisher API 访问权限的 Google Play service account。
- FastAPI app 前方必须有 HTTPS ingress。

## 必需密钥

这些配置必须存放在 git 之外，优先使用主机 secret manager 或部署平台 secret store：

- `DATABASE_URL`：async SQLAlchemy URL，例如 `postgresql+asyncpg://...`。
- `DATABASE_SYNC_URL`：Alembic 使用的 sync SQLAlchemy URL，例如 `postgresql+psycopg2://...`。
- `FIREBASE_PROJECT_ID`。
- `FIREBASE_SERVICE_ACCOUNT_PATH`：挂载后的 service account JSON 路径。
- `GOOGLE_PLAY_SERVICE_ACCOUNT_PATH`：挂载后的 Android Publisher service account JSON 路径。
- `GOOGLE_PLAY_PACKAGE_NAME`：默认为 `com.nurtlina.app`，除非 package 发生变更。
- `ALLOWED_ORIGINS`：只填写生产域名。生产环境不得使用 `*`。

## 最小 Service Account 权限

Firebase service account：

- 验证 Firebase Auth ID tokens。
- 账号删除时 revoke/delete 已认证的 Firebase user。

Google Play service account：

- 对订阅和一次性商品具备 Android Publisher API read access。
- Pub/Sub push 应在 ingress 或平台边缘限制为预期 service account/audience。

## 部署步骤

1. 使用 `backend/Dockerfile` 构建后端镜像。
2. 通过环境变量或挂载 secret files 提供所有必需密钥。
3. 替换服务容器前先运行数据库迁移：

```bash
uv run alembic upgrade head
```

4. 启动或滚动更新后端服务。
5. 检查 `/health`，确认部署后的 `version` 和 UTC `time` 字段。
6. 使用 staging 用户触发一次已认证的 `/api/v1/me/init` 请求。
7. 确认日志不包含 service account 内容、purchase tokens、baby notes 或原始个人记录。

## 回滚

1. 将容器镜像回滚到上一个已知可用 tag。
2. 只有在确认失败 migration 可逆且不会造成数据风险时，才运行 Alembic downgrade。
3. 如果回滚发生在部分 migration 之后，先创建数据库快照。

## 备份与恢复

- 发布前启用托管 Postgres 自动备份。
- 每次生产 migration 前创建手动快照。
- 公测前至少在 staging 数据库中演练一次恢复。

## 健康检查与可观测性

- `/health` 不应依赖数据库或 Firebase。
- 请求日志应包含 `X-Request-Id`。
- 对持续 5xx、migration 失败和数据库连接失败设置告警。
- Billing RTDN 失败应记录日志，但不得记录原始 purchase tokens。

## 本地验证

```bash
cd backend
uv run pytest
uv run ruff check app
uv run ruff format --check app
```

测试使用 `app/tests/conftest.py` 中的 dummy values；unit test import 不需要真实生产密钥。
