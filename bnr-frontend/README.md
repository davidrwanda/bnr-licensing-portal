# BNR Licensing Portal — Frontend

**Author: David NTAMAKEMWA**

React 18 + TypeScript + Vite frontend for the BNR Bank Licensing & Compliance Portal.

The root `README.md` covers the full project including Docker setup, seed accounts and API docs. This file covers frontend-specific details.

---

## Dev server

```bash
cp .env.example .env   # set VITE_API_URL=http://localhost:8080
npm install
npm run dev
```

Runs at `http://localhost:5173`. Hot module replacement is enabled so changes show up immediately without a full reload.

The backend needs to be running separately — either via Docker or `./mvnw spring-boot:run` from `bnr-backend/`.

---

## Build

```bash
npm run build
```

Output goes to `dist/`. In production the build is served by nginx inside the Docker container — see the `Dockerfile` and `nginx.conf` in this directory.

---

## Environment variables

| Variable | Description | Default |
|----------|-------------|---------|
| `VITE_API_URL` | Base URL of the Spring Boot API | `http://localhost:8080` |

This is baked into the bundle at build time by Vite. If you change it, you need to rebuild.

---

## What's in here

```
src/
├── api/           Axios client + typed functions for every endpoint
├── context/       Auth state (stored in localStorage, cleared on logout)
├── pages/
│   ├── LoginPage.tsx
│   ├── DashboardPage.tsx
│   ├── ApplicationDetailPage.tsx
│   ├── applicant/NewApplicationPage.tsx
│   └── admin/
│       ├── AuditLogPage.tsx
│       └── UsersPage.tsx
└── components/
    ├── layout/Shell.tsx     Navigation bar + page wrapper
    └── ui/                  StatusBadge, Spinner, ErrorAlert, EmptyState
```

---

## Role-based UI

The nav bar and available actions change depending on who is logged in. The rules mirror what the backend enforces — a reviewer only sees applications assigned to them, an approver only sees applications that are `REVIEW_COMPLETE`, and so on. If the backend returns 403, the frontend surfaces the actual error message rather than a generic fallback.

Draft applications are only visible to the applicant who created them. No other role can see or open a draft.
