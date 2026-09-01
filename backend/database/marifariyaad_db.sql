-- ===================================================================
-- Mari-Fariyaad - Gujarat Vidyapith Complaint Management Portal
-- PostgreSQL Setup Script
-- ===================================================================
-- Provided for reference / manual setup only. Spring Boot
-- (spring.jpa.hibernate.ddl-auto=update) will automatically create/
-- update these tables on application startup once the database
-- itself exists - running this file is optional but recommended for
-- a clean, explicit initial schema.
--
-- Usage (psql):
--   psql -U postgres -f marifariyaad_db.sql
-- ===================================================================

-- Run once, connected to the default "postgres" database.
-- (Unlike MySQL, PostgreSQL cannot CREATE DATABASE and then USE it
-- in the same script/session - reconnect with \c after this line,
-- or simply run the rest of this file against the new database.)
CREATE DATABASE marifariyaad_db
  WITH ENCODING = 'UTF8'
  LC_COLLATE = 'en_US.UTF-8'
  LC_CTYPE = 'en_US.UTF-8'
  TEMPLATE = template0;

-- \c marifariyaad_db

-- ---------------------------------------------------------------
-- users
-- ---------------------------------------------------------------
CREATE TABLE IF NOT EXISTS users (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    full_name       VARCHAR(150)  NOT NULL,
    email           VARCHAR(150)  NOT NULL UNIQUE,
    mobile          VARCHAR(20)   NOT NULL,
    gender          VARCHAR(20),
    password_hash   VARCHAR(255)  NOT NULL,
    role            VARCHAR(20)   NOT NULL DEFAULT 'USER',
    department      VARCHAR(100),
    hostel          VARCHAR(100),
    address         TEXT,
    enabled         BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP(0)  NOT NULL,
    CONSTRAINT chk_users_role CHECK (role IN ('USER', 'ADMIN'))
);

-- ---------------------------------------------------------------
-- pending_registrations (email OTP verification, pre-account-creation)
-- ---------------------------------------------------------------
CREATE TABLE IF NOT EXISTS pending_registrations (
    id               BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    full_name        VARCHAR(150) NOT NULL,
    email            VARCHAR(150) NOT NULL,
    mobile           VARCHAR(20)  NOT NULL,
    gender           VARCHAR(20),
    password_hash    VARCHAR(255) NOT NULL,
    role             VARCHAR(20)  NOT NULL DEFAULT 'USER',
    department       VARCHAR(100),
    hostel           VARCHAR(100),
    address          TEXT,
    otp_hash         VARCHAR(255) NOT NULL,
    otp_expires_at   TIMESTAMP(0) NOT NULL,
    created_at       TIMESTAMP(0) NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_pending_registrations_email ON pending_registrations (email);

-- ---------------------------------------------------------------
-- password_reset_otps
-- ---------------------------------------------------------------
CREATE TABLE IF NOT EXISTS password_reset_otps (
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    email        VARCHAR(150) NOT NULL,
    otp_hash     VARCHAR(255) NOT NULL,
    expires_at   TIMESTAMP(0) NOT NULL,
    used         BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at   TIMESTAMP(0) NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_password_reset_otps_email ON password_reset_otps (email);

-- ---------------------------------------------------------------
-- complaints
-- ---------------------------------------------------------------
CREATE TABLE IF NOT EXISTS complaints (
    id                    BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    ticket_id             VARCHAR(30)  NOT NULL UNIQUE,
    user_id               BIGINT       NOT NULL,
    title                 VARCHAR(255) NOT NULL,
    category              VARCHAR(50)  NOT NULL,
    category_name         VARCHAR(150),
    location_type         VARCHAR(50),
    building              VARCHAR(150),
    floor                 VARCHAR(50),
    room                  VARCHAR(50),
    department            VARCHAR(100),
    hostel                VARCHAR(100),
    description           TEXT         NOT NULL,
    priority              VARCHAR(20)  NOT NULL,
    status                VARCHAR(20)  NOT NULL DEFAULT 'Pending',
    assigned_to           VARCHAR(150),
    photo_path            VARCHAR(255),
    photo_original_name   VARCHAR(255),
    video_path            VARCHAR(255),
    video_original_name   VARCHAR(255),
    submitted_at          TIMESTAMP(0) NOT NULL,
    CONSTRAINT fk_complaints_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT chk_complaints_status CHECK (status IN ('Pending','Assigned','In_Progress','Resolved','Rejected'))
);

CREATE INDEX IF NOT EXISTS idx_complaints_user ON complaints (user_id);
CREATE INDEX IF NOT EXISTS idx_complaints_status ON complaints (status);

-- ---------------------------------------------------------------
-- complaint_timeline
-- ---------------------------------------------------------------
CREATE TABLE IF NOT EXISTS complaint_timeline (
    id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    complaint_id   BIGINT      NOT NULL,
    status         VARCHAR(20) NOT NULL,
    note           TEXT,
    created_at     TIMESTAMP(0) NOT NULL,
    CONSTRAINT fk_timeline_complaint FOREIGN KEY (complaint_id) REFERENCES complaints(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_timeline_complaint ON complaint_timeline (complaint_id);

-- ---------------------------------------------------------------
-- Create the first ADMIN account manually.
-- Generate a BCrypt hash for your chosen password (e.g. via
-- bcrypt-generator.com or Spring's BCryptPasswordEncoder) and
-- put it in place of <BCRYPT_HASH_HERE> below, then uncomment and run.
-- ---------------------------------------------------------------
-- INSERT INTO users (full_name, email, mobile, gender, password_hash, role, enabled, created_at)
-- VALUES ('Admin', 'admin@gujaratvidyapith.org', '9999999999', 'Other',
--         '<BCRYPT_HASH_HERE>', 'ADMIN', TRUE, NOW());
