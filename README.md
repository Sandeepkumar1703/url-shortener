# URL Shortener API

A production-ready **URL Shortener REST API** built with **Java 17**, **Spring Boot 3**, **PostgreSQL**, and **Flyway**. The application allows users to create short URLs, redirect to the original destination, retrieve analytics, and delete shortened URLs. It follows a clean layered architecture with validation, global exception handling, automated testing, and interactive API documentation.

---

## Features

* Create short URLs for any valid URL
* Automatic unique short code generation
* Redirect short URLs to the original destination
* URL expiration support
* Click tracking and statistics
* Delete shortened URLs
* Bean Validation for request validation
* Global exception handling with consistent error responses
* Interactive Swagger/OpenAPI documentation
* Database versioning with Flyway
* Comprehensive unit, controller, and repository tests
* Testcontainers integration for repository testing

---

## Tech Stack

### Backend

* Java 17
* Spring Boot 3.3.2
* Spring Web
* Spring Data JPA
* Hibernate
* Bean Validation

### Database

* PostgreSQL
* Flyway

### Testing

* JUnit 5
* Mockito
* MockMvc
* Testcontainers

### Documentation

* SpringDoc OpenAPI
* Swagger UI

### Build Tool

* Maven

---

## Architecture

```
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

The project follows a layered architecture that separates responsibilities between the web, business, and persistence layers, making the codebase easy to maintain and test.

---

## Project Structure

```
src
├── main
│   ├── java
│   │   └── com.sandeep.urlshortener
│   │       ├── controller
│   │       ├── dto
│   │       ├── entity
│   │       ├── exception
│   │       ├── repository
│   │       ├── service
│   │       └── config
│   └── resources
│       ├── db
│       │   └── migration
│       └── application.properties
│
└── test
    ├── controller
    ├── repository
    └── service
```

---

## API Endpoints

| Method | Endpoint                         | Description              |
| ------ | -------------------------------- | ------------------------ |
| POST   | `/api/v1/urls`                   | Create a short URL       |
| GET    | `/api/v1/urls/{shortCode}`       | Redirect to original URL |
| GET    | `/api/v1/urls/{shortCode}/stats` | Retrieve URL statistics  |
| DELETE | `/api/v1/urls/{shortCode}`       | Delete a short URL       |

---

## Example Request

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

### Response

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

## URL Statistics Response

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

## Error Response

```json
{
  "timestamp": "2026-08-09T08:05:24.922",
  "status": 404,
  "error": "Not Found",
  "message": "Short URL 'DOESNOTEXIST' not found"
}
```

---

## Getting Started

### Prerequisites

* Java 17+
* Maven 3.9+
* Docker Desktop
* Docker Compose

---

### Clone the Repository

```bash
git clone https://github.com/<your-username>/url-shortener.git
cd url-shortener
```

---

### Start PostgreSQL

```bash
docker compose up -d
```

---

### Run the Application

```bash
mvn spring-boot:run
```

The application starts on:

```
http://localhost:8080
```

---

## API Documentation

Swagger UI

```
http://localhost:8080/swagger-ui/index.html
```

OpenAPI Specification

```
http://localhost:8080/v3/api-docs
```

---

## Running Tests

Execute all tests:

```bash
mvn test
```

Run the complete verification pipeline:

```bash
mvn verify
```

Current Test Coverage

* Controller Tests
* Service Tests
* Repository Tests
* MockMvc API Tests
* Mockito Unit Tests
* Testcontainers PostgreSQL Integration Tests

---

## Database Migrations

Flyway automatically executes migrations during application startup.

Migration scripts are located in:

```
src/main/resources/db/migration
```

---

## Validation

The API validates incoming requests using Jakarta Bean Validation.

Examples include:

* Valid URL format
* Required fields
* Future expiration date
* Non-empty request values

---

## Exception Handling

A centralized global exception handler provides consistent error responses for:

* Resource Not Found (404)
* Validation Errors (400)
* Expired URLs (410)
* Duplicate Short Codes (409)
* Internal Server Errors (500)

---

## Future Enhancements

* User authentication and authorization
* Custom short codes
* QR code generation
* Redis caching
* Rate limiting
* Analytics dashboard
* URL access history
* Scheduled cleanup of expired URLs
* Docker image publishing
* CI/CD with GitHub Actions
* Cloud deployment (AWS)

---

## Author

**Sandeep Kumar Prasad**

Backend Developer

Specializing in Java, Spring Boot, REST APIs, PostgreSQL, and scalable backend systems.

---

## License

This project is licensed under the MIT License.
