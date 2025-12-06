# Enrollment gRPC System – Architecture and Setup

## Overview
A microservices-based enrollment system built with Spring Boot, gRPC, PostgreSQL, and a web UI (Thymeleaf). Services communicate via gRPC; the web-node acts as the UI gateway. Authentication is JWT-based with an HMAC secret shared across services.

## Services and Ports
- **web-node** (HTTP UI)
  - HTTP: 8080
  - Talks to gRPC backends via service names.
- **auth-enrollment-service** (Auth, Users, Enrollment orchestration)
  - HTTP: 8082, gRPC: 9091
  - Issues JWT tokens (HS256) and validates credentials.
  - Owns DB schema and runs Flyway migrations.
- **course-service** (Courses catalog)
  - HTTP: 8083, gRPC: 9092
  - CRUD-like operations for courses (admin/faculty flows).
- **grade-service** (Grades)
  - HTTP: 8084, gRPC: 9093
  - Faculty uploads grades; students view.
- **postgres** (Database)
  - TCP: 5432
  - Single DB "enrollment" shared by services.

## Data Model (simplified)
- users(id, password, role)
- courses(id, name, units, laboratory, faculty_id, capacity)
- enrollments(student_id, course_id, grade)

Capacity governs whether a course is "open" (current < capacity) or "closed" (>= capacity). Enrollment is blocked when full.

## AuthN/AuthZ
- JWT HS256 signed with `JWT_SECRET` (shared across services).
- Roles: STUDENT, FACULTY, ADMIN
- First login bootstraps the user:
  - `admin*` → ADMIN
  - `f_*` or `faculty*` → FACULTY
  - otherwise → STUDENT
- Passwords are stored bcrypt-hashed. First-time login sets password; later logins validate it.

## Inter-service gRPC
- web-node uses gRPC clients:
  - auth-enrollment-service: 9091
  - course-service: 9092
  - grade-service: 9093
- All gRPC calls are PLAINTEXT inside the Docker network (consider TLS or VPN for untrusted networks).

## Setup Requirements
Before running the project, ensure the following are installed:
1. Java
  - Windows: Download from Oracle, set JAVA_HOME, and add to PATH
  - MacOS: `brew install open` and follow instructions to set JAVA_HOME
2. grpcurl
  - Windows: Download the binary from grpcurl releases and add it to PATH
  - MacOS: `brew install grpcurl`
3. Docker
  - Windows & MacOs: Install Docker Desktop and ensure it's running
4. Gradle
  - Windows: Download from Gradle releases and add to PATH
  - MacOs: `brew install gradle`

## Docker Compose (local, single host)
- Build all services:
  - `gradle :auth-enrollment-service:bootJar :course-service:bootJar :grade-service:bootJar :web-node:bootJar`
- Start the stack:
  - `docker compose up --build -d postgres auth-enrollment-service course-service grade-service web-node`
- Web UI: http://localhost:8080

Environment (excerpt from compose):
- `JWT_SECRET=dev-shared-secret-...` (same across all services)
- `SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/enrollment`
- `SPRING_DATASOURCE_USERNAME=enrollment`
- `SPRING_DATASOURCE_PASSWORD=enrollment`

## Database & Migrations
- Flyway runs in auth-enrollment-service.
- Migrations:
  - V1__init.sql: Sets up the core schema (users, courses, enrollments) with relationships, and seeds initial data including default users, sample courses, and a starter enrollment
  - V2__add_capacity.sql: Adds a capacity column to the course table and seeds initial capacity values for existing courses
  - V3__add_user_password.sql: Introduces a password column to the users table to support credential-based login, with a safe default for existing records
  - V4__add_admin.sql: Seeds a default admin account with a pre-hashed password, ensuring it's created only if it doesn't already exist
  - V5__enrollment_completion.sql: Adds term, academic year, and completion timestamp fields to enrollments, and creates an index to optimize course/term/year queries
  - V6__course_completion_flag.sql: Adds term, academic year, and completion timestamp fields to courses to track course-level completion even without enrollments
- Only one migration per version. If duplicates exist, delete/rename to maintain unique versioning.

## Development (without Docker)
- Start Postgres locally (5432) and create DB/user:
  - DB: enrollment, user/pass: enrollment/enrollment
- Set env vars per service and run jars:
  - `SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/enrollment`
  - `SPRING_DATASOURCE_USERNAME=enrollment`
  - `SPRING_DATASOURCE_PASSWORD=enrollment`
  - `JWT_SECRET=dev-shared-secret-...`
- web-node also needs:
  - `AUTH_ENROLL_ADDRESS=static://localhost:9091`
  - `COURSE_ADDRESS=static://localhost:9092`
  - `GRADE_ADDRESS=static://localhost:9093`

## Multi-VM/Bare-metal Deployment
- Suggested layout: DB, AUTH, COURSE, GRADE, WEB on separate VMs (or combined).
- Open firewall ports: 5432, 9091/9092/9093, 8080.
- Use environment overrides on web-node:
  - `AUTH_ENROLL_ADDRESS=static://<AUTH_IP>:9091`
  - `COURSE_ADDRESS=static://<COURSE_IP>:9092`
  - `GRADE_ADDRESS=static://<GRADE_IP>:9093`
- Ensure `JWT_SECRET` is identical across all services.

## Typical Flows
- **Student**
  - Login → Dashboard shows enrollments → View available courses → Enroll → View grades.
- **Faculty**
  - Login → My Courses → View students → Upload/edit grades.
- **Admin**
  - Login → Manage courses (e.g., capacity), register users (via RPC), future admin UI.

## Troubleshooting
- "application error processing RPC" during login:
  - Check web-node → auth-enrollment-service connectivity and DB connectivity.
  - Confirm Flyway applied and tables exist: `docker compose exec postgres psql -U enrollment -d enrollment -c "\dt"`.
- "users table does not exist":
  - Ensure Flyway is enabled and only unique migration versions exist.
- JWT signature errors:
  - Verify `JWT_SECRET` hashes match across containers: `printf %s "$JWT_SECRET" | md5sum`.
- Compose service names vs localhost:
  - Inside Compose, use `postgres`, `auth-enrollment-service`, `course-service`, `grade-service` instead of localhost.

## UI
- Tailwind-based minimal UI.
- Student: Courses (open/closed by capacity), Grades.
- Faculty: My Courses, Course Students (inline grade edit).
- Admin: planned capacity management (RPC/UI can be added).

## Commands Quick Reference
- Build all: `gradle build` (or specific `:module:bootJar`)
- Start (Compose): `docker compose up --build -d`
- Logs: `docker compose logs --tail=200 <service>`
- Reset DB (destructive): `docker compose down -v && docker compose up -d postgres auth-enrollment-service`

---
For enhancements (admin capacity UI, TLS for gRPC, health endpoints), open an issue or request and we’ll extend the stack.
