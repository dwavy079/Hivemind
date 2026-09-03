# FileShare

A file-sharing app: upload files, organize them into folders, keep every previous version, and hand out shareable download links. Backend is Spring Boot (Java), frontend is React (Vite), and file content lives in AWS S3.

## Features (v1)

- **Accounts** — register/login with email + password, JWT access + refresh tokens (refresh tokens rotate on use; logging in again revokes the previous refresh token).
- **Upload / download** — files are stored in S3; the backend never writes them to local disk.
- **Folders** — organize files into nested folders.
- **Versioning** — re-uploading a file with the same name adds a new version instead of overwriting it. Every version stays downloadable.
- **Shareable links** — generate a public, unguessable link to a file, optionally with an expiry. Anyone with the link can download without an account. Links can be revoked, and each records a download count.

## Architecture

```
file-sharing-app/
├── backend/     Spring Boot 3 (Java 17) REST API
└── frontend/    React 18 + Vite SPA
```

- **Auth**: Spring Security + JWT (`io.jsonwebtoken`/jjwt), BCrypt password hashing. Built from scratch for this project rather than reusing an existing template.
- **Storage**: AWS S3 via the AWS SDK v2. The bucket key layout is `users/{userId}/files/{fileItemId}/v{versionNumber}-{filename}`.
- **Database**: JPA/Hibernate. Defaults to an embedded H2 file database for local dev (zero setup); PostgreSQL is used in the `prod` profile.

## Prerequisites

- JDK 17+
- Maven 3.9+
- Node.js 18+ and npm
- An AWS account with an S3 bucket (see below)
- PostgreSQL 14+ — only if you run the `prod` profile; the default `dev` profile needs nothing extra

## 1. Set up the S3 bucket

1. Create a bucket, e.g. `file-sharing-app-bucket`, in your preferred region (e.g. `eu-west-1`). Keep "Block all public access" **on** — the app never makes objects public; downloads are proxied through the backend.
2. Create an IAM user (or role, if deploying to AWS) with a policy scoped to just that bucket:

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": ["s3:PutObject", "s3:GetObject", "s3:DeleteObject"],
      "Resource": "arn:aws:s3:::file-sharing-app-bucket/*"
    }
  ]
}
```

3. Generate an access key for that user (or, if running on EC2/ECS/Lambda, just attach the role — no keys needed, see below).

## 2. Configure and run the backend

The backend reads all config from environment variables (see `backend/src/main/resources/application.yml` for every key and its default).

```bash
cd backend

export AWS_REGION=eu-west-1
export AWS_S3_BUCKET=file-sharing-app-bucket
export AWS_ACCESS_KEY_ID=...        # omit both of these to use the default AWS credential
export AWS_SECRET_ACCESS_KEY=...    # chain (env vars / ~/.aws/credentials / IAM role) instead
export JWT_SECRET=$(openssl rand -base64 32)
export CORS_ALLOWED_ORIGINS=http://localhost:5173
export SHARE_LINK_BASE_URL=http://localhost:5173/s

mvn spring-boot:run
```

The API comes up on `http://localhost:8080`. On first run (dev profile) it creates an H2 database file under `backend/data/`.

To run against PostgreSQL instead:

```bash
docker compose up -d postgres   # from the repo root, or point DB_URL at your own instance
export SPRING_PROFILES_ACTIVE=prod
export DB_URL=jdbc:postgresql://localhost:5432/filesharing
export DB_USERNAME=postgres
export DB_PASSWORD=postgres
mvn spring-boot:run
```

## 3. Configure and run the frontend

```bash
cd frontend
cp .env.example .env
# edit .env if your backend isn't on http://localhost:8080

npm install
npm run dev
```

Open `http://localhost:5173`, create an account, and start uploading.

## API summary

All authenticated endpoints expect `Authorization: Bearer <accessToken>`.

| Method | Path | Description |
| --- | --- | --- |
| POST | `/api/v1/auth/register` | Create an account |
| POST | `/api/v1/auth/authenticate` | Log in |
| POST | `/api/v1/auth/refresh` | Exchange a refresh token for a new token pair |
| POST | `/api/v1/auth/logout` | Revoke the current refresh token |
| GET | `/api/v1/files?folderId=` | List a folder's contents (omit `folderId` for root) |
| POST | `/api/v1/files/folders` | Create a folder |
| POST | `/api/v1/files/upload` | Upload a file (multipart: `file`, `parentId?`, `name?`) — re-uploading an existing name adds a version |
| GET | `/api/v1/files/{id}/download` | Download the latest version |
| GET | `/api/v1/files/{id}/versions` | List a file's versions |
| GET | `/api/v1/files/{id}/versions/{versionId}/download` | Download a specific version |
| PATCH | `/api/v1/files/{id}/rename` | Rename a file or folder |
| PATCH | `/api/v1/files/{id}/move` | Move a file or folder |
| DELETE | `/api/v1/files/{id}` | Delete a file or folder (folders delete recursively) |
| POST | `/api/v1/files/share-links` | Create a share link (`fileItemId`, optional `versionId`, optional `expiresInHours`) |
| GET | `/api/v1/files/{fileItemId}/share-links` | List share links for a file |
| DELETE | `/api/v1/files/share-links/{id}` | Revoke a share link |
| GET | `/api/v1/share/{token}` | Public: get info about a shared file |
| GET | `/api/v1/share/{token}/download` | Public: download a shared file |

## Deploying to AWS (suggested path)

- **Backend**: containerize with a simple `Dockerfile` (build with `mvn package`, run the resulting jar) and deploy to ECS Fargate or Elastic Beanstalk, with an IAM task role granted the S3 policy above instead of static keys. Put RDS PostgreSQL behind it and set `SPRING_PROFILES_ACTIVE=prod`.
- **Frontend**: `npm run build` produces static files in `frontend/dist/` — serve them from S3 + CloudFront, or any static host.
- **Secrets**: put `JWT_SECRET` and DB credentials in AWS Secrets Manager / Parameter Store rather than plain env vars in production.

## CI

`.github/workflows/ci.yml` builds and tests both the backend (`mvn verify`) and frontend (`npm run build`) on every push and PR.
