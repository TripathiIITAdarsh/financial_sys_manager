# Personal Finance Manager

A Spring Boot 3 web application that lets users track income, expenses, savings goals, and generate financial reports.

## Tech Stack

- Java 17
- Spring Boot 3.2 (Web, Data JPA, Security, Validation)
- H2 in-memory database
- JUnit 5 + Spring Security Test (JaCoCo for coverage)
- Maven

## Architecture

Layered: `Controller` → `Service` → `Repository`, with DTOs at the API boundary and JPA entities at the persistence layer.

```
src/main/java/com/financemanager
├── PersonalFinanceManagerApplication.java
├── config/        # CommandLineRunner seeding default categories
├── controller/    # REST endpoints (auth, transactions, categories, goals, reports)
├── dto/           # Request/response DTOs with bean validation
├── entity/        # JPA entities: User, Category, Transaction, SavingsGoal
├── exception/     # Custom exceptions + @ControllerAdvice handler
├── repository/    # Spring Data JPA repositories
├── security/      # Security config, AuthN entry points, current-user helper
└── service/       # Business logic
```

Authentication is session-based using a `JSESSIONID` (renamed `FMSESSIONID` in production) HTTP-only cookie. All `/api/**` endpoints (except `register` and `login`) require an authenticated session.

## Running locally

```bash
# build & run unit tests
mvn clean test

# run the app
mvn spring-boot:run
```

The API is then served at `http://localhost:8080/api/...`. Default categories are seeded on first start.

## Test coverage

`mvn test` produces a JaCoCo report under `target/site/jacoco/index.html`.

## Deploying to Render

The repo includes a `Dockerfile` and `render.yaml`. On Render:

1. Create a new **Web Service** from this repo.
2. Choose **Docker** as the runtime (Render will pick up `Dockerfile`).
3. Render injects `PORT`; the app already binds to `${PORT:8080}`.

## API Summary

### Auth
- `POST /api/auth/register` — body: `{username,password,fullName,phoneNumber}`
- `POST /api/auth/login` — body: `{username,password}` — sets session cookie
- `POST /api/auth/logout` — invalidates session

### Transactions
- `POST   /api/transactions` — body: `{amount,date,category,description?}`
- `GET    /api/transactions?startDate&endDate&categoryId`
- `PUT    /api/transactions/{id}` — body: `{amount?,category?,description?}`
- `DELETE /api/transactions/{id}`

### Categories
- `GET    /api/categories`
- `POST   /api/categories` — body: `{name,type}` (type = INCOME | EXPENSE)
- `DELETE /api/categories/{name}` — only for custom categories

### Savings Goals
- `POST   /api/goals` — body: `{goalName,targetAmount,targetDate,startDate?}`
- `GET    /api/goals`
- `GET    /api/goals/{id}`
- `PUT    /api/goals/{id}` — body: `{targetAmount?,targetDate?}`
- `DELETE /api/goals/{id}`

### Reports
- `GET /api/reports/monthly/{year}/{month}`
- `GET /api/reports/yearly/{year}`

### Error Format

All errors return JSON: `{ "message": "<description>" }` with HTTP status:

| Status | Meaning |
|--------|---------|
| 400 | Validation / malformed input |
| 401 | Missing or invalid auth |
| 403 | Accessing data you don't own |
| 404 | Resource not found |
| 409 | Conflict (e.g. duplicate username/category) |

## Design Decisions

- **Default categories** are stored in the DB with `user = null` and a seeder fills them on startup.
- **Categories are name-scoped per user**; defaults are visible globally. Custom names are unique per user and cannot collide with defaults.
- **Transactions** cannot be created with a future date, and the `date` field is immutable on update.
- **Savings goal progress** is computed lazily as `sum(income) - sum(expenses)` between `startDate` and `min(today, targetDate)`. Deleting a transaction removes its contribution.
- **Data isolation** is enforced in the repository layer: queries always include the current user.
- **Errors** flow through a `@ControllerAdvice` global handler so no 5xx leaks for known scenarios.
