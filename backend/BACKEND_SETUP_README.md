# Mari-Fariyaad — Backend Setup Guide

This project is a real **Spring Boot + Spring Security + PostgreSQL + Email
OTP** backed application, split into two sibling folders:

```
Mari-Fariyaad/
├── backend/    <- this folder: pom.xml, all Java code, application.properties
└── frontend/   <- templates/ (HTML) + static/ (css, js, lang json)
```

The backend is still a single Spring Boot app (Thymeleaf server-rendered
pages + REST APIs) — it now just loads its templates and static assets from
the sibling `frontend/` folder on disk instead of bundling them on its own
classpath. **This means the app must always be started with `backend/` as the
working directory**, so the relative `../frontend` path resolves correctly.

## 1. Requirements

- Java 17+
- Maven 3.8+
- PostgreSQL 13+ (running locally or reachable over the network)
- A Gmail (or other SMTP) account for sending OTP emails

## 2. Configure Environment Variables

Copy `.env.example` (in this `backend/` folder) to `.env` (or export the
variables directly in your shell / IDE run configuration) and fill in real
values:

```
DB_URL=jdbc:postgresql://localhost:5432/marifariyaad_db
DB_USERNAME=postgres
DB_PASSWORD=your_postgres_password

MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=your-gmail-address@gmail.com
MAIL_PASSWORD=your-16-char-app-password

UPLOAD_DIR=uploads
```

**Gmail note:** you cannot use your normal Gmail password for SMTP. Create an
["App Password"](https://myaccount.google.com/apppasswords) (requires 2-Step
Verification to be enabled on the Google account) and use that 16-character
value as `MAIL_PASSWORD`.

If you're using an IDE (IntelliJ / VS Code), set these as environment
variables on the run configuration **and set the working directory to
`backend/`**. If you're running from a terminal:

```bash
cd backend
export $(grep -v '^#' .env | xargs)
mvn spring-boot:run
```

## 3. Create the PostgreSQL Database

Two options:

**Option A — let Hibernate do it (simplest):** just create an empty database
and Spring Boot will create/update all tables automatically on startup
(`spring.jpa.hibernate.ddl-auto=update`):

```sql
CREATE DATABASE marifariyaad_db WITH ENCODING = 'UTF8' TEMPLATE = template0;
```

**Option B — run the provided SQL script** for a fully predictable initial
schema:

```bash
psql -U postgres -f database/marifariyaad_db.sql
```

(Run from inside `backend/`, or adjust the path.)

## 4. Run the Application

```bash
cd backend
mvn clean install
mvn spring-boot:run
```

The app starts on **http://localhost:8080**. If pages 404 or come back with
no CSS/JS, double check you ran the command from inside `backend/` — that's
what lets `../frontend` resolve.

## 5. Create the First Admin Account

There is no public "become an admin" flow (by design — every self-registered
account is a normal `USER`). To create your first `ADMIN` account, either:

- Register normally through `/register.html`, then update that user's `role`
  column to `ADMIN` directly in PostgreSQL:
  ```sql
  UPDATE users SET role = 'ADMIN' WHERE email = 'admin@gujaratvidyapith.org';
  ```
- Or generate a BCrypt hash for a chosen password and insert a row directly
  (see the commented-out `INSERT` at the bottom of
  `database/marifariyaad_db.sql`).

Admins log in through the same `/login.html` (or `/admin-login.html`) form —
the server checks the account's `role` and redirects to `/admin-dashboard.html`
automatically.

## 6. What Changed Under the Hood

- **Real authentication**: Spring Security + BCrypt + server-side HTTP
  sessions (no localStorage, no JWT). Login/registration/logout/`/api/auth/me`
  all talk to real REST endpoints.
- **Email OTP registration**: new accounts are held in a `pending_registrations`
  table (with a BCrypt-hashed, 10-minute-expiry OTP) until the OTP is verified;
  only then is the real `users` row created.
- **Forgot password**: a `password_reset_otps` table backs a
  request → verify → reset flow, also OTP + BCrypt + 10 minute expiry.
- **Complaints**: persisted to PostgreSQL (`complaints` + `complaint_timeline`
  tables) with a generated `GVP-YYYY-XXXXXX` ticket ID, photo (≤10MB, images
  only) and video (≤50MB, video only) attachments stored under
  `backend/uploads/complaints/{photos,videos}` with UUID file names (originals
  are never trusted, preventing path traversal).
- **Authorization**: normal users can only see their own complaints; `ADMIN`
  accounts can see and update all complaints (status changes always append a
  new timeline entry).
- **Database**: migrated from MySQL to PostgreSQL — driver, dialect, JDBC URL
  and the setup script were all updated; no entity/JPA code changes were
  needed since `GenerationType.IDENTITY` and `columnDefinition = "TEXT"` are
  both natively Postgres-compatible.
- **Project layout**: split into `frontend/` (templates + static assets) and
  `backend/` (all Java code) as two sibling folders, with the backend reading
  `frontend/` from disk (`file:../frontend/...`) instead of packaging it on
  the classpath. The app is still one Spring Boot process — this is a folder
  reorganization, not a service split.

## 7. Key REST Endpoints

| Method | Path                                | Notes                          |
|--------|--------------------------------------|---------------------------------|
| POST   | `/api/auth/register`                | Step 1 of signup (sends OTP)    |
| POST   | `/api/auth/verify-registration`     | Step 2 (creates account, logs in) |
| POST   | `/api/auth/login`                   | Session-based login             |
| POST   | `/api/auth/forgot-password`         | Sends reset OTP                 |
| POST   | `/api/auth/verify-reset-otp`        | Verifies reset OTP              |
| POST   | `/api/auth/reset-password`          | Sets new password               |
| POST   | `/api/auth/logout`                  | Invalidates session             |
| GET    | `/api/auth/me`                      | Current logged-in user          |
| PUT    | `/api/users/me`                     | Update profile (name/mobile)    |
| POST   | `/api/users/me/change-password`     | Change password (logged in)     |
| POST   | `/api/complaints` (multipart)       | Submit a complaint              |
| GET    | `/api/complaints`                   | My complaints                   |
| GET    | `/api/complaints/stats`             | My dashboard stats               |
| GET    | `/api/complaints/search?query=`     | Search own complaints by ticket ID or email (logged in) |
| GET    | `/api/complaints/{ticketId}`        | Get one complaint (owner/admin) |
| GET    | `/api/complaints/admin/all`         | All complaints (admin only)     |
| PUT    | `/api/complaints/{ticketId}/status` | Update status/assignee (admin)  |

All protected endpoints return **HTTP 401 JSON** if not logged in, and
protected pages redirect to `/login.html`.

**Ticket ID search note:** `/track.html` and `/api/complaints/search` require
login by design (a normal user can only search among their own complaints;
an `ADMIN` account searches across all complaints). This was verified
end-to-end: `ComplaintService.search()` matches the ticket ID or email
case-insensitively, `main.js`'s `handleTrackSearch()` calls it correctly, and
the home page's quick-track box passes the ticket ID through
`/track.html?id=...`, which pre-fills and auto-runs the search. If you want
ticket tracking to work **without** login (e.g. for a public tracker page),
that's a deliberate security-config change — flag it and it can be added.
