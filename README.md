# Mari-Fariyaad (મારી-ફરિયાદ)
## Complaint Management Portal for GVP Estate & University Campus

### Spring Boot 3.x / Java 17 / Apache Maven 3.9.16 Project

**Mari-Fariyaad** is a comprehensive, production-ready frontend for Gujarat Vidyapith Estate & Infrastructure Maintenance.

---

### Project Structure
```text
Mari-Fariyaad/
├── README.md
├── Mari-Fariyaad_Master_Prompt.txt
├── backend/                        <- Spring Boot app (all Java code, config, DB)
│   ├── pom.xml
│   ├── .env.example
│   ├── BACKEND_SETUP_README.md     <- full setup guide, start here
│   ├── database/
│   │   └── marifariyaad_db.sql     <- PostgreSQL schema
│   ├── uploads/                    <- runtime complaint photo/video storage
│   └── src/main/
│       ├── java/com/gvp/marifariyaad/
│       └── resources/
│           └── application.properties
└── frontend/                       <- templates + static assets (no Java)
    ├── templates/
    │   ├── index.html
    │   ├── about.html
    │   ├── complaint.html
    │   ├── track.html
    │   ├── login.html
    │   ├── admin-login.html
    │   ├── register.html
    │   ├── forgot-password.html
    │   ├── dashboard.html
    │   ├── admin-dashboard.html
    │   ├── departments.html
    │   ├── hostels.html
    │   ├── profile.html
    │   ├── faq.html
    │   └── contact.html
    └── static/
        ├── css/
        │   └── custom.css
        ├── js/
        │   ├── main.js
        │   └── virtual-keyboard.js
        └── lang/
            ├── en.json
            ├── gu.json
            └── hi.json
```

`backend/` and `frontend/` are separate folders so each can be worked on
independently, but this is still **one Spring Boot application** — the
backend reads `frontend/templates` and `frontend/static` straight off disk
(`file:../frontend/...`) at startup rather than bundling them on its
classpath. It is not a client/server split with two running processes or two
ports; there's a single app on **:8080** serving both the pages and the API.

---

### Key Features
- **3 Languages Support**: Gujarati (ગુજરાતી), English, and Hindi (JSON translation system) — selection made on any page persists across the whole site via `localStorage`.
- **Optional Virtual Keyboard**: off by default, toggled independently of language, follows the selected language when on.
- **Gujarat Vidyapith Heritage Theme**: Warm Saffron (#C17B3A), Deep Brown (#6B4226), Warm Cream (#F5EDE0).
- **20 Complaint Categories**: Kadiya Kam, Suthari Kam, Udhai Kam, Gatar, Sauchalay Safai, Pani, Plumbing, Electric Work, Street Light, Road Repair, Garden, Cleaning, Water Leakage, Water Cooler, WiFi, Classroom, Lab, Computer, Furniture, Other.
- **Mandatory Email**: Email required for status tracking.
- **Role-based Access**: real Spring Security session auth — Student, Faculty, Staff, Hostel Resident, Research Scholar, Visitor, Contract Worker map to `USER`; Estate/Admin staff to `ADMIN`.
- **PostgreSQL-backed**, Email OTP verified registration & password reset.

---

### How to Run

See **`backend/BACKEND_SETUP_README.md`** for full setup (PostgreSQL, SMTP, env vars). Quick version:

```bash
cd backend
cp .env.example .env   # fill in real DB + SMTP values
export $(grep -v '^#' .env | xargs)
mvn spring-boot:run
```

Then open `http://localhost:8080`. **Must be run from inside `backend/`** so
the `../frontend` relative path resolves.

