# ShopLite — a simple online store

A student project for the *Software Development Life Cycle* course (Igor Sikorsky Kyiv Polytechnic Institute, Faculty of Informatics and Computer Engineering).
Customers browse the catalog, fill a cart and place orders paid on delivery;
an administrator manages the catalog and processes orders.

- Requirements specification — Lab 1 (FR-01 … FR-15, NFR-01 … NFR-08)
- Architecture and UML — Lab 2, see [docs/architecture.md](docs/architecture.md)
- Task board — Jira, project **SCRUM** (ShopLite)

## Tech stack

| Part | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 4.1 (Spring MVC, Spring Data JPA, Spring Security, Validation) |
| Views | Thymeleaf + Bootstrap 5 |
| Database | PostgreSQL (H2 for local runs and tests) |
| Migrations | Flyway |
| Build | Maven (wrapper `mvnw` included) |
| Tests | JUnit 5, Mockito, Spring Boot Test |

## Project structure

```
src/main/java/com/shoplite
├── ShopLiteApplication.java   entry point
├── config/                    SecurityConfig (BCrypt, roles, CSRF)
├── common/                    business exceptions, error handling, current user info
├── user/                      User, Role, registration and login (FR-01, FR-02)
├── catalog/                   Category, Product, catalog and product page (FR-03 – FR-05)
├── cart/                      Cart, CartItem, CartService (FR-06)
├── order/                     Order, OrderItem, OrderStatus, OrderService (FR-07 – FR-10, FR-14)
└── stats/                     StatsService — sales statistics (FR-15)
src/main/resources
├── application.properties             settings (H2 by default)
├── application-postgres.properties    PostgreSQL profile
├── db/migration/                      Flyway migrations (schema and seed data)
├── templates/                         Thymeleaf pages
└── static/css/
src/test/java/com/shoplite     unit and integration tests
docs/                          documentation (architecture)
.run/                          IntelliJ IDEA run configurations
```

Each module is split into layers: **controller → service (interface + implementation) → repository**.
Controllers never call repositories directly, and services depend on interfaces (SOLID).

## Running

Requires JDK 21. A separate Maven installation is not needed — use the wrapper.

**Quick start (in-memory H2 database):**

```bash
./mvnw spring-boot:run          # Windows: mvnw.cmd spring-boot:run
```

Open http://localhost:8080. Data is kept in memory and reset on restart.

**With PostgreSQL:** create a `shoplite` database and run with the `postgres` profile:

```bash
# Windows PowerShell
$env:DB_PASSWORD="your_password"; ./mvnw spring-boot:run "-Dspring-boot.run.profiles=postgres"
```

Environment variables: `DB_URL` (default `jdbc:postgresql://localhost:5432/shoplite`), `DB_USERNAME` (`postgres`), `DB_PASSWORD`.
In IntelliJ IDEA, set them in Run Configuration → Active profiles / Environment variables.

**In IntelliJ IDEA:** open the project folder and use the ready-made run configurations **ShopLite run** and **ShopLite tests**.

**Tests:**

```bash
./mvnw test
```

**Development admin account:** `admin@shoplite.local` / `admin12345` (created by migration V2, for local use only).

## Implementation status

| Requirement | Status |
|---|---|
| FR-01 Registration, FR-02 Log in / log out | ✅ Sprint 1 |
| FR-03 Paginated catalog, FR-04 Search / filter / sort, FR-05 Product page | ✅ Sprint 1 |
| NFR-02, NFR-03 BCrypt, roles, CSRF | ✅ Sprint 1 |
| FR-06 – FR-10 Cart and orders | 🔧 domain model and services done, pages in Sprint 2 |
| FR-12 – FR-15 Admin panel | ⏳ Sprint 2 / backlog |

## Team and roles

The project is done by one person — **Oksana Ostrozhynska, group IO-44** — acting as analyst, architect, backend and frontend developer, QA engineer and DevOps.

## Git workflow

- `main` is the stable branch; nobody commits to it directly;
- every Jira task gets its own branch `feature/SCRUM-<number>-<short-name>`;
- changes reach `main` only through a pull request after code review;
- commit messages start with the Jira key, e.g. `SCRUM-11: Add user registration`.
