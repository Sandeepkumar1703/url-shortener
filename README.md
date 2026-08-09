# URL Shortener API

> A production-oriented **URL Shortener REST API** built with **Java 17, Spring Boot 3, PostgreSQL, Redis, Flyway, Docker, and GitHub Actions**.
>
> The application provides URL creation, redirection, expiration handling, click tracking, statistics, deletion, Redis caching, IP-based rate limiting, centralized exception handling, automated testing, containerization, and CI.
>
> The architecture is designed around a **read-heavy workload**, where URL redirects can significantly outnumber URL creation and management operations.

---

# Features

* Create short URLs for valid URLs
* Generate compact, URL-safe short codes
* Collision-safe short-code generation
* Redirect short URLs to original destinations
* URL expiration support
* Track click counts
* Retrieve URL statistics
* Delete shortened URLs
* Request validation using Jakarta Bean Validation
* Centralized global exception handling
* Duplicate short-code protection
* Redis caching for frequently accessed URLs
* Cache-aside caching strategy
* Cache invalidation on URL deletion
* Redis-backed rate limiting
* IP-based request throttling
* Rate-limit response headers
* `Retry-After` support for throttled requests
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
* Horizontal scalability design

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

The application follows a layered architecture combined with:

* Cache-aside Redis caching
* Redis-backed rate limiting
* PostgreSQL as the source of truth
* Stateless application instances
* Read-heavy redirect optimization
* Horizontal scalability design

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
                         │ Rate Limit        │
                         │ Interceptor       │
                         └─────────┬─────────┘
                                   │
                                   ▼
                         ┌───────────────────┐
                         │   Service Layer   │
                         └─────────┬─────────┘
                                   │
                         ┌─────────▼─────────┐
                         │      Redis        │
                         │ Cache + Rate Limit│
                         └─────────┬─────────┘
                                   │
                              Cache Miss
                                   │
                                   ▼
                         ┌───────────────────┐
                         │    PostgreSQL     │
                         │  Source of Truth  │
                         └───────────────────┘
```

---

# Core Design Requirements

The URL shortener is designed around the following system-design concepts:

| Requirement             | Implementation                                                    |
| ----------------------- | ----------------------------------------------------------------- |
| Short-code generation   | Compact URL-safe identifiers with database uniqueness enforcement |
| Database design         | PostgreSQL with indexed/unique short-code lookup                  |
| Redis caching           | Cache-aside strategy for redirect lookups                         |
| Read-heavy architecture | Redis-first redirect path                                         |
| Rate limiting           | Redis-backed, IP-based request limiting                           |
| Scalability basics      | Stateless application design and horizontal scaling architecture  |

---

# Short-Code Generation

The application generates a compact URL-safe identifier for each URL.

Example:

```text
https://example.com/very/long/url

              ↓

http://localhost:8080/Ab12Cd
```

The short code is designed to be:

* Compact
* URL-safe
* Efficient to query
* Fast to generate
* Collision-safe

### Important Design Note

The short code is an **identifier**, not a cryptographic hash.

The database remains the final authority for uniqueness through a unique constraint on the short-code field.

Conceptually:

```text
Generate short code
        │
        ▼
Check uniqueness
        │
        ├── Available ──► Save URL
        │
        └── Collision ──► Generate another code
```

This prevents duplicate public identifiers.

---

# Database Design

PostgreSQL is the persistent source of truth.

The core database consists of a `urls` table.

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

The `short_code` field is the primary lookup key for redirect operations and should have a unique index/constraint.

---

# Redis Caching

Redis is used as a high-speed cache in front of PostgreSQL.

The application follows the **cache-aside pattern**.

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

## Cache Hit

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

PostgreSQL is not required for the URL lookup.

## Cache Miss

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

This reduces database read pressure for frequently accessed URLs.

---

# Redis Key Design

The application uses Redis for more than one purpose.

Different key namespaces separate cache data from rate-limit state.

```text
Redis
│
├── URL Cache
│   └── urlCache::{shortCode}
│
└── Rate Limits
    └── rate_limit:{operation}:{clientIp}
```

Example:

```text
urlCache::5MkILI

rate_limit:create:172.18.0.1

rate_limit:redirect:172.18.0.1
```

This allows a single Redis deployment to support both caching and distributed rate-limit state.

---

# Cache Invalidation

Redis is treated as a cache, not the source of truth.

The expected lifecycle is:

```text
Create
  │
  ▼
PostgreSQL
  │
  ▼
Redis Cache
```

For reads:

```text
Redis
 │
 ├── HIT  → Return cached URL
 │
 └── MISS → PostgreSQL → Redis → Return URL
```

For deletion:

```text
DELETE URL
     │
     ▼
PostgreSQL
     │
     ▼
Invalidate Redis cache
```

Cache invalidation is important for preventing deleted or outdated URLs from being served from Redis.

---

# Read-Heavy Architecture

URL shorteners typically have an asymmetric workload:

```text
URL Creation       → Low volume
URL Statistics     → Moderate volume
URL Deletion       → Low volume
URL Redirection    → Very high volume
```

Therefore, the redirect path is optimized for high read volume.

```text
Read Request
     │
     ▼
Rate Limit Check
     │
     ▼
Redis Cache
     │
     ├── HIT ──────► Return URL
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

The objective is to prevent PostgreSQL from becoming the bottleneck when popular short URLs are repeatedly accessed.

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
       ├── Cache hits
       │
       └── Cache misses → PostgreSQL
```

The actual cache-hit ratio depends on traffic patterns, cache expiration, and the number of unique URLs.

Redis therefore acts as the first read layer while PostgreSQL remains the system of record.

---

# Rate Limiting

The application implements Redis-backed rate limiting through a Spring MVC `HandlerInterceptor`.

Rate limiting occurs before requests reach the controller.

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

Rate limiting is currently based on the client IP address.

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

# Rate Limiting and Proxies

The interceptor checks:

```text
X-Forwarded-For
X-Real-IP
Remote Address
```

When deployed behind a reverse proxy or load balancer, forwarded IP headers should only be trusted when they are supplied by a trusted proxy.

Otherwise, clients may be able to spoof forwarded IP information.

A production deployment should therefore configure trusted proxy handling appropriately.

---

# Why Rate Limiting?

Rate limiting protects the application from:

* Accidental request floods
* API abuse
* Automated scraping
* Excessive URL creation
* Redirect abuse
* Resource exhaustion

Because rate-limit state is stored in Redis, multiple Spring Boot instances can share the same rate-limit state.

---

# Horizontal Scalability

The current Docker Compose environment runs a single Spring Boot application instance.

However, the application is designed so that the application layer can be scaled horizontally.

Current deployment:

```text
                 Client
                    │
                    ▼
              ┌──────────┐
              │   App    │
              └────┬─────┘
                   │
          ┌────────┴────────┐
          ▼                 ▼
       Redis           PostgreSQL
```

Scalable target architecture:

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
        │  App #1  │     │  App #2  │     │  App #3  │
        └────┬─────┘     └────┬─────┘     └────┬─────┘
             │                │                │
             └────────────────┼────────────────┘
                              │
                 ┌────────────┴────────────┐
                 │                         │
                 ▼                         ▼
           ┌───────────┐             ┌───────────┐
           │   Redis   │             │ PostgreSQL│
           │Cache+Rate │             │Source of  │
           │  Limits   │             │   Truth   │
           └───────────┘             └───────────┘
```

Because cache and rate-limit state are externalized into Redis and persistent data is stored in PostgreSQL, application instances do not need to maintain local state for these concerns.

---

# Database as Source of Truth

PostgreSQL remains the authoritative persistent storage layer.

```text
                Application
                     │
              ┌──────┴──────┐
              │             │
              ▼             ▼
            Redis       PostgreSQL
            Cache        Source of Truth
```

If Redis is restarted or its cache is cleared, URL data can be retrieved from PostgreSQL and the cache can be rebuilt through the cache-aside flow.

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

Popular URLs can therefore be served without repeatedly querying PostgreSQL.

---

# Consistency Considerations

Redis is not treated as the source of truth.

The consistency model is:

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

When URL data changes or a URL is deleted, the corresponding Redis entry should be invalidated.

This is particularly important for:

* URL deletion
* URL expiration
* URL metadata changes

---

# Failure Handling

## Redis Unavailable

Redis is a supporting infrastructure component rather than the permanent source of truth.

A production implementation can allow database-backed fallback for cache misses so that temporary Redis failure does not cause permanent data loss.

URL data remains persisted in PostgreSQL.

Redis failure may still affect:

* Cache performance
* Rate limiting
* Distributed request state

High-availability Redis can be introduced for production deployments.

## PostgreSQL Unavailable

PostgreSQL is the persistent source of truth.

If PostgreSQL is unavailable:

* URL creation cannot be safely persisted
* Cache misses cannot be populated
* Statistics updates may fail

Production deployments should therefore use appropriate:

* Backups
* Monitoring
* Connection pooling
* High-availability planning

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

The associated Redis cache entry should also be invalidated.

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

Docker Compose provides:

```text
Spring Boot
PostgreSQL
Redis
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

The current automated test suite covers:

* Controller tests
* Service tests
* Repository tests
* MockMvc tests
* Mockito unit tests
* PostgreSQL integration tests
* Testcontainers integration tests

Current verified result:

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

```text
Docker Compose
│
├── Spring Boot Application
├── PostgreSQL
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

# Scalability Basics

The current implementation establishes the fundamental building blocks required to scale the service.

## Current Architecture

```text
Client
  │
  ▼
Spring Boot
  │
  ├── Redis Cache
  │
  ├── Redis Rate Limits
  │
  └── PostgreSQL
```

## Horizontal Scaling

The application layer can be replicated:

```text
                 Load Balancer
                       │
          ┌────────────┼────────────┐
          ▼            ▼            ▼
        App #1       App #2       App #3
          │            │            │
          └────────────┼────────────┘
                       │
              ┌────────┴────────┐
              ▼                 ▼
            Redis          PostgreSQL
```

The application does not rely on local in-memory cache or local rate-limit state, allowing multiple instances to share Redis-backed state.

---

# Production Scalability Roadmap

The current project implements the core scalability foundations. The following are possible future improvements.

### Application

* Multiple Spring Boot instances
* Load balancer
* Health checks
* Graceful shutdown
* Connection-pool tuning
* Horizontal Pod Autoscaling

### Redis

* Redis Sentinel
* Redis Cluster
* High-availability Redis
* Cache monitoring

### PostgreSQL

* Read replicas
* Connection-pool optimization
* Query/index optimization
* Partitioning for very large datasets
* Automated backups
* PostgreSQL high availability

### Infrastructure

* Kubernetes deployment
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

Potential product and infrastructure enhancements include:

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

It is used for performance-sensitive and ephemeral data.

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

The implemented system can be summarized as:

```text
                         ┌─────────────────┐
                         │     Clients     │
                         └────────┬────────┘
                                  │
                                  ▼
                         ┌─────────────────┐
                         │  Spring Boot    │
                         │   Application   │
                         └────────┬────────┘
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

For horizontal scaling:

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
                         │      Redis      │
                         │ Cache + Limits  │
                         └────────┬────────┘
                                  │
                                  ▼
                         ┌─────────────────┐
                         │   PostgreSQL    │
                         │ Source of Truth │
                         └─────────────────┘
```

The architecture separates responsibilities:

```text
Short-code generation → Compact URL-safe identifiers
PostgreSQL            → Durable persistence
Redis                 → Cache + rate limiting
Spring Boot           → API and business logic
Docker                → Containerized deployment
GitHub Actions        → Automated verification
```

This provides the core foundations required for a scalable URL-shortening service while keeping PostgreSQL as the durable source of truth and Redis as the high-performance shared infrastructure layer.

---

# Requirement Coverage

The project covers the requested URL Shortener system-design requirements:

| Requirement                     | Status | Implementation                                                   |
| ------------------------------- | ------ | ---------------------------------------------------------------- |
| Hashing / Short-code generation | ✅      | Compact URL-safe identifiers with collision protection           |
| Database Design                 | ✅      | PostgreSQL, `urls` table, unique short-code constraint, Flyway   |
| Redis Caching                   | ✅      | Cache-aside strategy and Redis URL cache                         |
| Read-heavy Architecture         | ✅      | Redis-first redirect path                                        |
| Rate Limiting                   | ✅      | Redis-backed IP-based limits                                     |
| Scalability Basics              | ✅      | Stateless application design and horizontal scaling architecture |

> **Note:** Short-code generation is an identifier-generation mechanism rather than cryptographic hashing. The database uniqueness constraint provides the final collision-safety guarantee.

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