# Mari-Fariyaad — Backend Setup Guide

This project has been converted from a frontend/localStorage demo into a real
**Spring Boot + Spring Security + MySQL + Email OTP** backed application. The
existing UI, layout, and Bootstrap styling are unchanged — only the plumbing
underneath is now real.

## 1. Requirements

- Java 17+
- Maven 3.8+
- MySQL 8.x (running locally or reachable over the network)
- A Gmail (or other SMTP) account for sending OTP emails

## 2. Configure Environment Variables

Copy `.env.example` to `.env` (or export the variables directly in your shell /
IDE run configuration) and fill in real values:

```
DB_URL=jdbc:mysql://localhost:3306/marifariyaad_db?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
DB_USERNAME=root
DB_PASSWORD=your_mysql_password

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
variables on the run configuration. If you're running from a terminal:

```bash
export $(grep -v '^#' .env | xargs)
mvn spring-boot:run
```

## 3. Create the MySQL Database

Two options:

**Option A — let Hibernate do it (simplest):**
Just create an empty database and Spring Boot will create/update all tables
automatically on startup (`spring.jpa.hibernate.ddl-auto=update`):

```sql
CREATE DATABASE marifariyaad_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

**Option B — run the provided SQL script** for a fully predictable initial
schema:

```bash
mysql -u root -p < database/marifariyaad_db.sql
```

## 4. Run the Application

```bash
mvn clean install
mvn spring-boot:run
```

The app starts on **http://localhost:8080**.

## 5. Create the First Admin Account

There is no public "become an admin" flow (by design — every self-registered
account is a normal `USER`). To create your first `ADMIN` account, either:

- Register normally through `/register.html`, then update that user's `role`
  column to `ADMIN` directly in MySQL:
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
  table (with a BCrypt-hashed, 10‑minute-expiry OTP) until the OTP is verified;
  only then is the real `users` row created.
- **Forgot password**: a `password_reset_otps` table backs a
  request → verify → reset flow, also OTP + BCrypt + 10 minute expiry.
- **Complaints**: persisted to MySQL (`complaints` + `complaint_timeline`
  tables) with a generated `GVP-YYYY-XXXXXX` ticket ID, photo (≤10MB, images
  only) and video (≤50MB, video only) attachments stored under
  `uploads/complaints/{photos,videos}` with UUID file names (originals are
  never trusted, preventing path traversal).
- **Authorization**: normal users can only see their own complaints; `ADMIN`
  accounts can see and update all complaints (status changes always append a
  new timeline entry).
- **Hindi language file** (`hi.json`) had Gujarati text mistakenly present in
  a few keys — fixed to proper Devanagari Hindi.

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
| GET    | `/api/complaints/search?query=`     | Search by ticket ID or email    |
| GET    | `/api/complaints/{ticketId}`        | Get one complaint (owner/admin) |
| GET    | `/api/complaints/admin/all`         | All complaints (admin only)     |
| PUT    | `/api/complaints/{ticketId}/status` | Update status/assignee (admin)  |

All protected endpoints return **HTTP 401 JSON** if not logged in, and
protected pages redirect to `/login.html`.
