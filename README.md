# BNR Bank Licensing & Compliance Portal

**Author: David NTAMAKEMWA**

This is a bank licensing portal built for the National Bank of Rwanda. It replaces a process that was entirely manual — applications coming in by email, documents tracked in spreadsheets, approvals passed around informally. The goal was a single system where applications move through a defined workflow, every action is recorded permanently, and no one can approve something they personally reviewed.

---

## Running with Docker (easiest way)

You need [Docker Desktop](https://www.docker.com/products/docker-desktop/) installed. That's it — no Java, Maven, Node or PostgreSQL needed on your machine.

```bash
git clone <your-repo-url>
cd bnr-licensing-portal

docker compose up --build
```

The first build will take a few minutes while it pulls images and downloads Maven dependencies. After that, subsequent starts are much faster because Docker caches the layers.

| What | URL |
|------|-----|
| Frontend | http://localhost:3000 |
| Backend API | http://localhost:8080 |
| Swagger UI | http://localhost:8080/swagger-ui.html |

```bash
# Stop containers
docker compose down

# Stop and delete all data (fresh start)
docker compose down -v
```

---

## Test accounts

When the app starts for the first time, Flyway runs the seed migration automatically. You can log in immediately with any of these accounts — no manual database setup needed.

| Role | Email | Password |
|------|-------|----------|
| Admin | admin@bnr.rw | Admin@12345 |
| Reviewer | reviewer@bnr.rw | Review@12345 |
| Approver | approver@bnr.rw | Approve@12345 |
| Applicant | applicant@example.rw | Apply@12345 |

Two applications are also seeded so you can see the workflow in action without creating anything from scratch:

- **Kigali Community Savings Bank** — status `SUBMITTED`. Log in as admin, assign the reviewer, then switch to the reviewer account and work through the review.
- **Rwanda Digital Finance Ltd** — status `REVIEW_COMPLETE`, already reviewed by `reviewer@bnr.rw`. Log in as `approver@bnr.rw` to test the approval. If you try to approve it while logged in as the reviewer, it will be rejected — the system enforces that the person who reviews an application cannot be the same person who makes the final call.

---

## Running the tests

```bash
cd bnr-backend

./mvnw test -Dgroups=unit          # fast, no database needed
./mvnw test -Dgroups=integration   # needs Docker (Testcontainers)
./mvnw test                        # everything
```

The concurrency test is worth running if you want to see the optimistic locking in action — it fires two simultaneous approval requests at the same application and asserts that exactly one succeeds and the other gets a 409.

---

## Running locally without Docker

If you want to run the backend and frontend directly:

**You need:** Java 21+, Maven 3.9+, PostgreSQL 15+, Node.js 18+

**Database:**
```bash
psql -U postgres -c "CREATE DATABASE bnr_portal;"
psql -U postgres -c "CREATE USER bnr_user WITH PASSWORD 'changeme';"
psql -U postgres -c "GRANT ALL PRIVILEGES ON DATABASE bnr_portal TO bnr_user;"
```

**Backend:**
```bash
cd bnr-backend
cp .env.example .env   # fill in your DB credentials and JWT secrets
./mvnw spring-boot:run
```

Flyway runs migrations and seeds the database on startup. API available at `http://localhost:8080`.

**Frontend:**
```bash
cd bnr-frontend
cp .env.example .env   # set VITE_API_URL=http://localhost:8080
npm install
npm run dev
```

Frontend at `http://localhost:5173`.

---

## Project layout

```
bnr-licensing-portal/
├── docker-compose.yml
├── bnr-backend/                        Spring Boot 3.3, Java 21
│   ├── src/main/java/rw/bnr/licensing/
│   │   ├── auth/                       JWT filter, token service, login endpoints
│   │   ├── application/                State machine, service, REST controllers
│   │   ├── document/                   Upload, versioning, download
│   │   ├── audit/                      Append-only log, query service
│   │   ├── user/                       User listing endpoint
│   │   ├── domain/                     JPA entities and repositories
│   │   └── common/                     Global exception handler, ApiResponse envelope
│   └── src/main/resources/db/migration/
│       ├── V1__create_schema.sql
│       ├── V2__audit_log_insert_only_grants.sql
│       ├── V3__seed_data.sql
│       └── V4__convert_enums_to_varchar.sql
└── bnr-frontend/                       React 18, Vite, TypeScript
    └── src/
        ├── api/                        Axios client with JWT interceptors
        ├── context/                    Auth state
        ├── pages/                      Login, Dashboard, Application detail, Admin pages
        └── components/                 Shell, StatusBadge, Spinner, etc.
```

---

## Architecture

```
Browser (React SPA)
      |
      | HTTP/REST
      |
Spring Boot API  ←  auth, role checks, state machine
      |
PostgreSQL  ←  ACID, optimistic locking, append-only audit grants
```

Standard three-tier. I chose a monolith over microservices — at the scale of one regulatory body processing licensing applications, splitting into services would add distributed-systems overhead without adding anything useful. The internal package boundaries (auth, application, document, audit) are clean enough that extraction would be straightforward later if load ever demanded it.

---

## Roles

| Role | What they can do |
|------|-----------------|
| Applicant | Create, edit (draft only), submit, withdraw, resubmit applications; upload documents |
| Reviewer | View and act on applications assigned to them; request more info; complete review |
| Approver | Make the final approve or reject decision — but not on applications they reviewed |
| Admin | Assign reviewers; view all non-draft applications; view global audit log; manage users |

Role enforcement is in the backend. A user who strips the frontend and hits the API directly still gets 403 on anything they are not allowed to do.

---

## Application lifecycle

```
DRAFT → SUBMITTED → UNDER_REVIEW ──→ INFO_REQUESTED → RESUBMITTED ─┐
                          │                                           │
                          └─────────── REVIEW_COMPLETE ──────────────┤
                                              │
                                    ┌─────────┴──────────┐
                                 APPROVED             REJECTED
                                 (terminal)           (terminal)

Any state before a terminal → WITHDRAWN (also terminal)
```

Illegal transitions return 422. Once something is approved, rejected or withdrawn it stays that way — there is no undo.

---

## Some decisions I made and why

**Auth:** JWT with short-lived access tokens (15 min) and a refresh token stored in the database. Logout revokes the refresh token immediately. I went with JWT over sessions because there is no session store to manage — the backend stays stateless.

**Separation of duties:** The rule that the reviewer cannot also approve is enforced in three independent places: the service layer, a database CHECK constraint, and the UI. The idea is that if one layer has a bug, the others still hold.

**Audit trail:** The application database user has INSERT on `audit_logs` and nothing else. No UPDATE, no DELETE — enforced in the V2 Flyway migration at the PostgreSQL grant level, not just by convention in application code. The table uses BIGSERIAL as the primary key, so a gap in the sequence is evidence that a row was deleted.

**Concurrency:** Every row in the applications table has a `@Version` column. If two users try to act on the same application at the same time, whoever saves second sees `rows_affected = 0` and gets a 409. I chose optimistic locking over pessimistic because with pessimistic locking, holding a row lock while a user reads an application and thinks about what to do would block everyone else from touching it for an unpredictable amount of time.

**Document versioning:** When an applicant resubmits after an information request, the previous documents are marked superseded and new ones are saved with an incremented version number. Nothing is ever deleted. Reviewers can always look back at what was originally submitted.

---

## API

Every response uses the same envelope:

```json
{ "success": true,  "data": { ... }, "error": null }
{ "success": false, "data": null,    "error": { "code": "INVALID_TRANSITION", "message": "..." } }
```

Unexpected server errors never leak a stack trace. The server logs the full error with a reference ID and returns only that ID to the client.

Interactive docs at `http://localhost:8080/swagger-ui.html`.

### Auth
| Method | Path | Notes |
|--------|------|-------|
| POST | `/api/auth/login` | Returns access token + refresh token |
| POST | `/api/auth/refresh` | Get a new access token using the refresh token |
| POST | `/api/auth/logout` | Revokes the refresh token immediately |

### Applications
| Method | Path | Who can call it |
|--------|------|-----------------|
| POST | `/api/applications` | Applicant |
| GET | `/api/applications` | Everyone (filtered by role) |
| GET | `/api/applications/:id` | Owner / assigned reviewer / approver / admin |
| PUT | `/api/applications/:id` | Applicant, draft only |
| PATCH | `/api/applications/:id/submit` | Applicant (owner) |
| PATCH | `/api/applications/:id/assign-reviewer` | Admin |
| PATCH | `/api/applications/:id/request-info` | Assigned reviewer |
| PATCH | `/api/applications/:id/complete-review` | Assigned reviewer |
| PATCH | `/api/applications/:id/resubmit` | Applicant (owner) |
| PATCH | `/api/applications/:id/approve` | Approver, not the reviewer of this application |
| PATCH | `/api/applications/:id/reject` | Approver, not the reviewer of this application |
| PATCH | `/api/applications/:id/withdraw` | Applicant (owner) |

### Documents
| Method | Path | Notes |
|--------|------|-------|
| POST | `/api/applications/:id/documents` | Max 5 MB, enforced server-side before touching disk |
| GET | `/api/applications/:id/documents` | Current version only |
| GET | `/api/applications/:id/documents/history` | All versions including superseded |
| GET | `/api/documents/:docId/download` | Authenticated, access-checked |

### Audit
| Method | Path | Who can call it |
|--------|------|-----------------|
| GET | `/api/applications/:id/audit` | All roles — applicants see only their own |
| GET | `/api/audit-logs` | Admin only, paginated |

---

## What's missing and why I left it out

**Cryptographic hash chain on the audit log.** The INSERT-only grant stops tampering through the application layer. It does not stop a database administrator with superuser access. A production system would add a hash chain over log entries and ship them periodically to a notarised external store. I scoped that out of v1 because it requires additional infrastructure decisions that are better made with the operations team.

**Immediate token revocation.** If an access token is stolen it goes stale in 15 minutes on its own. Revoking it instantly across all sessions would need a Redis blacklist. That's a reasonable addition but I didn't want to pull in another infrastructure dependency without a concrete requirement for it.

**Email or SMS notifications.** Nothing is sent when an application changes state. You'd need a message queue (RabbitMQ or similar) behind the backend and a notification panel in the UI. Intentionally deferred.

**2FA.** I'd want this for Reviewer, Approver and Admin roles before putting this in front of real users.

**Appeals.** A rejected application currently requires starting fresh with a new submission. An appeal flow would need its own states and its own rules around who can see what — I didn't want to design that without input on how it should actually work.
