# Central do Alfabeto - Server (API)

[![CI](https://github.com/Central-do-alfabeto/Central-do-Alfabeto-Servidor/actions/workflows/ci.yml/badge.svg)](https://github.com/Central-do-alfabeto/Central-do-Alfabeto-Servidor/actions/workflows/ci.yml)

Leia em português: [LEIA-ME.md](LEIA-ME.md)

## Table of contents

- [Overview](#overview)
- [Features](#features)
- [Stack and architecture](#stack-and-architecture)
- [Endpoints](#endpoints)
- [Prerequisites](#prerequisites)
- [Environment variables](#environment-variables)
- [Running locally](#running-locally)
- [Database and migrations](#database-and-migrations)
- [Tests](#tests)
- [Validation performed](#validation-performed)
- [Continuous integration](#continuous-integration)
- [Deployment](#deployment)
- [Implementation details and known limitations](#implementation-details-and-known-limitations)
- [Credits](#credits)

## Overview

Central do Alfabeto is a literacy game driven by voice: children listen to letters and syllables, repeat them aloud and answer exercises, while an educator follows their progress. It was built as an extension project at IFSP (Instituto Federal de Sao Paulo, campus Campos do Jordao, CJO).

This repository is the **backend API** only. It handles accounts, the educator-student relationship, per-phase progress and (optionally) speech transcription.

- Frontend repository: <https://github.com/Central-do-alfabeto/Central-do-Alfabeto-FrontEnd>
- Public demo: <https://central-do-alfabeto.github.io/Central-do-Alfabeto-FrontEnd/>

> The public demo runs **without this backend**. There is no hosted instance of this API; to use it you have to run it yourself as described below.

## Features

- Registration and login for two roles: student (`STUDENT`) and educator (`EDUCATOR`).
- Passwords stored as BCrypt hashes; stateless authentication with JWT (HS256).
- Students record one progress snapshot per phase (errors and audio replays).
- Educators link students to themselves (by e-mail and exact name), list them and read their per-phase progress.
- Speech transcription endpoint (Vosk, Portuguese small model) that receives a WAV file and returns text. It is an optional capability: if the model cannot be loaded the rest of the API keeps working.
- Input validation with JSON `400` responses listing the invalid fields.
- CORS restricted to a configurable list of origins.

## Stack and architecture

| Area | Technology |
| --- | --- |
| Language / runtime | Java 17 (built and tested locally with JDK 21) |
| Framework | Spring Boot 3.5 (Web, Data JPA, Security, Validation) |
| Database | PostgreSQL (production/dev), H2 in PostgreSQL mode (tests) |
| Migrations | Flyway |
| Authentication | JWT via jjwt 0.11.5, BCrypt |
| Speech recognition | Vosk 0.3.45 with `vosk-model-small-pt-0.3` |
| Build | Maven (wrapper included) |

Package layout under `com.centraldoalfabeto.game`:

- `domain.controller` - REST controllers (`/auth`, `/players`, `/educators`, `/audio`).
- `service` - business rules (`AuthService`, `JogadorService`, `EducadorService`, `ProgressService`, `JwtService`, `VoskTranscriptionService`).
- `repository` - Spring Data JPA repositories.
- `domain.model` - JPA entities: `User`, `PlayerData`, `EducatorStudentLink`.
- `dto` - request/response objects, with Bean Validation annotations.
- `security` and `config` - JWT filter, security filter chain, CORS and the validation error handler.

**Authentication.** `POST /auth/login` returns a JWT containing the user id (subject), role and e-mail; it expires after `JWT_EXPIRATION_MS` (24 hours by default). A filter reads the `Authorization: Bearer <token>` header and populates the security context. The role is taken from the `metadados` JSON column of the user (`{"role":"student"}` or `{"role":"educator"}`), set at registration. Login ignores any role sent by the client and refuses accounts without a recognized role.

**Authorization.** `/players/**` requires `STUDENT`, `/educators/**` requires `EDUCATOR` (except the public register routes), and controllers additionally check that the id in the path equals the authenticated user. A student can only write their own progress; an educator can only read progress of students linked to them.

**Vosk.** The model is loaded at startup from the `vosk-model/` directory relative to the working directory. If loading fails (missing model, unsupported native library), an error is logged and only `/audio/transcribe` is affected.

## Endpoints

| Method | Path | Authentication | Description |
| --- | --- | --- | --- |
| POST | `/auth/login` | Public | Logs in with `email` and `password`; returns token, role and role-specific data (next phase for students, linked students for educators). |
| POST | `/players/register` | Public | Registers a student (`nome`, `email`, `senha`) and returns a token. `409` if the e-mail exists (compared case-insensitively). |
| POST | `/educators/register` | Public | Registers an educator (same body) and returns a token. |
| POST | `/players/{id}/progress` | `STUDENT`, own id | Stores the snapshot of a phase (`currentPhaseIndex` 0 to 25, `errorsData`, `soundRepeatsData`). `400` if the phase skips ahead of the next expected one; `409` if the phase was already stored. |
| GET | `/educators/{id}/students` | `EDUCATOR`, own id | Lists the educator's linked students. |
| POST | `/educators/{id}/students` | `EDUCATOR`, own id | Links a student; body `email` and `studentName`, which must match the student's registration. Any mismatch returns the same generic `400`, so accounts cannot be enumerated. |
| GET | `/educators/student-progress/{studentId}` | `EDUCATOR`, linked student | Returns totals and per-phase snapshots of a linked student. |
| PUT | `/educators/{id}/updateStudentIds` | `EDUCATOR`, own id | Body is a list of student ids; can only keep or remove students already linked (`400` if it contains anyone else). |
| POST | `/audio/transcribe` | Any authenticated user (student or educator) | Multipart field `audioFile`: WAV PCM 16-bit, mono, 16 kHz, up to 1 MB (about 30 s). Returns the text as `text/plain`; **when nothing is recognized the response is `200` with an empty body** (the frontend treats an empty string as "no result"). `415` for other formats, `413` if too large, `503` if the speech model is unavailable. |

Error bodies are JSON. Validation failures return `400` with `{"error": "Dados inválidos", "fields": {"campo": "mensagem"}}`. Missing, invalid or expired tokens return `401` with `{"error": "..."}`; `403` is used only for lack of permission. Unique-constraint violations (concurrent registration or duplicated phase progress) return `409`. Registration requires a name, a valid e-mail and a password of 8 to 72 characters.

### Error codes

All error bodies are JSON (`{"error": "..."}`), with or without a token.

| Status | When |
| --- | --- |
| 400 | Validation failure (with `fields`), malformed JSON, missing parameter or multipart part, invalid value in the URL (e.g. a non-UUID id), phase out of sequence or out of range, generic student-link failure |
| 401 | Missing, invalid or expired token (also for unknown routes without a token) |
| 403 | Authenticated but not allowed (wrong role or another user's id) |
| 404 | Unknown route (with a valid token) |
| 405 | HTTP method not supported for the route |
| 409 | Duplicate e-mail, duplicate phase progress, student already linked, unique-constraint violation |
| 413 | Upload larger than 1 MB |
| 415 | Unsupported content type, or audio that is not WAV PCM 16-bit mono 16 kHz |
| 500 | Unexpected failure (generic message, details only in the server log) |
| 503 | Speech model not loaded |

## Prerequisites

- JDK 17 or newer (`JAVA_HOME` must point to it; Maven itself is downloaded by the wrapper).
- PostgreSQL 14+ for running the API without Docker.
- [Git LFS](https://git-lfs.com/) so that the Vosk model files (`*.fst`) are downloaded on clone.
- Docker with Compose, optional.

## Environment variables

| Variable | Required | Default | Description |
| --- | --- | --- | --- |
| `DATABASE_URL` | Yes | - | JDBC URL, e.g. `jdbc:postgresql://localhost:5432/centraldoalfabeto`. |
| `DATABASE_USERNAME` | Yes | - | Database user. |
| `DATABASE_PASSWORD` | Yes | - | Database password. |
| `JWT_SECRET` | Yes | - | Signing secret, at least 32 characters. The application refuses to start without it. |
| `JWT_EXPIRATION_MS` | No | `86400000` | Token lifetime in milliseconds. |
| `CORS_ALLOWED_ORIGINS` | No | `http://localhost:5173` | Comma-separated list of allowed origins. |
| `VOSK_MODEL_PATH` | No | `vosk-model` | Directory of the Vosk model, relative to the working directory. |

See `.env.example` for fictitious values. The application does **not** read `.env` by itself; export the variables in your shell. Docker Compose reads a `.env` file in this directory for variable substitution. `.env` is ignored by Git.

## Running locally

### With Docker Compose

```bash
git lfs pull                       # make sure the Vosk model is present
cp .env.example .env               # then edit the values
docker compose up --build
```

Compose starts PostgreSQL 16 (published only on `127.0.0.1:5432`) and the API on `http://localhost:8080`. The `DATABASE_URL` inside Compose points to the `db` service. The Docker setup was run locally; see [Validation performed](#validation-performed).

### Without Docker

1. Create an empty PostgreSQL database and user.
2. Make sure `vosk-model/` contains the model. If it does not, run `./download_vosk_model.sh` (Bash, needs `curl` and `unzip`), which downloads `vosk-model-small-pt-0.3` from alphacephei.com into `vosk-model/`.
3. Export the variables and start the API from the repository root (the model path is relative to the working directory):

```bash
export DATABASE_URL=jdbc:postgresql://localhost:5432/centraldoalfabeto
export DATABASE_USERNAME=alfabeto
export DATABASE_PASSWORD=change-me
export JWT_SECRET=$(openssl rand -base64 48)
./mvnw spring-boot:run
```

On PowerShell use `$env:NOME = "valor"` for each variable. Flyway creates the schema on first start.

To use it with the frontend, run the frontend in dev mode (default origin `http://localhost:5173`) pointing to `http://localhost:8080`.

## Database and migrations

The schema is versioned with Flyway in `src/main/resources/db/migration`. `V1__schema_inicial.sql` creates `users`, `player_data` and `educators_students`. Hibernate runs with `ddl-auto=validate`, so it never alters the schema; any schema change must be a new migration.

`V2__email_minusculo.sql` lowercases existing e-mails and adds a `CHECK` that forces e-mails to be lowercase; together with the `UNIQUE` constraint of `V1` this makes e-mail uniqueness case-insensitive in both PostgreSQL and H2. It fails on purpose if two existing accounts differ only by letter case. E-mails are trimmed and lowercased on registration, login and student linking.

For a **pre-existing database** that already has these tables (for example one created before Flyway was introduced), do not run `V1` over it. Baseline it first so that Flyway marks `V1` as applied, e.g. with `spring.flyway.baseline-on-migrate=true` and `spring.flyway.baseline-version=1` for the first start, and check that the existing columns match `V1`. This validation was only exercised on H2, not against a real PostgreSQL.

## Tests

```bash
./mvnw -B test
```

78 tests: service unit tests (Mockito) and integration tests with MockMvc covering authorization between users, input validation, login role handling and CORS. They run against an in-memory H2 database in PostgreSQL mode and need no external database. The same Flyway migration is applied; the `jsonb` type is emulated in H2 through a domain created in the test JDBC URL. The Vosk model is not loaded in tests (`vosk.model-path` is empty) and the recognizer is mocked.

## Validation performed

Besides the automated tests, the Docker setup was validated manually on a development machine: the image was built, started with Compose together with PostgreSQL 16 (disposable credentials, non-default host ports), and then removed. Observed: Flyway applied `V1` and `V2`; Hibernate `ddl-auto=validate` passed; the Vosk model loaded inside the image; the application ran as a non-root user using about 400 to 470 MiB of memory; and the registration, login, progress, student-link and error-code scenarios above behaved as documented, including a real transcription request of a silent WAV (`200`, empty body). It has **never been deployed** to a hosting platform.

## Continuous integration

`.github/workflows/ci.yml` runs `./mvnw -B test` with JDK 17 on every push and pull request to `main` (with Git LFS enabled for checkout).

## Deployment

There is **no public hosted instance** of this API, and no deployment configuration beyond the Dockerfile. Hosting it would require:

- a Java 17 runtime (or the provided image) and a managed PostgreSQL;
- the environment variables above, with a strong `JWT_SECRET`, and `CORS_ALLOWED_ORIGINS` set to the frontend origin;
- HTTPS in front of the API (it serves plain HTTP);
- enough memory for Vosk: in a local measurement the process used roughly 350 to 450 MB of RAM, mostly the speech model and native memory. Free tiers with 512 MB are very tight; the Dockerfile sets `-XX:MaxRAMPercentage=60` and `JAVA_OPTS` can be overridden;
- the Vosk model inside the image (`COPY vosk-model`), which needs Git LFS content at build time;
- a glibc-based image; the native Vosk library does not work on Alpine.

## Implementation details and known limitations

- **Student-educator link has no student consent.** An educator links a student knowing only their e-mail and exact name; the student is not asked to accept.
- **Speech accuracy.** The Vosk small Portuguese model is used without a restricted grammar, so accuracy is low for very short items such as single letters. The endpoint validates the WAV format and size, but not audio quality or duration.
- **Progress.** `currentPhaseIndex` is limited to 0 to 25 (the game has 26 phases) and phases cannot be skipped: a snapshot is accepted only for the next expected phase or an earlier one not yet stored. The server trusts the error and replay counters sent by the client.
- **Tokens.** JWTs cannot be revoked and are not checked against the database on each request; there are no refresh tokens, rate limiting or e-mail verification.
- **Roles** live in a JSON column (`metadados`) rather than in a dedicated column; they are read in a single place (`UserRole`) with structured JSON parsing.
- **Passwords** are limited to 72 characters (BCrypt limit).
- **Docker**: the image and Compose setup were built and run locally (see [Validation performed](#validation-performed)); they were never deployed to a hosting platform.
- **PostgreSQL**: the automated tests use H2. Migrations and schema validation were also exercised against a real PostgreSQL 16 in the local Docker run, but not as part of the CI workflow.
- The repository history contains old credentials from before the cleanup; they should be considered compromised and rotated.
- The Eclipse output folder `bin/` is no longer versioned (it is ignored). Earlier history still carries its large files.

## Credits

Project built at IFSP-CJO by:

- Brian Gustavo de Oliveira Brandao
- Tiago J. Saraiva
- Luiz Henrique de Oliveira Freitas

(Names taken from the Git history of this repository.)
