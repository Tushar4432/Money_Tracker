# Money Tracker — Project Summary

## 1. Project Overview

**Money Tracker** is an AI-powered personal finance application that helps users understand and manage their spending. Users upload bank statements (CSV or XLSX), and the system automatically parses, categorizes, and analyzes their transactions. An AI financial coach provides personalized recommendations, health scores, and affordability checks based on the user's actual financial data.

### Key Features

- **Statement Ingestion** — Upload CSV/XLSX bank statements; duplicate uploads are silently ignored via transaction hashing
- **Auto-Categorization** — Rule-based classifier maps transaction descriptions to 14 categories (Food, Shopping, Transport, etc.)
- **Spending Analytics** — Income vs. expense summaries, category breakdowns, monthly trends, and period-over-period comparisons
- **Spending Goals** — Set category-level budgets (weekly/monthly/quarterly/yearly) with progress tracking
- **Smart Notifications** — Alerts when approaching or exceeding goal limits
- **AI Financial Coach** — Chat interface powered by a local LLM (Ollama) or cloud LLM (OpenRouter) that knows your transaction history
- **JWT Authentication** — Secure registration, login, and session management

---

## 2. Technology Stack

| Layer | Technology | Version |
|---|---|---|
| **Frontend** | React | 19.x |
| | Vite | 8.x |
| | React Router | 7.x |
| | Framer Motion | 12.x |
| | Lenis | 1.x |
| **Backend** | Java | 21 |
| | Spring Boot | 4.1.0 |
| | Spring Security | (managed by Boot) |
| | Spring Data JPA | (managed by Boot) |
| | SpringDoc OpenAPI | 2.8.4 |
| **Database** | PostgreSQL | 14+ |
| | H2 (tests) | (test scope) |
| **AI / LLM** | Ollama (local) | — |
| | OpenRouter (production) | — |
| | OkHttp (HTTP client) | 4.12.0 |
| **Auth** | JJWT | 0.12.6 |
| **File Parsing** | Apache POI (XLSX) | 5.4.1 |
| | OpenCSV (CSV) | 5.11.1 |
| **Build** | Maven Wrapper | — |
| | npm | — |

---

## 3. Architecture

### 3.1 High-Level Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                      Client Browser                          │
│  React SPA (Vite dev server :5173 / nginx prod)             │
│  ┌─────────┐ ┌──────────┐ ┌──────────┐ ┌───────────────┐   │
│  │  Auth   │ │Dashboard │ │  Goals   │ │  AI Coach     │   │
│  │  Page   │ │ /Analytics│ │  Page    │ │  Chat UI      │   │
│  └─────────┘ └──────────┘ └──────────┘ └───────────────┘   │
│                        │                                     │
│              ApiClient (JWT + localStorage)                  │
└────────────────────────┬────────────────────────────────────┘
                         │ HTTP (JSON)
                         │ /money_tracker/api/v1/*
┌────────────────────────▼────────────────────────────────────┐
│                   Spring Boot Backend                        │
│                   Port 8080, Context: /money_tracker         │
│                                                              │
│  ┌──────────────────────────────────────────────────────┐    │
│  │              JWT Authentication Filter                │    │
│  │         (OncePerRequestFilter → SecurityContext)      │    │
│  └──────────────────────────────────────────────────────┘    │
│                                                              │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────────┐   │
│  │  Auth    │ │Transact. │ │Analytics │ │     AI       │   │
│  │Controller│ │Controller│ │Controller│ │  Controller  │   │
│  └────┬─────┘ └────┬─────┘ └────┬─────┘ └──────┬───────┘   │
│       │            │            │               │            │
│  ┌────▼─────┐ ┌────▼─────┐ ┌────▼─────┐ ┌──────▼───────┐   │
│  │  Auth    │ │Transact. │ │Analytics │ │   AI Chat    │   │
│  │ Service  │ │ Service  │ │ Service  │ │   Service    │   │
│  └────┬─────┘ └────┬─────┘ └────┬─────┘ └──────┬───────┘   │
│       │            │            │               │            │
│  ┌────▼─────┐ ┌────▼─────┐     │        ┌──────▼───────┐   │
│  │  User    │ │  Trans.  │     │        │  AiLlmClient │   │
│  │  Repo    │ │  Repo    │     │        │  (Interface) │   │
│  └──────────┘ └──────────┘     │        └──────┬───────┘   │
│                                 │               │            │
│  ┌──────────┐ ┌──────────┐     │        ┌──────▼───────┐   │
│  │  Goal    │ │Notific.  │     │        │OllamaClient / │   │
│  │  Repo    │ │  Repo    │     │        │OpenRouter    │   │
│  └──────────┘ └──────────┘     │        └──────────────┘   │
│                                 │                            │
│  ┌──────────────────────────────▼────────────────────────┐  │
│  │           Spring Data JPA (Hibernate)                  │  │
│  └───────────────────────────────────────────────────────┘  │
└──────────────────────────┬───────────────────────────────────┘
                           │ JDBC
┌──────────────────────────▼───────────────────────────────────┐
│                      PostgreSQL                               │
│  Database: money_tracker                                      │
│  ┌──────────┐ ┌──────────────┐ ┌─────────────┐               │
│  │app_users │ │transactions  │ │spending_goals│               │
│  └──────────┘ └──────────────┘ └─────────────┘               │
│  ┌──────────┐ ┌──────────────┐ ┌─────────────┐               │
│  │categories│ │notifications │ │ai_chat_history│              │
│  └──────────┘ └──────────────┘ └─────────────┘               │
│  ┌──────────────┐                                              │
│  │ file_uploads │                                              │
│  └──────────────┘                                              │
└───────────────────────────────────────────────────────────────┘
```

### 3.2 Frontend Architecture

```
frontend/src/
├── main.jsx                    # React entry point
├── App.jsx                     # Root component (AuthProvider → AIChatProvider → Routes)
├── routes.jsx                  # Route definitions with ProtectedRoute guard
├── api/
│   ├── client.js               # Singleton ApiClient (JWT, fetch, 401 handling)
│   ├── authService.js          # Login, register, getProfile, logout
│   ├── transactionService.js   # Upload, list, summary
│   ├── analyticsService.js     # Summary, categories, trends, comparison, budget
│   ├── goalService.js          # CRUD + progress
│   ├── notificationService.js     # CRUD, mark read, count
│   └── aiService.js            # Chat, recommendations, health score, affordability
├── context/
│   ├── AuthContext.jsx         # Token + user state, login/register/logout
│   └── AIChatContext.jsx       # Chat messages + send
├── components/
│   ├── Dock.jsx                # Bottom navigation dock
│   ├── ProtectedRoute.jsx      # Redirects unauthenticated users to /auth
│   └── AIChat.jsx              # Chat panel UI
└── pages/
    ├── Auth.jsx                # Login / Register forms
    ├── Home.jsx                # Dashboard + statement upload
    ├── Analytics.jsx           # Charts and spending breakdown
    ├── Goals.jsx               # Goal CRUD + progress bars
    └── Coach.jsx               # AI chat interface
```

### 3.3 Backend Architecture

```
backend/src/main/java/com/tracker/MoneyTracker/
├── MoneyTrackerApplication.java   # Boot entry point
├── auth/
│   ├── AppUser.java               # User entity (UUID, username, password, email)
│   ├── AppUserDetailService.java  # Spring Security UserDetailsService
│   ├── UserRepository.java       # JPA repository
│   ├── UserService.java          # User registration logic
│   ├── UserRegistrationService.java # Registration with validation
│   ├── AuthController.java        # /register, /login, /logout, /users/{name}
│   ├── JwtUtil.java              # Token generation, validation, extraction
│   ├── JwtAuthenticationFilter.java # OncePerRequestFilter → SecurityContext
│   └── Constants.java            # Auth constants
├── transaction/
│   ├── Transaction.java           # Transaction entity
│   ├── TransactionRepository.java # JPA repository
│   ├── TransactionService.java    # Upload → parse → classify → persist
│   ├── TransactionController.java # /upload, /, /summary
│   ├── FileParser.java            # CSV/XLSX parsing (Apache POI + OpenCSV)
│   ├── CategoryClassifier.java    # Delegates to CategoryMapper
│   ├── CategoryMapper.java        # Rule-based keyword → category mapping
│   └── TransactionHashUtil.java   # SHA-256 hash for deduplication
├── analytics/
│   ├── AnalyticsService.java      # SQL aggregation queries → DTOs
│   ├── AnalyticsController.java   # /summary, /categories, /trends, /comparison, /budget
│   └── dto/
│       ├── SpendingSummary.java
│       ├── CategoryBreakdown.java
│       ├── MonthlyTrend.java
│       ├── SpendingComparison.java
│       ├── BudgetStatus.java
│       └── CategoryChange.java
├── goal/
│   ├── SpendingGoal.java          # Goal entity
│   ├── SpendingGoalRepository.java
│   ├── GoalService.java           # CRUD + progress calculation
│   ├── GoalController.java        # /goals CRUD + /progress
│   └── GoalProgress.java          # Progress DTO
├── notification/
│   ├── Notification.java          # Notification entity
│   ├── NotificationRepository.java
│   ├── NotificationService.java   # Create, mark read, count
│   └── NotificationController.java
├── ai/
│   ├── client/
│   │   └── AiLlmClient.java       # Interface (Strategy pattern)
│   ├── service/
│   │   ├── AiChatService.java         # Chat with LLM using transaction context
│   │   ├── RecommendationService.java # AI spending recommendations
│   │   ├── AffordabilityService.java  # Can-afford analysis
│   │   └── HealthScoreService.java    # Financial health score (0-100)
│   ├── prompt/
│   │   ├── ChatPromptBuilder.java
│   │   ├── RecommendationPromptBuilder.java
│   │   └── AffordabilityPromptBuilder.java
│   ├── dto/
│   │   ├── ChatRequest.java / ChatResponse.java
│   │   ├── RecommendationResponse.java
│   │   ├── HealthScoreResponse.java
│   │   └── AffordabilityRequest.java / AffordabilityResponse.java
│   ├── entity/
│   │   └── AiChatMessage.java
│   └── repository/
│       └── AiChatMessageRepository.java
├── configs/
│   ├── SecurityConfig.java        # Spring Security filter chain + CORS
│   └── AsyncConfig.java           # Async thread pool
├── error/
│   └── ErrorResponse.java        # Standard error DTO
└── exception/
    ├── BaseException.java         # Base custom exception
    ├── ResourceNotFoundException.java
    ├── DuplicateResourceException.java
    ├── BadRequestException.java
    ├── UnauthorizedException.java
    ├── ForbiddenException.java
    └── InternalServerException.java
```

---

## 4. Design Patterns

### 4.1 Strategy Pattern — AI LLM Client

The `AiLlmClient` interface decouples the application from the underlying LLM provider:

```java
public interface AiLlmClient {
    String generate(String prompt);
}
```

Two implementations exist:
- **`OllamaClient`** — connects to a local Ollama instance (dev/test)
- **`GroqClient`** — connects to OpenRouter (production)

The active implementation is selected at runtime via the `ai.provider` property in `application.yaml`. Services depend only on the interface, making it trivial to add new providers.

### 4.2 Template Method — Prompt Builders

The AI module uses dedicated prompt builders (`ChatPromptBuilder`, `RecommendationPromptBuilder`, `AffordabilityPromptBuilder`) that encapsulate the logic for constructing context-rich prompts. Each builder injects the user's transaction data, goals, and analytics into a structured prompt before sending to the LLM.

### 4.3 Layered Architecture

Each module follows a consistent three-layer pattern:

```
Controller (HTTP) → Service (business logic) → Repository (data access)
```

Controllers handle request/response. Services contain business rules. Repositories abstract persistence. Entities and DTOs are never leaked across layers inappropriately.

### 4.4 Filter Chain — JWT Authentication

`JwtAuthenticationFilter` extends Spring's `OncePerRequestFilter` and intercepts every request to extract and validate the JWT from the `Authorization` header. Valid tokens populate the `SecurityContextHolder`, enabling method-level security annotations.

### 4.5 Global Exception Handler

A `@ControllerAdvice` class catches all custom exceptions (`BadRequestException`, `ResourceNotFoundException`, `DuplicateResourceException`, etc.) and maps them to consistent `ErrorResponse` objects with appropriate HTTP status codes.

### 4.6 Singleton API Client (Frontend)

The `ApiClient` class uses a private `#token` field and a module-level singleton instance. It automatically attaches the JWT to every request, handles 401 responses by dispatching a custom `auth:unauthorized` event, and persists the token to `localStorage`.

### 4.7 Context-Based State Management (Frontend)

React Context provides global state for authentication (`AuthContext`) and AI chat (`AIChatContext`). The `AuthContext` listens for the `auth:unauthorized` window event from the API client to trigger automatic logout.

### 4.8 Repository Pattern

All database access uses Spring Data JPA repository interfaces. Custom query methods (e.g., `findByUserIdAndCategory`, `findByUserIdOrderByCreatedAtDesc`) leverage method-name query derivation. Complex analytics use `@Query` with native SQL aggregations.

---

## 5. Database Design

### 5.1 Schema

| Table | Purpose |
|---|---|
| `app_users` | User accounts (UUID PK, unique username/email) |
| `categories` | 14 predefined spending categories (seeded on first run) |
| `user_transactions` | Parsed transactions with hash-based deduplication |
| `spending_goals` | User-defined category budgets with time periods |
| `file_uploads` | Upload tracking (status: PENDING → PROCESSING → COMPLETED/FAILED) |
| `notifications` | Goal warnings, budget tips, spending insights |
| `ai_chat_history` | Conversation between user and AI coach |

### 5.2 Design Decisions

- **Single transactions table** — no partitioning or sharding needed at MVP scale. PostgreSQL handles 10M+ rows with proper indexing.
- **Transaction hashing** — SHA-256 of (date, amount, type, original_detail) prevents duplicate imports. A unique constraint on `(user_id, transaction_hash)` enforces this.
- **Normalized categories** — categories are a separate table with foreign keys, enabling efficient filtering and reporting.
- **UUID primary keys** — all entities use UUIDs, avoiding enumeration attacks and supporting distributed systems in the future.
- **Composite indexes** — indexes on `(user_id, transaction_date)`, `(user_id, category)`, and `(user_id, category, transaction_date)` optimize the most common query patterns.

### 5.3 Index Strategy

| Index | Use Case |
|---|---|
| `idx_tx_user_date` | Dashboard: transactions for a user in date range |
| `idx_tx_user_category` | Category filtering |
| `idx_tx_user_cat_date` | Analytics: category breakdown over time |
| `idx_tx_date` | Recent transactions across all users |
| `idx_goals_user_cat` | Active goals by user and category |
| `idx_notifications_user_read` | Unread notification count |

---

## 6. Local Development Configuration

### 6.1 Backend (`application-dev.yaml`)

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/money_tracker
    username: postgres
    password: 1234
  jpa:
    hibernate:
      ddl-auto: update    # Auto-creates tables from @Entity classes
    show-sql: true

jwt:
  secret: <dev-fallback-base64>
  expiration: 86400000    # 24 hours

ollama:
  base-url: http://localhost:11434
  model: llama3.2
  temperature: 0.7
  max-tokens: 512

server:
  port: 8080
  servlet:
    context-path: /money_tracker
```

### 6.2 Frontend (`.env.local`)

```
VITE_API_URL=http://localhost:8080/money_tracker/api/v1
```

The Vite dev server (`vite.config.js`) proxies `/money_tracker/api` → `http://localhost:8080`, so the frontend can use relative paths in API calls.

### 6.3 Starting Local Dev

```bash
# Terminal 1 — Backend
cd backend
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev

# Terminal 2 — Frontend
cd frontend
npm run dev

# Terminal 3 — Ollama (optional, for AI features)
ollama serve
ollama pull llama3.2
```

Access:
- Frontend: `http://localhost:5173`
- Backend API: `http://localhost:8080/money_tracker/api/v1`
- Swagger UI: `http://localhost:8080/money_tracker/swagger-ui/index.html`

---

## 7. Production Configuration

### 7.1 Backend (`application-prod.yaml`)

```yaml
spring:
  datasource:
    url: ${DATABASE_URL}        # Railway PostgreSQL
    username: ${PGUSER}
    password: ${PGPASSWORD}
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: false

ai:
  provider: openrouter

openrouter:
  api-key: ${OPENROUTER_API_KEY}
  base-url: https://openrouter.ai/api/v1
  model: openai/gpt-oss-120b:free
  temperature: 0.7
  max-tokens: 512

cors:
  allowed-origins: ${CORS_ALLOWED_ORIGINS}
  allowed-origin-patterns: "*"
```

### 7.2 Frontend (`.env.production`)

No `VITE_API_URL` is set — the frontend uses relative paths. In production, nginx serves the built SPA and proxies `/money_tracker/api` to the backend on the same origin.

### 7.3 Production Deployment (Railway)

- **Backend**: Built as a fat JAR via `./mvnw clean package`, deployed with the PostgreSQL plugin
- **Frontend**: Built via `npm run build`, static files served by nginx
- **AI**: Uses OpenRouter free tier (no local Ollama needed)
- **CORS**: Configured to allow the Railway frontend domain

---

## 8. Testing Report

### 8.1 Testing Philosophy

- **Unit tests** verify individual components in isolation (services, utilities, mappers)
- **Integration tests** verify the full stack end-to-end (controllers → services → repositories → database) using real code with no mocking
- Integration tests use **H2 in-memory database** for fast execution
- AI integration tests require a running **Ollama** instance

### 8.2 Test Configuration

Integration tests are gated behind a Maven profile:

```xml
<!-- Default: exclude integration tests -->
<excludes>
    <exclude>**/integration/**</exclude>
</excludes>

<!-- Activated with -Drun.integration.tests=true -->
<profile>
    <id>integration-tests</id>
    <activation>
        <property>
            <name>run.integration.tests</name>
            <value>true</value>
        </property>
    </activation>
</profile>
```

Test properties (`application-test.yaml`):
- H2 in-memory database with `create-drop` (fresh schema per test class)
- Same JWT secret and Ollama config as dev

### 8.3 Test Coverage Summary

| Test Class | Type | Tests | Coverage Area |
|---|---|---|---|
| `DatabaseIntegrationTest` | Integration | 27 | All JPA repositories (User, Transaction, Goal, Notification) |
| `AuthFlowIntegrationTest` | Integration | 15 | Register, login, protected endpoints, logout, full lifecycle |
| `ApplicationFlowIntegrationTest` | Integration | 15 | Upload, goals CRUD, notifications, analytics, end-to-end journey |
| `AiIntegrationTest` | Integration | 17 | Chat, recommendations, health score, affordability |
| `JwtUtilTest` | Unit | 8 | Token generation, validation, expiration |
| `FileParserTest` | Unit | 6 | CSV/XLSX parsing edge cases |
| `CategoryClassifierTest` | Unit | 12 | Category classification accuracy |
| `CategoryMapperTest` | Unit | 14 | Keyword-to-category mapping |
| `AnalyticsServiceTest` | Unit | 10 | Aggregation logic |
| `GoalServiceTest` | Unit | 8 | Goal CRUD and progress calculation |
| `NotificationServiceTest` | Unit | 6 | Notification lifecycle |
| `HealthScoreServiceTest` | Unit | 5 | Health score computation |
| `TransactionServiceTest` | Unit | 9 | Upload and deduplication logic |
| `TransactionControllerTest` | Unit | 7 | Controller request handling |
| `GoalControllerTest` | Unit | 6 | Controller request handling |
| `NotificationControllerTest` | Unit | 5 | Controller request handling |
| `GlobalExceptionHandlerTest` | Unit | 8 | Error response formatting |
| `ExceptionTest` | Unit | 4 | Custom exception hierarchy |
| `OllamaClientTest` | Unit | 3 | LLM client error handling |
| **Total** | | **~170** | |

### 8.4 How to Run

```bash
# Unit tests only (fast, no external dependencies)
cd backend && ./mvnw test

# Integration tests (requires Ollama)
cd backend && ./mvnw test -Drun.integration.tests=true

# Specific integration test class
./mvnw test -Drun.integration.tests=true -Dtest="AuthFlowIntegrationTest"

# All tests (unit + integration)
./mvnw test -Drun.integration.tests=true -Dtest="com.tracker.MoneyTracker.**"
```

### 8.5 Expected Test Execution Times

| Test Class | Approximate Time |
|---|---|
| `DatabaseIntegrationTest` | ~12 seconds |
| `AuthFlowIntegrationTest` | ~37 seconds |
| `ApplicationFlowIntegrationTest` | ~19 seconds |
| `AiIntegrationTest` | ~3-5 minutes (depends on LLM) |
| All unit tests | ~2 minutes |

---

## 9. API Design

### 9.1 Authentication

All endpoints except `/auth/register` and `/auth/login` require a `Bearer` token in the `Authorization` header.

### 9.2 Context Path

All routes are prefixed with `/money_tracker` (configured via `server.servlet.context-path`).

### 9.3 Versioning

API is versioned via URL path: `/api/v1/`. Breaking changes introduce `/api/v2/`.

### 9.4 Error Responses

All errors return a consistent JSON structure:

```json
{
  "timestamp": "2026-06-24T10:30:00",
  "status": 400,
  "message": "Invalid request: userId is required",
  "path": "/money_tracker/api/v1/transaction"
}
```

### 9.5 CORS

- **Dev**: Vite proxy handles CORS (same-origin requests)
- **Prod**: Configured via `cors.allowed-origins` in `application-prod.yaml`

---

## 10. Security Considerations

- **JWT tokens** are signed with a configurable secret (HS256) and expire after 24 hours
- **Passwords** are encoded with BCrypt (via Spring Security)
- **CORS** is restricted to known origins in production
- **Input validation** uses `@Valid` annotations with custom messages
- **Transaction deduplication** prevents data inflation from repeated uploads
- **No sensitive data** in logs (SQL logging disabled in production)

---

## 11. Future Enhancements

- PDF bank statement parsing (currently CSV/XLSX only)
- Recurring transaction detection
- Multi-currency support
- Export reports (PDF/CSV)
- Mobile app (React Native)
- Plaid/OpenBanking integration for automatic transaction sync
- More LLM providers (Anthropic, Google) via the Strategy pattern
- WebSocket-based real-time notifications
- Dark mode UI

---

## 12. Project Structure (Complete)

```
Money_Tracker/
├── README.md                          # Quick start guide
├── PROJECT_SUMMARY.md                 # This file (detailed documentation)
├── .gitignore
├── backend/
│   ├── pom.xml                        # Maven build config
│   ├── mvnw / mvnw.cmd                # Maven wrapper scripts
│   ├── MoneyTracker.iml               # IntelliJ module file
│   ├── database.md                    # Database design rationale
│   ├── INTEGRATION_TESTS.md           # Integration test documentation
│   ├── MoneyTracker.postman_collection.json
│   ├── HELP.md                        # Spring Boot Maven plugin reference
│   └── src/
│       ├── main/
│       │   ├── java/com/tracker/MoneyTracker/
│       │   │   ├── MoneyTrackerApplication.java
│       │   │   ├── auth/
│       │   │   │   ├── AppUser.java
│       │   │   │   ├── AppUserDetailService.java
│       │   │   │   ├── Constants.java
│       │   │   │   ├── JwtAuthenticationFilter.java
│       │   │   │   ├── JwtUtil.java
│       │   │   │   ├── UserRepository.java
│       │   │   │   ├── UserService.java
│       │   │   │   ├── UserRegistrationService.java
│       │   │   │   └── AuthController.java
│       │   │   ├── transaction/
│       │   │   │   ├── CategoryClassifier.java
│       │   │   │   ├── CategoryMapper.java
│       │   │   │   ├── FileParser.java
│       │   │   │   ├── Transaction.java
│       │   │   │   ├── TransactionController.java
│       │   │   │   ├── TransactionHashUtil.java
│       │   │   │   ├── TransactionRepository.java
│       │   │   │   └── TransactionService.java
│       │   │   ├── analytics/
│       │   │   │   ├── AnalyticsController.java
│       │   │   │   ├── AnalyticsService.java
│       │   │   │   └── dto/ (5 DTOs)
│       │   │   ├── goal/
│       │   │   │   ├── GoalController.java
│       │   │   │   ├── GoalProgress.java
│       │   │   │   ├── GoalService.java
│       │   │   │   ├── SpendingGoal.java
│       │   │   │   └── SpendingGoalRepository.java
│       │   │   ├── notification/
│       │   │   │   ├── Notification.java
│       │   │   │   ├── NotificationController.java
│       │   │   │   ├── NotificationRepository.java
│       │   │   │   └── NotificationService.java
│       │   │   ├── ai/
│       │   │   │   ├── client/AiLlmClient.java
│       │   │   │   ├── service/ (4 services)
│       │   │   │   ├── prompt/ (3 builders)
│       │   │   │   ├── dto/ (6 DTOs)
│       │   │   │   ├── entity/AiChatMessage.java
│       │   │   │   └── repository/AiChatMessageRepository.java
│       │   │   ├── configs/
│       │   │   │   ├── AsyncConfig.java
│       │   │   │   └── SecurityConfig.java
│       │   │   ├── error/
│       │   │   │   └── ErrorResponse.java
│       │   │   └── exception/
│       │   │       ├── BaseException.java
│       │   │       ├── BadRequestException.java
│       │   │       ├── DuplicateResourceException.java
│       │   │       ├── ForbiddenException.java
│       │   │       ├── InternalServerException.java
│       │   │       ├── ResourceNotFoundException.java
│       │   │       └── UnauthorizedException.java
│       │   └── resources/
│       │       ├── application.yaml
│       │       ├── application-dev.yaml
│       │       ├── application-prod.yaml
│       │       └── sql/schema.sql
│       └── test/
│           ├── java/com/tracker/MoneyTracker/
│           │   ├── ai/client/OllamaClientTest.java
│           │   ├── ai/service/HealthScoreServiceTest.java
│           │   ├── analytics/AnalyticsServiceTest.java
│           │   ├── analytics/AnalyticsControllerTest.java
│           │   ├── auth/AuthControllerTest.java
│           │   ├── auth/JwtUtilTest.java
│           │   ├── error/GlobalExceptionHandlerTest.java
│           │   ├── exception/ExceptionTest.java
│           │   ├── goal/GoalControllerTest.java
│           │   ├── goal/GoalServiceTest.java
│           │   ├── integration/
│           │   │   ├── AiIntegrationTest.java
│           │   │   ├── ApplicationFlowIntegrationTest.java
│           │   │   ├── AuthFlowIntegrationTest.java
│           │   │   └── DatabaseIntegrationTest.java
│           │   ├── notification/NotificationControllerTest.java
│           │   ├── notification/NotificationServiceTest.java
│           │   └── transaction/
│           │       ├── CategoryClassifierTest.java
│           │       ├── CategoryMapperTest.java
│           │       ├── FileParserTest.java
│           │       ├── TransactionControllerTest.java
│           │       └── TransactionServiceTest.java
│           └── resources/
│               ├── application-test.yaml
│               ├── schema.sql
│               └── test-statement.xlsx
├── frontend/
│   ├── package.json
│   ├── vite.config.js
│   ├── eslint.config.js
│   ├── .env.local
│   ├── .env.production
│   ├── README.md
│   ├── public/ (favicon, icons)
│   ├── dist/ (build output)
│   └── src/
│       ├── main.jsx
│       ├── App.jsx
│       ├── App.css
│       ├── routes.jsx
│       ├── index.css
│       ├── api/ (7 service files)
│       ├── components/ (Dock, ProtectedRoute, AIChat)
│       ├── context/ (AuthContext, AIChatContext)
│       └── pages/ (Home, Analytics, Goals, Coach, Auth)
└── .idea/ (IntelliJ config)
```
