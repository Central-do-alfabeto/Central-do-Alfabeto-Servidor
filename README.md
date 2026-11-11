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
| POST | `/players/register` | Public | Registers a student (`nome`, `email`, `senha`) and returns a token. `409` if the e-mail exists. |
| POST | `/educators/register` | Public | Registers an educator (same body) and returns a token. |
| POST | `/players/{id}/progress` | `STUDENT`, own id | Stores the snapshot of a phase (`currentPhaseIndex`, `errorsData`, `soundRepeatsData`). `409` if the phase was already stored. |
| GET | `/educators/{id}/students` | `EDUCATOR`, own id | Lists the educator's linked students. |
| POST | `/educators/{id}/students` | `EDUCATOR`, own id | Links a student; body `email` and `studentName`, which must match the student's registration. |
| GET | `/educators/student-progress/{studentId}` | `EDUCATOR`, linked student | Returns totals and per-phase snapshots of a linked student. |
| PUT | `/educators/{id}/updateStudentIds` | `EDUCATOR`, own id | Body is a list of student ids; can only keep or remove students already linked (`400` if it contains anyone else). |
| POST | `/audio/transcribe` | Any authenticated user | Multipart field `audioFile` (WAV, 16 kHz); returns the transcribed text as plain text. |

Validation failures return `400` with `{"error": "Dados inválidos", "fields": {"campo": "mensagem"}}`. Registration requires a name, a valid e-mail and a password of 8 to 72 characters.

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

See `.env.example` for fictitious values. The application does **not** read `.env` by itself; export the variables in your shell. Docker Compose reads a `.env` file in this directory for variable substitution. `.env` is ignored by Git.

## Running locally

### With Docker Compose

```bash
git lfs pull                       # make sure the Vosk model is present
cp .env.example .env               # then edit the values
docker compose up --build
```

Compose starts PostgreSQL 16 (published only on `127.0.0.1:5432`) and the API on `http://localhost:8080`. The `DATABASE_URL` inside Compose points to the `db` service. Note: the Docker setup has not been run end to end; only the Compose file syntax was validated.

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

For a **pre-existing database** that already has these tables (for example one created before Flyway was introduced), do not run `V1` over it. Baseline it first so that Flyway marks `V1` as applied, e.g. with `spring.flyway.baseline-on-migrate=true` and `spring.flyway.baseline-version=1` for the first start, and check that the existing columns match `V1`. This validation was only exercised on H2, not against a real PostgreSQL.

## Tests

```bash
./mvnw -B test
```

38 tests: service unit tests (Mockito) and integration tests with MockMvc covering authorization between users, input validation, login role handling and CORS. They run against an in-memory H2 database in PostgreSQL mode and need no external database. The same Flyway migration is applied; the `jsonb` type is emulated in H2 through a domain created in the test JDBC URL. The Spring context also tries to load the Vosk model from `vosk-model/`.

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
- **Speech accuracy.** The Vosk small Portuguese model is used without a restricted grammar, so accuracy is low for very short items such as single letters. The transcript is extracted from Vosk's JSON by string search, and the endpoint does not validate the WAV format, sample rate or size.
- **Progress.** The server accepts any non-negative phase index and does not enforce the order or a maximum number of phases.
- **Tokens.** JWTs cannot be revoked and are not checked against the database on each request; there are no refresh tokens, rate limiting or e-mail verification.
- **Roles** live in a JSON column and are matched by name, rather than in a dedicated column.
- **Passwords** are limited to 72 characters (BCrypt limit).
- **Docker** images and Compose were not executed in the environment used to prepare this repository.
- **PostgreSQL** was not used in automated tests; schema validation against it is unverified.
- The repository history contains old credentials from before the cleanup; they should be considered compromised and rotated.
- The Eclipse output folder `bin/` is no longer versioned (it is ignored). Earlier history still carries its large files.

## Credits

Project built at IFSP-CJO by:

- Brian Gustavo de Oliveira Brandao
- Tiago J. Saraiva
- Luiz Henrique de Oliveira Freitas

(Names taken from the Git history of this repository.)
