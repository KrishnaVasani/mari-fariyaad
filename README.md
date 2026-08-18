# Mari-Fariyaad (મારી-ફરિયાદ)
## Complaint Management Portal for GVP Estate & University Campus

### Spring Boot 3.x / Java 17 / Apache Maven 3.9.16 Project

**Mari-Fariyaad** is a comprehensive, production-ready frontend for Gujarat Vidyapith Estate & Infrastructure Maintenance.

---

### Project Structure
```text
Mari-Fariyaad/
├── pom.xml
├── Mari-Fariyaad_Master_Prompt.txt
├── README.md
└── src/
    └── main/
        ├── java/
        │   └── com/gvp/marifariyaad/
        └── resources/
            ├── application.properties
            ├── templates/
            │   ├── index.html
            │   ├── about.html
            │   ├── complaint.html
            │   ├── track.html
            │   ├── login.html
            │   ├── admin-login.html
            │   ├── register.html
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
                │   └── main.js
                └── lang/
                    ├── en.json
                    ├── gu.json
                    └── hi.json
```

---

### Key Features
- **3 Languages Support**: Gujarati (ગુજરાતી), English, and Hindi (JSON translation system).
- **Gujarat Vidyapith Heritage Theme**: Warm Saffron (#C17B3A), Deep Brown (#6B4226), Warm Cream (#F5EDE0).
- **20 Complaint Categories**: Kadiya Kam, Suthari Kam, Udhai Kam, Gatar, Sauchalay Safai, Pani, Plumbing, Electric Work, Street Light, Road Repair, Garden, Cleaning, Water Leakage, Water Cooler, WiFi, Classroom, Lab, Computer, Furniture, Other.
- **Mandatory Email**: Email required for status tracking.
- **Role-based Access UI**: Student, Faculty, Staff, Hostel Resident, Research Scholar, Visitor, Contract Worker, Admin.
- **Spring Security & JWT Ready UI**.

---

### How to Run with Spring Boot
1. Ensure Java 17+ and Apache Maven 3.9.16+ are installed.
2. Run `mvn spring-boot:run` in the terminal.
3. Open `http://localhost:8080` in your web browser.
"# mari-fariyaad" 
