# URL Shortener API

![CI](https://github.com/Sandeepkumar1703/url-shortener/actions/workflows/ci.yml/badge.svg)
![Java](https://img.shields.io/badge/Java-17-orange)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.3.2-brightgreen)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-blue)
![Docker](https://img.shields.io/badge/Docker-Ready-2496ED)
![Maven](https://img.shields.io/badge/Maven-Build-red)
![License](https://img.shields.io/badge/License-MIT-green)

> A production-ready **URL Shortener REST API** built with **Java 17**, **Spring Boot 3**, **PostgreSQL**, **Flyway**, **Docker**, and **GitHub Actions CI**. The application enables users to create, manage, and analyze shortened URLs while following enterprise software development practices including layered architecture, automated testing, database versioning, containerization, and continuous integration.

---

# Features

- Create short URLs for any valid URL
- Automatically generate unique short codes
- Redirect short URLs to the original destination
- URL expiration support
- Track click counts
- Retrieve URL statistics
- Delete shortened URLs
- Request validation using Jakarta Bean Validation
- Centralized global exception handling
- Interactive Swagger/OpenAPI documentation
- Database schema management using Flyway
- Docker & Docker Compose support
- GitHub Actions CI pipeline
- Unit, Controller, Service, and Repository testing
- Testcontainers integration for database testing

---

# Tech Stack

| Category | Technology |
|----------|------------|
| Language | Java 17 |
| Framework | Spring Boot 3.3.2 |
| Web | Spring MVC |
| Persistence | Spring Data JPA, Hibernate |
| Database | PostgreSQL |
| Database Migration | Flyway |
| Validation | Jakarta Bean Validation |
| API Documentation | SpringDoc OpenAPI, Swagger UI |
| Testing | JUnit 5, Mockito, MockMvc, Testcontainers |
| Containerization | Docker, Docker Compose |
| Build Tool | Maven |
| CI/CD | GitHub Actions |

---

# Architecture

```text
                Client
                   │
                   ▼
          REST Controller
                   │
                   ▼
           Service Layer
                   │
                   ▼
         Repository Layer
                   │
                   ▼
             PostgreSQL
```

The project follows a clean layered architecture that separates presentation, business logic, and persistence concerns, making the application scalable, maintainable, and easy to test.

---

# Project Structure

```text
src
├── main
│   ├── java
│   │   └── com.sandeep.urlshortener
│   │       ├── config
│   │       ├── controller
│   │       ├── dto
│   │       ├── entity
│   │       ├── exception
│   │       ├── repository
│   │       ├── service
│   │       └── util
│   │
│   └── resources
│       ├── db
│       │   └── migration
│       └── application.properties
│
└── test
    ├── controller
    ├── repository
    └── service

.github
└── workflows
    └── ci.yml

Dockerfile
docker-compose.yml
pom.xml
README.md
```

---

# REST API Endpoints

| Method | Endpoint | Description |
|---------|----------|-------------|
| POST | `/api/v1/urls` | Create Short URL |
| GET | `/api/v1/urls/{shortCode}` | Redirect to Original URL |
| GET | `/api/v1/urls/{shortCode}/stats` | Retrieve URL Statistics |
| DELETE | `/api/v1/urls/{shortCode}` | Delete Short URL |

---

# Sample Request

### Create Short URL

```http
POST /api/v1/urls
Content-Type: application/json
```

```json
{
  "originalUrl": "https://www.google.com",
  "expiresAt": "2026-12-31T23:59:59"
}
```

---

# Sample Response

```json
{
  "shortCode": "Ab12Cd",
  "originalUrl": "https://www.google.com",
  "shortUrl": "http://localhost:8080/Ab12Cd",
  "createdAt": "2026-08-09T08:06:07.903Z",
  "expiresAt": "2026-12-31T23:59:59",
  "clickCount": 0
}
```

---

# URL Statistics

```json
{
  "shortCode": "Ab12Cd",
  "originalUrl": "https://www.google.com",
  "createdAt": "2026-08-09T08:06:07.903Z",
  "expiresAt": "2026-12-31T23:59:59",
  "clickCount": 42
}
```

---

# Error Response

```json
{
  "timestamp": "2026-08-09T08:05:24.922",
  "status": 404,
  "error": "Not Found",
  "message": "Short URL 'DOESNOTEXIST' not found"
}
```

---

# Getting Started

## Prerequisites

- Java 17 or later
- Maven 3.9+
- Docker Desktop
- Git

---

## Clone the Repository

```bash
git clone https://github.com/Sandeepkumar1703/url-shortener.git

cd url-shortener
```

---

# Running with Docker

Build and start the application along with PostgreSQL.

```bash
docker compose up --build
```

Run in detached mode:

```bash
docker compose up -d
```

Stop containers:

```bash
docker compose down
```

The application will be available at:

```
http://localhost:8080
```

---

# Running Without Docker

Start PostgreSQL manually, then run:

```bash
mvn spring-boot:run
```

---

# API Documentation

Swagger UI

```
http://localhost:8080/swagger-ui/index.html
```

OpenAPI Specification

```
http://localhost:8080/v3/api-docs
```

---

# Running Tests

Run all tests

```bash
mvn test
```

Run the complete verification pipeline

```bash
mvn verify
```

Current automated test suite includes:

- Controller Tests
- Service Tests
- Repository Tests
- MockMvc Tests
- Mockito Unit Tests
- Testcontainers PostgreSQL Integration Tests

All tests pass successfully.

---

# Database Migrations

Flyway automatically executes database migrations during application startup.

Migration files are located in:

```text
src/main/resources/db/migration
```

---

# Validation

The application validates incoming requests using Jakarta Bean Validation.

Implemented validations include:

- Valid URL format
- Required request fields
- Future expiration date
- Non-empty request body

---

# Exception Handling

A centralized exception handler provides standardized API responses.

| HTTP Status | Description |
|-------------|-------------|
| 400 | Validation Error |
| 404 | Resource Not Found |
| 409 | Duplicate Short Code |
| 410 | URL Expired |
| 500 | Internal Server Error |

---

# Docker Support

The project includes:

- Dockerfile
- Docker Compose
- PostgreSQL Container
- Spring Boot Application Container

Build and run:

```bash
docker compose up --build
```

Stop:

```bash
docker compose down
```

---

# Continuous Integration

GitHub Actions automatically performs:

- Maven Build
- Dependency Resolution
- Unit Testing
- Integration Testing
- Maven Verify

Every push and pull request is automatically validated.

---

# Future Enhancements

- JWT Authentication
- User Accounts
- Custom Short URLs
- QR Code Generation
- Redis Caching
- Rate Limiting
- URL Analytics Dashboard
- Scheduled Cleanup of Expired URLs
- Docker Image Publishing
- Kubernetes Deployment
- Prometheus & Grafana Monitoring
- AWS Deployment

---

# Author

**Sandeep Kumar Prasad**

Backend Developer

**Tech Stack**

- Java
- Spring Boot
- REST APIs
- PostgreSQL
- Docker
- Maven
- GitHub Actions

---

# License

This project is licensed under the **MIT License**.