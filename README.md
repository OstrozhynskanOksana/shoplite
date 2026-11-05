# ShopLite — a simple online store

A student project for the *Software Development Life Cycle* course (Igor Sikorsky Kyiv Polytechnic Institute, Faculty of Informatics and Computer Engineering).
Customers browse the catalog, fill a cart and place orders paid on delivery;
an administrator manages the catalog, processes orders and sees monthly sales.

- Requirements specification — Lab 1 (FR-01 … FR-15, NFR-01 … NFR-08)
- Architecture and UML — Lab 2, see [docs/architecture.md](docs/architecture.md)
- Task board — Jira, project **SCRUM** (ShopLite)

## Tech stack

| Part | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 4.1 (Spring MVC, Spring Data JPA, Spring Security, Validation) |
| Views | Thymeleaf + Bootstrap 5 |
| Database | PostgreSQL 16 (H2 for local runs and tests) |
| Migrations | Flyway |
| Build and dependencies | Maven (`pom.xml`, wrapper `mvnw` included) |
| Containers | Docker, Docker Compose |
| Tests | JUnit 5, Mockito, Spring Boot Test, Postman |

## Project structure

```
src/main/java/com/shoplite
├── ShopLiteApplication.java   entry point
├── config/                    SecurityConfig (BCrypt, roles, CSRF, HTTP Basic for the API)
├── common/                    business exceptions, error pages, current user helpers
├── user/                      registration, login, profile (FR-01, FR-02, FR-11)
├── catalog/                   categories, products, catalog pages, admin catalog service (FR-03 – FR-05, FR-12, FR-13)
├── cart/                      cart and cart page (FR-06)
├── order/                     checkout, order history, cancellation, status rules (FR-07 – FR-10, FR-14)
├── stats/                     monthly sales (FR-15)
├── admin/                     admin panel controllers (/admin/**)
└── api/                       REST API (/api/**)
src/main/resources
├── application.properties             settings (H2 by default)
├── application-postgres.properties    PostgreSQL profile
├── db/migration/                      Flyway migrations (schema and seed data)
├── templates/                         Thymeleaf pages
└── static/css/
src/test/java/com/shoplite     unit and integration tests
postman/                       Postman collection for the REST API
docs/                          documentation (architecture)
.run/                          IntelliJ IDEA run configurations
Dockerfile, docker-compose.yml
```

Each module is split into layers: **controller → service (interface + implementation) → repository**.
Controllers never call repositories directly, and services depend on interfaces (SOLID).

## Running

### Option 1 — Docker (application + PostgreSQL)

Requires Docker Desktop.

```bash
docker compose up --build
```

Open http://localhost:8080. Data is stored in the `pgdata` volume and survives restarts.
Stop with `docker compose down` (add `-v` to delete the data).

### Option 2 — locally with the in-memory H2 database

Requires JDK 21.

```bash
./mvnw spring-boot:run          # Windows: mvnw.cmd spring-boot:run
```

In IntelliJ IDEA open the project folder and use the run configurations **ShopLite run** and **ShopLite tests**.

### Option 3 — locally with your own PostgreSQL

Create a `shoplite` database and run with the `postgres` profile:

```bash
# Windows PowerShell
$env:DB_PASSWORD="your_password"; ./mvnw spring-boot:run "-Dspring-boot.run.profiles=postgres"
```

Environment variables: `DB_URL` (default `jdbc:postgresql://localhost:5432/shoplite`), `DB_USERNAME` (`postgres`), `DB_PASSWORD`.

### Accounts

- Administrator (created by migration V2, local use only): `admin@shoplite.local` / `admin12345`
- Customers register on the **Sign up** page.

## REST API

| Method | URL | Access | Description |
|---|---|---|---|
| GET | `/api/products?q=&category=&sort=price_asc\|price_desc&page=` | public | catalog page (12 items) |
| GET | `/api/products/{id}` | public | one active product |
| GET | `/api/orders/{id}/status` | HTTP Basic | status of an own order (any order for admins) |

Errors are returned as JSON `{"status": 404, "error": "Product not found"}`.
Import `postman/ShopLite.postman_collection.json` into Postman and run the collection to check the API.

## Tests

```bash
./mvnw test
```

## Requirements status

### Functional

| Requirement | Where | Status |
|---|---|---|
| FR-01 Registration | `/register` | ✅ |
| FR-02 Log in / log out | `/login`, header | ✅ |
| FR-03 Catalog, 12 per page | `/products`, `GET /api/products` | ✅ |
| FR-04 Search, category filter, price sorting | `/products` | ✅ |
| FR-05 Product page | `/products/{id}` | ✅ |
| FR-06 Cart | `/cart`, Add to cart on the product page | ✅ |
| FR-07 Checkout, pay on delivery | `/checkout` | ✅ |
| FR-08 Stock control | checkout and cancellation | ✅ |
| FR-09 Order history | `/orders`, `/orders/{id}` | ✅ |
| FR-10 Cancel a NEW order | order page | ✅ |
| FR-11 Profile | `/profile` | ✅ |
| FR-12 Manage products | `/admin/products` | ✅ |
| FR-13 Manage categories | `/admin/categories` | ✅ |
| FR-14 Process orders | `/admin/orders` | ✅ |
| FR-15 Monthly sales | `/admin/stats` | ✅ |

### Non-functional

| Requirement | How it is met |
|---|---|
| NFR-01 Performance | paginated catalog, indexes, entity graphs instead of N+1 queries; response time checked in the Postman collection |
| NFR-02 Password security | BCrypt hashes only |
| NFR-03 Access control | `/admin/**` for ADMIN only, login required for cart and orders, CSRF on forms, 401 for unauthenticated API calls |
| NFR-04 Reliable checkout | one transaction with row locks; full rollback when stock is insufficient |
| NFR-05 Input validation | Bean Validation on every form, readable messages next to fields, JSON errors in the API |
| NFR-06 UI | Bootstrap 5 responsive layout (360 px – 1920 px), English UI |
| NFR-07 Test coverage | unit and integration tests (extended in Lab 6) |
| NFR-08 One-command deployment | `docker compose up --build` |

## Team and roles

The project is done by one person — **Oksana Ostrozhynska, group IO-44** — acting as analyst, architect, backend and frontend developer, QA engineer and DevOps.

## Git workflow

- `main` is the stable branch; nobody commits to it directly;
- every Jira task gets its own branch `feature/SCRUM-<number>-<short-name>`;
- changes reach `main` only through a pull request after code review;
- commit messages start with the Jira key, e.g. `SCRUM-11: Add user registration`.
