# BNR Bank Licensing & Compliance Portal

> Regulatory licensing portal for the **National Bank of Rwanda** — replaces the manual email/spreadsheet process with a structured, fully auditable workflow from application submission through document review to final approval.

**Author: David NTAMAKEMWA**

---

## Quick start (Docker — recommended)

The entire stack starts with a single command. No Java, Maven, Node, or PostgreSQL installation required on the host machine.

**Prerequisites:** [Docker Desktop](https://www.docker.com/products/docker-desktop/) installed and running.

```bash
git clone <your-repo-url>
cd bnr-licensing-portal

docker compose up --build
```

First build takes ~3–5 minutes (downloads base images and Maven dependencies). Subsequent starts are fast.

| Service  | URL                          |
|----------|------------------------------|
| Frontend | http://localhost:3000         |
| Backend API | http://localhost:8080     |
| Swagger UI  | http://localhost:8080/swagger-ui.html |

To stop:
```bash
docker compose down
```

To stop and wipe all database data:
```bash
docker compose down -v
```

---

## Seed accounts

The database is seeded automatically on first startup. No manual setup required.

| Role      | Email                      | Password       |
|-----------|----------------------------|----------------|
| Admin     | admin@bnr.rw               | Admin@12345    |
| Reviewer  | reviewer@bnr.rw            | Review@12345   |
| Approver  | approver@bnr.rw            | Approve@12345  |
| Applicant | applicant@example.rw       | Apply@12345    |

**Pre-seeded applications:**

- **Application A** — *Kigali Community Savings Bank* — `SUBMITTED`, ready for reviewer assignment. Log in as `admin@bnr.rw` to assign `reviewer@bnr.rw`.
- **Application B** — *Rwanda Digital Finance Ltd* — `REVIEW_COMPLETE`, reviewed by `reviewer@bnr.rw`. Log in as `approver@bnr.rw` to make the final decision. This application demonstrates the **separation-of-duties rule** — `reviewer@bnr.rw` cannot approve this application because they reviewed it.

---

## Running tests

```bash
# Unit tests only (no database required)
cd bnr-backend
./mvnw test -Dgroups=unit

# Integration + concurrency tests (requires Docker for Testcontainers)
./mvnw test -Dgroups=integration

# All tests
./mvnw test
```

---

## Manual local setup (without Docker)

If you prefer to run without Docker:

### Prerequisites

| Tool       | Version  |
|------------|----------|
| Java       | 21+      |
| Maven      | 3.9+     |
| PostgreSQL | 15+      |
| Node.js    | 18+      |

### 1. Database

```bash
psql -U postgres -c "CREATE DATABASE bnr_portal;"
psql -U postgres -c "CREATE USER bnr_user WITH PASSWORD 'changeme';"
psql -U postgres -c "GRANT ALL PRIVILEGES ON DATABASE bnr_portal TO bnr_user;"
```

### 2. Backend

```bash
cd bnr-backend
cp .env.example .env          # then edit .env with your values
./mvnw spring-boot:run
```

Flyway migrations run automatically on startup and seed the database. The API will be available at `http://localhost:8080`.

### 3. Frontend

```bash
cd bnr-frontend
cp .env.example .env          # VITE_API_URL=http://localhost:8080
npm install
npm run dev
```

Frontend runs at `http://localhost:5173`.

---

## Repository structure

```
bnr-licensing-portal/
├── docker-compose.yml          # Start the full stack
├── bnr-backend/                # Spring Boot 3.3 / Java 21 API
│   ├── src/
│   │   ├── main/java/rw/bnr/licensing/
│   │   │   ├── auth/           # JWT authentication
│   │   │   ├── application/    # Workflow service + state machine
│   │   │   ├── document/       # File upload + versioning
│   │   │   ├── audit/          # Append-only audit trail
│   │   │   ├── domain/         # JPA entities + repositories
│   │   │   └── common/         # Exception handling + API response envelope
│   │   └── resources/
│   │       └── db/migration/   # Flyway migrations (V1–V4)
│   └── src/test/               # Unit + integration tests
└── bnr-frontend/               # React 18 + Vite + TypeScript SPA
    └── src/
        ├── api/                # Typed API client (axios)
        ├── context/            # Auth context
        ├── pages/              # Login, Dashboard, Application detail
        └── components/         # Shell layout, StatusBadge, Spinner, etc.
```

---

## Architecture

```
┌─────────────────────────────────┐
│     React SPA  :3000            │  Role-driven UI — actions invisible if not permitted
└────────────────┬────────────────┘
                 │ HTTP / REST (JSON)
┌────────────────▼────────────────┐
│  Spring Boot API  :8080         │
│  ├─ Spring Security (JWT)       │  Auth + RBAC — enforced in backend, not just UI
│  ├─ ApplicationStateMachine     │  All workflow transitions validated here
│  ├─ AuditService                │  Every mutation logged, append-only
│  └─ DocumentService             │  Local file store, 5 MB limit, versioning
└────────────────┬────────────────┘
                 │
┌────────────────▼────────────────┐
│  PostgreSQL  :5432              │  ACID, row-level locking, @Version optimistic lock
└─────────────────────────────────┘
```

---

## Roles and what each can do

| Role      | Capabilities                                                                 |
|-----------|------------------------------------------------------------------------------|
| Applicant | Create, edit (DRAFT only), submit, withdraw, resubmit applications; upload documents |
| Reviewer  | View submitted applications; request additional information; complete review  |
| Approver  | Make final approve/reject decision — **cannot approve applications they reviewed** |
| Admin     | Assign reviewers; view all applications; view full audit log; manage users   |

---

## Application states

```
DRAFT → SUBMITTED → UNDER_REVIEW → INFO_REQUESTED → RESUBMITTED ─┐
                         │                                          │
                         └──────────── REVIEW_COMPLETE ────────────┤
                                              │
                                    ┌─────────┴─────────┐
                                 APPROVED            REJECTED
                                 (terminal)          (terminal)

Any pre-terminal state → WITHDRAWN (terminal)
```

Illegal transitions return `422 Unprocessable Entity`. Terminal states cannot be left.

---

## Key design decisions

### JWT over sessions
Stateless — no session store to manage. 15-minute access tokens + revocable refresh tokens stored in the database. Logout immediately invalidates the refresh token.

### Separation of duties
The reviewer and approver on the same application **must be different people**. Enforced at three independent levels: service layer check, database `CHECK` constraint, and UI (approve button hidden for the reviewer).

### Audit trail tamper-resistance
- Application DB user is granted `INSERT` only on `audit_logs` — no `UPDATE` or `DELETE`
- `BIGSERIAL` primary key means deletions are detectable through ID gaps
- Timestamp is set by the database clock (`DEFAULT NOW()`), not the application server
- Actor email is denormalised into each log entry so history remains readable after account changes

### Concurrency
Every `applications` row carries a `@Version` counter (optimistic locking). Two users acting on the same application simultaneously: one succeeds, the other gets `409 Conflict` and is asked to refresh. A dedicated Testcontainers integration test asserts this behaviour against a real PostgreSQL instance.

### Document versioning
When an applicant resubmits after an information request, existing documents are marked `superseded = true` and new documents get an incremented `document_version`. Previous documents are never deleted and remain accessible to reviewers.

---

## API overview

All responses follow a consistent envelope:

```json
{ "success": true,  "data": { ... }, "error": null }
{ "success": false, "data": null,    "error": { "code": "INVALID_TRANSITION", "message": "..." } }
```

No raw stack traces are ever returned. Unexpected errors log a server-side reference ID; only that ID is returned to the client.

Full interactive docs at **http://localhost:8080/swagger-ui.html** once the app is running.

### Auth
| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/auth/login` | Authenticate — returns access + refresh tokens |
| POST | `/api/auth/refresh` | Exchange refresh token for new access token |
| POST | `/api/auth/logout` | Revoke refresh token |

### Applications
| Method | Path | Roles |
|--------|------|-------|
| POST | `/api/applications` | Applicant |
| GET | `/api/applications` | All (scoped by role) |
| GET | `/api/applications/:id` | Owner, Reviewer, Approver, Admin |
| PUT | `/api/applications/:id` | Applicant (DRAFT only) |
| PATCH | `/api/applications/:id/submit` | Applicant (owner) |
| PATCH | `/api/applications/:id/assign-reviewer` | Admin |
| PATCH | `/api/applications/:id/request-info` | Reviewer (assigned) |
| PATCH | `/api/applications/:id/complete-review` | Reviewer (assigned) |
| PATCH | `/api/applications/:id/resubmit` | Applicant (owner) |
| PATCH | `/api/applications/:id/approve` | Approver (not the reviewer) |
| PATCH | `/api/applications/:id/reject` | Approver (not the reviewer) |
| PATCH | `/api/applications/:id/withdraw` | Applicant (owner) |

### Documents
| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/applications/:id/documents` | Upload document (max 5 MB, enforced server-side) |
| GET | `/api/applications/:id/documents` | Current (non-superseded) documents |
| GET | `/api/applications/:id/documents/history` | All versions across resubmissions |
| GET | `/api/documents/:docId/download` | Download a specific document |

### Audit
| Method | Path | Roles |
|--------|------|-------|
| GET | `/api/applications/:id/audit` | All roles (applicants see own only) |
| GET | `/api/audit-logs` | Admin only — paginated global log |

---

## Known limitations and future work

These are deliberate scope decisions for v1, documented for honesty:

- **Audit hash chain** — the current design prevents tampering through the application layer. A production deployment would add a cryptographic hash chain and periodic export to a notarised external store for full legal admissibility.
- **Token revocation** — JWTs expire naturally (15 min). Immediate revocation on all devices would require a Redis-backed blacklist.
- **Email notifications** — no email or SMS on state changes. A message queue (RabbitMQ / SQS) would be the foundation.
- **Two-factor authentication** — recommended for Reviewer, Approver, and Admin roles before production rollout.
- **Rate limiting** — login endpoint brute-force protection not implemented in v1.
- **Appeal process** — a rejected application requires a new submission. An appeal mechanism would need its own state machine extension.
- **SLA monitoring** — no automatic escalation for stalled applications.
