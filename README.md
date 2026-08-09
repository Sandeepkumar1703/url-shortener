# URL Shortener API

> A production-oriented **URL Shortener REST API** built with **Java 17, Spring Boot 3, PostgreSQL, Redis, Flyway, Docker, and GitHub Actions**.
>
> The application provides URL creation, redirection, expiration handling, click tracking, statistics, deletion, Redis caching, IP-based rate limiting, centralized exception handling, automated testing, containerization, and CI.
>
> The architecture is designed around a **read-heavy workload**, where URL redirects can significantly outnumber URL creation and management operations.

---

# Features

* Create short URLs for valid URLs
* Generate unique short codes
* Redirect short URLs to original destinations
* URL expiration support
* Track click counts
* Retrieve URL statistics
* Delete shortened URLs
* Request validation using Jakarta Bean Validation
* Centralized global exception handling
* Duplicate short-code protection
* Redis caching for frequently accessed URLs
* Redis-backed rate limiting
* IP-based request throttling
* Rate-limit response headers
* Retry-After support for throttled requests
* PostgreSQL persistence
* Flyway database migrations
* Swagger/OpenAPI documentation
* Docker and Docker Compose support
* GitHub Actions CI pipeline
* Unit and integration testing
* MockMvc controller testing
* Mockito service testing
* PostgreSQL integration testing with Testcontainers
* Layered architecture
* Read-heavy scalability architecture

---

# Tech Stack

| Category            | Technology                     |
| ------------------- | ------------------------------ |
| Language            | Java 17                        |
| Framework           | Spring Boot 3.3.2              |
| Web                 | Spring MVC                     |
| Persistence         | Spring Data JPA, Hibernate     |
| Primary Database    | PostgreSQL                     |
| Cache               | Redis                          |
| Database Migration  | Flyway                         |
| Validation          | Jakarta Bean Validation        |
| API Documentation   | SpringDoc OpenAPI / Swagger UI |
| Testing             | JUnit 5, Mockito, MockMvc      |
| Integration Testing | Testcontainers                 |
| Containerization    | Docker, Docker Compose         |
| Build Tool          | Maven                          |
| CI                  | GitHub Actions                 |

---

# Architecture

The application follows a layered architecture combined with a **cache-aside strategy** and **rate-limiting interceptor**.

```text
                         ┌───────────────────┐
                         │      Client       │
                         └─────────┬─────────┘
                                   │
                                   ▼
                         ┌───────────────────┐
                         │   Spring MVC      │
                         │    Controller     │
                         └─────────┬─────────┘
                                   │
                         ┌─────────▼─────────┐
                         │ Rate Limit         │
                         │ Interceptor        │
                         └─────────┬─────────┘
                                   │
                                   ▼
                         ┌───────────────────┐
                         │   Service Layer   │
                         └───────┬─────┬─────┘
                                 │     │
                    Cache Hit ───┘     └── Cache Miss
                      │                         │
                      ▼                         ▼
              ┌──────────────┐          ┌──────────────┐
              │    Redis     │          │ PostgreSQL   │
              │    Cache     │          │  Repository  │
              └──────────────┘          └──────────────┘
```

The main design principle is:

```text
Read Request
     │
     ▼
Rate Limit Check
     │
     ▼
Redis Cache
     │
     ├── HIT ──────► Return cached URL
     │
     └── MISS
           │
           ▼
       PostgreSQL
           │
           ▼
       Populate Redis
           │
           ▼
       Return URL
```

This prevents PostgreSQL from becoming the bottleneck when the same short URLs are requested repeatedly.

---

# Scalability Architecture

## 1. Read-Heavy Workload

URL shorteners typically have a highly asymmetric workload.

For example:

```text
URL Creation       → Low volume
URL Statistics     → Moderate volume
URL Deletion       → Low volume
URL Redirection    → Very high volume
```

A typical workload can look like:

```text
          1 URL Creation
                │
                ▼
       ┌─────────────────┐
       │                 │
       │   Many Reads    │
       │                 │
       └─────────────────┘
```

Therefore, the architecture prioritizes **fast reads**.

---

# Redis Caching

Redis is used as a high-speed cache in front of PostgreSQL.

## Cache-Aside Strategy

The application follows the cache-aside pattern.

```text
                  GET /Ab12Cd
                       │
                       ▼
                 Redis Cache
                  /        \
                HIT        MISS
                 │           │
                 ▼           ▼
             Redirect     PostgreSQL
                              │
                              ▼
                         Store in Redis
                              │
                              ▼
                           Redirect
```

### Cache Hit

When the short code exists in Redis:

```text
Client
  │
  ▼
Redis
  │
  ▼
Original URL
```

PostgreSQL is not required for the lookup.

### Cache Miss

When the short code does not exist in Redis:

```text
Client
  │
  ▼
Redis
  │
  └── MISS
       │
       ▼
   PostgreSQL
       │
       ▼
   Redis Cache
       │
       ▼
   Original URL
```

This significantly reduces database read pressure for popular URLs.

---

# Why Redis?

Without caching:

```text
100,000 redirects
        │
        ▼
100,000 database lookups
```

With caching:

```text
100,000 redirects
        │
        ▼
Redis
 │
 ├── 95,000 cache hits
 │
 └── 5,000 database lookups
```

The exact cache-hit ratio depends on traffic patterns, cache expiration, and the number of unique URLs.

Redis therefore acts as the first read layer while PostgreSQL remains the source of truth.

---

# Rate Limiting

The application implements Redis-backed rate limiting through a Spring MVC interceptor.

Rate limiting is applied before requests reach the controller.

```text
Client
   │
   ▼
RateLimitInterceptor
   │
   ▼
Redis Rate Limit
   │
   ├── Allowed ─────► Controller
   │
   └── Rejected ────► HTTP 429
```

## Current Limits

| Operation  |       Limit |     Window |
| ---------- | ----------: | ---------: |
| Create URL | 10 requests | 60 seconds |
| Redirect   | 60 requests | 60 seconds |
| Statistics | 30 requests | 60 seconds |
| Delete URL | 10 requests | 60 seconds |

The limits are applied per client IP.

---

# Rate Limit Headers

Successful requests include:

```http
X-RateLimit-Limit: 10
X-RateLimit-Remaining: 9
```

When the limit is exceeded:

```http
HTTP/1.1 429 Too Many Requests
Retry-After: 42
```

Example response:

```json
{
  "status": 429,
  "error": "Too Many Requests",
  "message": "Rate limit exceeded. Please try again later.",
  "retryAfterSeconds": 42
}
```

---

# Why Rate Limiting?

Rate limiting protects the system from:

* Accidental request floods
* API abuse
* Automated scraping
* Excessive URL creation
* Redirect abuse
* Resource exhaustion

Redis is suitable for this because rate-limit state can be shared across multiple application instances.

---

# Horizontal Scalability

The application is designed so that the Spring Boot application layer can be scaled horizontally.

Instead of:

```text
                 Client
                    │
                    ▼
              ┌──────────┐
              │   App    │
              └──────────┘
                    │
                    ▼
                Database
```

The application can scale to:

```text
                       ┌──────────────┐
                       │    Client    │
                       └───────┬──────┘
                               │
                               ▼
                       ┌──────────────┐
                       │Load Balancer │
                       └───────┬──────┘
                               │
              ┌────────────────┼────────────────┐
              │                │                │
              ▼                ▼                ▼
        ┌──────────┐     ┌──────────┐     ┌──────────┐
        │ App #1   │     │ App #2   │     │ App #3   │
        └────┬─────┘     └────┬─────┘     └────┬─────┘
             │                │                │
             └────────────────┼────────────────┘
                              │
                ┌─────────────┴─────────────┐
                │                           │
                ▼                           ▼
          ┌───────────┐               ┌───────────┐
          │   Redis   │               │ PostgreSQL│
          └───────────┘               └───────────┘
```

Because rate-limit state and cache data are stored externally in Redis, application instances do not need to maintain local state for these concerns.

---

# Database as Source of Truth

PostgreSQL remains the authoritative persistent storage layer.

Redis is treated as a cache rather than the primary database.

```text
                Application
                     │
              ┌──────┴──────┐
              │             │
              ▼             ▼
            Redis       PostgreSQL
            Cache        Source of Truth
```

If Redis is restarted or its cache is cleared, the application can retrieve the required URL information from PostgreSQL and rebuild the cache.

---

# Database Design

The core database consists of a `urls` table.

Conceptually:

```text
┌─────────────────────────────────────────────┐
│                    urls                     │
├─────────────────────────────────────────────┤
│ id                                          │
│ short_code                                  │
│ original_url                                │
│ created_at                                  │
│ expires_at                                  │
│ click_count                                 │
└─────────────────────────────────────────────┘
```

Important database responsibilities include:

* Persistent URL storage
* Unique short-code enforcement
* URL expiration information
* Click-count persistence
* Querying by short code

The `short_code` field should be indexed/unique because it is the primary lookup key for redirects.

---

# Short-Code Generation

The service generates a compact short code for each URL.

The short code is used as the public identifier:

```text
https://example.com/very/long/url

              ↓

http://localhost:8080/Ab12Cd
```

The short code must satisfy the following properties:

* Compact
* URL-safe
* Unique
* Fast to generate
* Efficient to query
* Collision-safe through database uniqueness enforcement

The database remains the final authority for uniqueness.

If a generated code conflicts with an existing code, the service can generate another code rather than allowing duplicate identifiers.

---

# Redirect Flow

The redirect path is optimized for high read volume.

```text
GET /Ab12Cd
      │
      ▼
Rate Limit Interceptor
      │
      ▼
Redis
      │
      ├── HIT
      │    │
      │    ▼
      │  Original URL
      │
      └── MISS
           │
           ▼
       PostgreSQL
           │
           ├── Not Found → 404
           │
           ├── Expired   → 410
           │
           ▼
       Store in Redis
           │
           ▼
       Original URL
```

The redirect path should therefore avoid unnecessary database queries whenever a popular URL is already cached.

---

# Scalability Considerations

The current architecture provides several foundations for scaling:

### Application Layer

Spring Boot instances are stateless with respect to:

* Redis cache state
* Rate-limit counters
* Persistent URL data

This allows multiple application instances to run behind a load balancer.

### Cache Layer

Redis handles:

* Frequently accessed URL lookups
* Rate-limit counters
* Shared state across application instances

### Database Layer

PostgreSQL remains the source of truth and can later be scaled using:

* Connection pooling
* Proper indexing
* Read replicas
* Query optimization
* Partitioning for very large datasets

### Load Balancing

Multiple Spring Boot instances can be placed behind:

```text
Load Balancer
      │
 ┌────┼────┐
 ▼    ▼    ▼
App  App  App
```

### Future Database Scaling

For very large workloads:

```text
                  Application
                      │
              ┌───────┴───────┐
              │               │
              ▼               ▼
           Redis          PostgreSQL
                              │
                    ┌─────────┴─────────┐
                    ▼                   ▼
              Primary DB          Read Replicas
```

Read replicas can be introduced when read traffic becomes too large for a single PostgreSQL instance.

---

# Consistency Considerations

Redis is not treated as the source of truth.

The expected consistency model is:

```text
PostgreSQL
    │
    │ source of truth
    ▼
Redis
    │
    │ cached representation
    ▼
Application
```

When URL data changes or a URL is deleted, the corresponding cache entry should be invalidated so stale data is not served.

This is particularly important for:

* URL deletion
* URL expiration
* Changes to URL metadata

---

# Failure Handling

## Redis Unavailable

Redis should be treated as a supporting infrastructure component rather than the permanent source of truth.

A production deployment can be designed so that a Redis failure does not result in permanent data loss because URL data remains in PostgreSQL.

The application can fall back to database reads where appropriate.

## PostgreSQL Unavailable

PostgreSQL is the persistent source of truth.

If PostgreSQL is unavailable:

* URL creation cannot be safely persisted
* Cache misses cannot be populated from the database
* Statistics updates may fail

Therefore PostgreSQL requires appropriate backups, monitoring, connection pooling, and high-availability planning for production deployments.

---

# REST API Endpoints

| Method | Endpoint                         | Description              |
| ------ | -------------------------------- | ------------------------ |
| POST   | `/api/v1/urls`                   | Create Short URL         |
| GET    | `/{shortCode}`                   | Redirect to Original URL |
| GET    | `/api/v1/urls/{shortCode}/stats` | Retrieve URL Statistics  |
| DELETE | `/api/v1/urls/{shortCode}`       | Delete Short URL         |

---

# API Examples

## Create Short URL

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

# Redirect

```http
GET /Ab12Cd
```

The application resolves the short code and redirects the client to the original URL.

Example:

```text
http://localhost:8080/Ab12Cd
        │
        ▼
https://www.google.com
```

---

# URL Statistics

```http
GET /api/v1/urls/Ab12Cd/stats
```

Example response:

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

# Delete Short URL

```http
DELETE /api/v1/urls/Ab12Cd
```

Successful response:

```http
204 No Content
```

The associated cache entry should also be invalidated.

---

# Error Handling

The application uses centralized exception handling.

| HTTP Status | Meaning               |
| ----------: | --------------------- |
|         400 | Validation Error      |
|         404 | Resource Not Found    |
|         409 | Duplicate Short Code  |
|         410 | URL Expired           |
|         429 | Rate Limit Exceeded   |
|         500 | Internal Server Error |

Example:

```json
{
  "timestamp": "2026-08-09T08:05:24.922",
  "status": 404,
  "error": "Not Found",
  "message": "Short URL 'DOESNOTEXIST' not found"
}
```

---

# Project Structure

```text
url-shortener
│
├── src
│   ├── main
│   │   ├── java
│   │   │   └── com.sandeep.urlshortener
│   │   │       │
│   │   │       ├── config
│   │   │       │   └── RateLimitConfig
│   │   │       │
│   │   │       ├── controller
│   │   │       │   └── UrlController
│   │   │       │
│   │   │       ├── dto
│   │   │       │   ├── request
│   │   │       │   └── response
│   │   │       │
│   │   │       ├── entity
│   │   │       │   └── Url
│   │   │       │
│   │   │       ├── exception
│   │   │       │   ├── DuplicateShortCodeException
│   │   │       │   ├── ResourceNotFoundException
│   │   │       │   └── UrlExpiredException
│   │   │       │
│   │   │       ├── interceptor
│   │   │       │   └── RateLimitInterceptor
│   │   │       │
│   │   │       ├── repository
│   │   │       │   └── UrlRepository
│   │   │       │
│   │   │       ├── service
│   │   │       │   ├── UrlService
│   │   │       │   ├── RateLimitService
│   │   │       │   └── impl
│   │   │       │
│   │   │       └── util
│   │   │
│   │   └── resources
│   │       ├── db
│   │       │   └── migration
│   │       └── application.properties
│   │
│   └── test
│       └── java
│           └── com.sandeep.urlshortener
│               ├── controller
│               ├── repository
│               └── service
│
├── .github
│   └── workflows
│       └── ci.yml
│
├── Dockerfile
├── docker-compose.yml
├── pom.xml
└── README.md
```

---

# Getting Started

## Prerequisites

Install:

* Java 17+
* Maven 3.9+
* Docker Desktop
* Git

---

# Clone Repository

```bash
git clone https://github.com/Sandeepkumar1703/url-shortener.git

cd url-shortener
```

---

# Running with Docker

Start the complete application stack:

```bash
docker compose up --build
```

Run in detached mode:

```bash
docker compose up -d
```

Stop the application:

```bash
docker compose down
```

The application will be available at:

```text
http://localhost:8080
```

---

# Running Without Docker

Start PostgreSQL and Redis locally.

Then run:

```bash
mvn spring-boot:run
```

---

# API Documentation

Swagger UI:

```text
http://localhost:8080/swagger-ui/index.html
```

OpenAPI specification:

```text
http://localhost:8080/v3/api-docs
```

---

# Running Tests

Run all tests:

```bash
mvn test
```

Run the complete Maven verification lifecycle:

```bash
mvn verify
```

The current test suite covers:

* Controller tests
* Service tests
* Repository tests
* MockMvc tests
* Mockito unit tests
* PostgreSQL integration tests
* Testcontainers integration tests

Current result:

```text
Tests run: 33
Failures: 0
Errors: 0
Skipped: 0

BUILD SUCCESS
```

---

# Database Migrations

Flyway manages database schema evolution.

Migration files are located at:

```text
src/main/resources/db/migration
```

Flyway automatically applies pending migrations during application startup.

---

# Validation

Incoming requests are validated using Jakarta Bean Validation.

Examples include:

* Required URL fields
* Valid URL format
* Non-empty values
* Valid expiration date
* Future expiration timestamp

---

# Docker

The project contains:

```text
Dockerfile
docker-compose.yml
```

The Docker Compose environment provides the infrastructure required by the application.

Typical architecture:

```text
Docker Compose
│
├── Spring Boot Application
│
├── PostgreSQL
│
└── Redis
```

Start:

```bash
docker compose up --build
```

Stop:

```bash
docker compose down
```

---

# Continuous Integration

GitHub Actions validates the project automatically.

The CI pipeline performs:

```text
Push / Pull Request
        │
        ▼
   Maven Build
        │
        ▼
   Compile Code
        │
        ▼
   Run Tests
        │
        ▼
 Integration Tests
        │
        ▼
   Maven Verify
```

This ensures that changes are automatically checked before merging.

---

# Production Scalability Roadmap

The current architecture provides the foundation for horizontal scaling.

Potential future improvements include:

### Application

* Multiple Spring Boot instances
* Load balancer
* Health checks
* Graceful shutdown
* Connection pool tuning

### Redis

* Redis Sentinel
* Redis Cluster
* High-availability Redis deployment
* Cache monitoring

### PostgreSQL

* Read replicas
* Connection pool optimization
* Query/index optimization
* Partitioning for very large datasets
* Automated backups
* PostgreSQL high availability

### Infrastructure

* Kubernetes deployment
* Horizontal Pod Autoscaling
* AWS deployment
* Managed PostgreSQL
* Managed Redis
* Infrastructure as Code using Terraform

### Observability

* Prometheus
* Grafana
* Centralized logging
* Distributed tracing
* Application metrics

---

# Future Enhancements

The core URL-shortening system is implemented. Potential product and infrastructure enhancements include:

* JWT Authentication
* User Accounts
* Custom Short URLs
* QR Code Generation
* Advanced URL Analytics
* Scheduled cleanup of expired URLs
* Kubernetes Deployment
* AWS Deployment
* Prometheus & Grafana Monitoring
* Distributed Tracing
* Redis High Availability
* PostgreSQL Read Replicas
* Load Balancer
* Horizontal Auto Scaling
* Production observability

---

# Design Decisions

## Why PostgreSQL?

PostgreSQL provides:

* Strong consistency
* Durable persistent storage
* Transaction support
* Unique constraints
* Mature indexing
* Reliable relational data management

It is used as the system of record.

## Why Redis?

Redis provides:

* Extremely fast key-value lookups
* Shared state across application instances
* Efficient rate limiting
* Low-latency caching

It is used for performance-sensitive ephemeral data.

## Why Spring Boot?

Spring Boot provides:

* Production-ready REST development
* Dependency injection
* Spring MVC
* Spring Data JPA
* Validation
* Exception handling
* Easy testing
* Docker-friendly deployment

---

# Architecture Summary

The final architecture can be summarized as:

```text
                         ┌─────────────────┐
                         │     Clients     │
                         └────────┬────────┘
                                  │
                                  ▼
                         ┌─────────────────┐
                         │  Load Balancer  │
                         └────────┬────────┘
                                  │
                    ┌─────────────┼─────────────┐
                    │             │             │
                    ▼             ▼             ▼
                ┌───────┐     ┌───────┐     ┌───────┐
                │ App 1 │     │ App 2 │     │ App 3 │
                └───┬───┘     └───┬───┘     └───┬───┘
                    │             │             │
                    └─────────────┼─────────────┘
                                  │
                         ┌────────▼────────┐
                         │ Rate Limiting   │
                         │     Redis       │
                         └────────┬────────┘
                                  │
                         ┌────────▼────────┐
                         │   URL Cache     │
                         │     Redis       │
                         └────────┬────────┘
                                  │
                              Cache Miss
                                  │
                                  ▼
                         ┌─────────────────┐
                         │   PostgreSQL    │
                         │ Source of Truth │
                         └─────────────────┘
```

The architecture separates responsibilities:

```text
PostgreSQL → Durable persistence
Redis      → Cache + rate limiting
Spring Boot → Business logic and API
Docker     → Containerized deployment
GitHub CI  → Automated verification
```

This provides a strong foundation for scaling the URL shortener from a single application instance to a horizontally scaled production architecture.

---

# Author

**Sandeep Kumar Prasad**

Backend Developer

### Core Technologies

* Java
* Spring Boot
* REST APIs
* PostgreSQL
* Redis
* Docker
* Maven
* GitHub Actions
* JUnit
* Mockito
* Testcontainers

---

# License

This project is licensed under the **MIT License**.
