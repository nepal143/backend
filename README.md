# PLACEMENTgO

PlacementGo is a Java-based web application that helps job seekers apply smarter by customizing resumes using job descriptions, enabling LinkedIn-assisted referral discovery, and tracking applications through a centralized dashboard powered by AI and scalable Spring Boot architecture.

# PlacementGo Backend

This repository contains the **Spring Boot backend** for the PlacementGo project. It exposes REST APIs that will be consumed by a separate frontend application.

---

## Tech Stack

- Java 17
- Spring Boot 3.x
- Maven (with Maven Wrapper)
- Spring Web
- Spring Data JPA
- Spring Security (currently relaxed for local dev)
- PostgreSQL 16.x
- Flyway (DB migrations)
- Swagger / OpenAPI (springdoc)

---

## Prerequisites

Make sure the following are installed on your system:

- **Java 17** (check with `java -version`)
- **PostgreSQL 16.x**
- Git (optional, for cloning)

> ⚠️ Maven installation is **NOT required**. The project uses **Maven Wrapper (`mvnw`)**.

---

## Project Structure (Important)

```
placementGo-backend
├── src/main/java/com/placementgo
│   ├── auth/
│   ├── user/
│   ├── resume/
│   ├── referral/
│   ├── application/
│   ├── interview/
│   ├── common/
│   ├── config/
│   └── PlacementGoBackendApplication.java
│
├── src/main/resources
│   ├── application.yml
│   └── db/migration
│       └── V1__init.sql
│
├── pom.xml
├── mvnw
└── mvnw.cmd
```

Each module (auth, user, resume, etc.) follows this internal structure:

```
<module-name>
├── controller
├── service
├── repository
└── entity
```

---

## Database Setup (PostgreSQL)

### 1. Create Database

Open pgAdmin or psql and create a database:

```sql
CREATE DATABASE placementgo;
```

### 2. Configure Credentials

Update `src/main/resources/application.yml`:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/placementgo
    username: postgres
    password: 1234

  jpa:
    hibernate:
      ddl-auto: validate

flyway:
  enabled: true
```

---

## Running the Application (Local)

### Windows / PowerShell

From the project root:

```powershell
.\mvnw spring-boot:run
```

### Expected Logs

You should see:

```
Tomcat started on port 8080
Started PlacementGoBackendApplication
```

---

## Swagger (API Documentation)

After the app starts, open:

```
http://localhost:8080/swagger-ui/index.html
```

Swagger shows:

- All available APIs
- Request / response schemas
- Ability to test APIs directly

---

## Important Notes

- This project is **backend only**. There is no frontend/UI in this repo.
- `/` (root URL) may return 404 if no controller is mapped — this is expected.
- Swagger is enabled for **development only**.
- Flyway automatically runs DB migrations on startup.

---

## Common Issues

### 1. `mvn` not recognized

Use Maven Wrapper instead:

```bash
.\mvnw spring-boot:run
```

### 2. PostgreSQL connection refused

- Ensure PostgreSQL service is running
- Check port (default: 5432)
- Verify username/password in `application.yml`

### 3. Swagger returns 404

Ensure this dependency exists in `pom.xml`:

```xml
<dependency>
  <groupId>org.springdoc</groupId>
  <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
  <version>2.5.0</version>
</dependency>
```

---

## How Team Members Should Start

1. Clone the repository
2. Install Java 17 and PostgreSQL
3. Create database `placementgo`
4. Update `application.yml`
5. Run `./mvnw spring-boot:run`
6. Open Swagger UI and start testing APIs

---

## Status

- Backend setup: ✅
- DB + Flyway: ✅
- Swagger: ✅
- Auth & modules: 🚧 In progress

---

If the app starts and Swagger loads, the setup is correct.
