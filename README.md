# Money Tracker

An AI-powered personal finance application that ingests bank statements (CSV/XLSX), categorizes transactions automatically, provides spending analytics, and coaches users toward their financial goals with the help of a local LLM.

---

## Table of Contents

- [Overview](#overview)
- [Tech Stack](#tech-stack)
- [Architecture](#architecture)
- [Prerequisites](#prerequisites)
- [Running Locally](#running-locally)
- [API Endpoints](#api-endpoints)
- [Environment Variables](#environment-variables)
- [Testing](#testing)
- [Production Deployment](#production-deployment)

---

## Overview

Money Tracker parses uploaded bank statements, classifies each transaction into categories (Food, Shopping, Transport, etc.), and surfaces insights through:

- **Analytics** — income vs. expense, category breakdowns, monthly trends, period comparisons
- **Spending Goals** — set category-level budgets and track progress
- **AI Coach** — chat with an LLM (Ollama / OpenRouter) that knows your transaction history, get personalized recommendations, a financial health score, and affordability checks
- **Notifications** — alerts when approaching or exceeding goal limits

---

## Tech Stack

| Layer | Technology |
|---|---|
| **Frontend** | React 19, Vite 8, React Router 7, Framer Motion, Lenis |
| **Backend** | Java 21, Spring Boot 4.1, Spring Security, Spring Data JPA |
| **Database** | PostgreSQL (prod / local), H2 (tests) |
| **AI / LLM** | Ollama (local), OpenRouter (production) |
| **Auth** | JWT (jjwt 0.12) |
| **File Parsing** | Apache POI (XLSX), OpenCSV (CSV) |
| **API Docs** | SpringDoc OpenAPI (Swagger UI) |
| **Build** | Maven wrapper (`mvnw`), npm |

---

## Architecture

```
┌─────────────────────────────────────────────────────────┐
│                     Frontend (Vite)                      │
│  React SPA — Auth, Dashboard, Analytics, Goals, Coach   │
│  Port: 5173 (dev)                                       │
└──────────────────────┬──────────────────────────────────┘
                       │  /money_tracker/api/v1/*
                       │  (Vite proxy in dev, nginx in prod)
┌──────────────────────▼──────────────────────────────────┐
│                   Backend (Spring Boot)                   │
│  Port: 8080, Context-Path: /money_tracker               │
│                                                         │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌───────────┐  │
│  │   Auth   │ │Transact. │ │Analytics │ │    AI     │  │
│  │  Module  │ │  Module  │ │  Module  │ │  Module   │  │
│  └──────────┘ └──────────┘ └──────────┘ └───────────┘  │
│  ┌──────────┐ ┌──────────┐                               │
│  │  Goals   │ │Notific.  │   ← Service → Repository     │
│  └──────────┘ └──────────┘     (Spring Data JPA)        │
└──────────────────────┬──────────────────────────────────┘
                       │
┌──────────────────────▼──────────────────────────────────┐
│                    PostgreSQL                             │
│  money_tracker database                                  │
│  Tables: app_users, categories, user_transactions,      │
│          spending_goals, file_uploads, notifications,     │
│          ai_chat_history                                 │
└─────────────────────────────────────────────────────────┘
```

### Backend Design Patterns

| Pattern | Where Used | Purpose |
|---|---|---|
| **Layered Architecture** | Controllers → Services → Repositories | Separation of concerns |
| **Strategy** | `AiLlmClient` interface (Ollama / OpenRouter) | Swap LLM provider via config |
| **Template Method** | Prompt builders (`ChatPromptBuilder`, `RecommendationPromptBuilder`, `AffordabilityPromptBuilder`) | Reusable prompt construction |
| **Repository** | All data access via Spring Data JPA | Abstract persistence |
| **DTO** | Analytics, AI responses | Decouple API contract from entities |
| **Filter Chain** | `JwtAuthenticationFilter` extends `OncePerRequestFilter` | JWT interception in Spring Security chain |
| **Global Exception Handler** | `ControllerAdvice` + custom exceptions | Consistent error responses |
| **Dependency Injection** | Constructor injection throughout | Loose coupling, testability |

### Frontend Architecture

| Concern | Implementation |
|---|---|
| **Routing** | React Router v7 with `<ProtectedRoute>` guard |
| **State** | React Context (`AuthContext`, `AIChatContext`) |
| **API Layer** | Singleton `ApiClient` class with JWT auto-attach + 401 auto-logout |
| **Auth Persistence** | JWT stored in `localStorage`, restored on mount |
| **Proxy** | Vite dev server proxies `/money_tracker/api` → `localhost:8080` |

---

## Prerequisites

| Requirement | Version | Notes |
|---|---|---|
| **Java** | 21+ | JDK (Eclipse Temurin, Corretto, etc.) |
| **Node.js** | 18+ | For frontend npm |
| **PostgreSQL** | 14+ | Local database named `money_tracker` |
| **Maven** | — | Use included `mvnw` wrapper |
| **Ollama** | — | Only needed for AI features (local mode) |

---

## Running Locally

### 1. Clone the Repository

```bash
git clone <repo-url>
cd Money_Tracker
```

### 2. Database Setup

Ensure PostgreSQL is running and create the database:

```sql
CREATE DATABASE money_tracker;
```

The schema is auto-created by Hibernate (`ddl-auto: update`) on first run. The default dev credentials are in `application-dev.yaml`:

- **URL**: `jdbc:postgresql://localhost:5432/money_tracker`
- **Username**: `postgres`
- **Password**: `1234`

### 3. Backend

From the `backend/` directory:

```bash
# Run the Spring Boot app
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

The API will be available at `http://localhost:8080/money_tracker/api/v1`

Swagger UI: `http://localhost:8080/money_tracker/swagger-ui/index.html`

### 4. Frontend

From the `frontend/` directory:

```bash
# Install dependencies (already done if node_modules exists)
npm install

# Start the Vite dev server
npm run dev
```

The app will be available at `http://localhost:5173`

Vite proxies `/money_tracker/api` requests to the backend on port 8080 automatically.

### 5. Ollama (Optional — for AI features)

```bash
# Pull the model
ollama pull llama3.2

# Ollama runs on http://localhost:11434 by default
```

The backend connects to Ollama automatically when running with the `dev` profile.

---

## API Endpoints

All endpoints are prefixed with `/money_tracker` (context-path) and then `/api/v1`.

| Group | Method | Endpoint | Auth | Description |
|---|---|---|---|---|
| **Auth** | POST | `/api/v1/auth/register` | Public | Register new user |
| | POST | `/api/v1/auth/login` | Public | Login, returns JWT |
| | POST | `/api/v1/auth/logout` | Public | Clear security context |
| | DELETE | `/api/v1/auth/users/{username}` | Public | Delete user |
| **Transactions** | POST | `/api/v1/transaction/upload` | Required | Upload CSV/XLSX statement |
| | GET | `/api/v1/transaction?userId={id}` | Required | List transactions |
| | GET | `/api/v1/transaction/summary?userId={id}` | Required | Category-wise summary |
| **Goals** | POST | `/api/v1/goals` | Required | Create spending goal |
| | GET | `/api/v1/goals?userId={id}` | Required | List all goals |
| | GET | `/api/v1/goals/active?userId={id}` | Required | List active goals |
| | PUT | `/api/v1/goals/{goalId}` | Required | Update goal |
| | DELETE | `/api/v1/goals/{goalId}` | Required | Delete goal |
| | GET | `/api/v1/goals/{goalId}/progress` | Required | Get goal progress % |
| **Notifications** | POST | `/api/v1/notifications` | Required | Create notification |
| | GET | `/api/v1/notifications?userId={id}` | Required | List (newest first) |
| | GET | `/api/v1/notifications/unread?userId={id}` | Required | Unread only |
| | GET | `/api/v1/notifications/unread/count?userId={id}` | Required | Count unread |
| | PUT | `/api/v1/notifications/{id}/read` | Required | Mark as read |
| | PUT | `/api/v1/notifications/read-all?userId={id}` | Required | Mark all read |
| | DELETE | `/api/v1/notifications/{id}` | Required | Delete notification |
| **Analytics** | GET | `/api/v1/analytics/summary?userId={id}` | Required | Income/expense/net |
| | GET | `/api/v1/analytics/categories?userId={id}` | Required | Category breakdown |
| | GET | `/api/v1/analytics/trends?userId={id}&periods=6` | Required | Monthly trends |
| | GET | `/api/v1/analytics/comparison?userId={id}` | Required | Period comparison |
| | GET | `/api/v1/analytics/budget?userId={id}` | Required | Budget status |
| **AI Coach** | POST | `/api/v1/ai/chat` | Required | Chat with AI coach |
| | GET | `/api/v1/ai/recommendations?userId={id}` | Required | Spending recommendations |
| | GET | `/api/v1/ai/health-score?userId={id}` | Required | Financial health (0-100) |
| | POST | `/api/v1/ai/affordability` | Required | Can afford a purchase? |

---

## Environment Variables

### Backend (`application-dev.yaml` — local dev)

| Variable | Default | Description |
|---|---|---|
| `DATABASE_URL` | `jdbc:postgresql://localhost:5432/money_tracker` | JDBC connection string |
| `PGUSER` | `postgres` | Database user |
| `PGPASSWORD` | `1234` | Database password |
| `JWT_SECRET` | (dev fallback) | Base64-encoded JWT signing key |
| `OLLAMA_BASE_URL` | `http://localhost:11434` | Ollama server URL |
| `OLLAMA_MODEL` | `llama3.2` | LLM model name |

### Backend (`application-prod.yaml` — production)

| Variable | Source | Description |
|---|---|---|
| `DATABASE_URL` | Railway env | PostgreSQL connection string |
| `PGUSER` | Railway env | Database user |
| `PGPASSWORD` | Railway env | Database password |
| `JWT_SECRET` | Railway env | JWT signing key |
| `OPENROUTER_API_KEY` | Railway env | OpenRouter API key |
| `CORS_ALLOWED_ORIGINS` | Railway env | Allowed CORS origins |

### Frontend (`.env.local` — local dev)

| Variable | Value | Description |
|---|---|---|
| `VITE_API_URL` | `http://localhost:8080/money_tracker/api/v1` | Backend API base URL |

In production, the frontend uses relative paths (same-origin via nginx proxy).

---

## Testing

### Unit Tests (default)

```bash
cd backend
./mvnw test
```

Runs all unit tests (excludes integration tests by default).

### Integration Tests

Requires Ollama running on `localhost:11434` with `llama3.2` model loaded.

```bash
cd backend
./mvnw test -Drun.integration.tests=true
```

### Run a Specific Integration Test Class

```bash
./mvnw test -Drun.integration.tests=true -Dtest="DatabaseIntegrationTest"
./mvnw test -Drun.integration.tests=true -Dtest="AuthFlowIntegrationTest"
./mvnw test -Drun.integration.tests=true -Dtest="ApplicationFlowIntegrationTest"
./mvnw test -Drun.integration.tests=true -Dtest="AiIntegrationTest"
```

### Test Summary

| Test Class | Tests | What It Covers |
|---|---|---|
| `DatabaseIntegrationTest` | 27 | JPA persistence against H2 |
| `AuthFlowIntegrationTest` | 15 | Full auth lifecycle via HTTP |
| `ApplicationFlowIntegrationTest` | 15 | Upload → Goals → Analytics → Notifications |
| `AiIntegrationTest` | 17 | AI chat, recommendations, health score, affordability |
| **Total Integration** | **74** | |

Plus unit tests for `JwtUtil`, `FileParser`, `CategoryClassifier`, `CategoryMapper`, `AnalyticsService`, `GoalService`, `NotificationService`, `HealthScoreService`, `TransactionService`, `TransactionController`, `GoalController`, `NotificationController`, `GlobalExceptionHandler`, and exception classes.

---

## Production Deployment

The application is deployed on **Railway**:

- **Backend**: Spring Boot JAR with PostgreSQL plugin
- **Frontend**: Static build served via nginx, which proxies `/money_tracker/api` to the backend
- **AI**: OpenRouter (free tier) for production LLM inference

### Build Commands

```bash
# Backend
cd backend
./mvnw clean package -DskipTests

# Frontend
cd frontend
npm run build
# Output in frontend/dist/
```

---

## Project Structure

```
Money_Tracker/
├── README.md                  # This file
├── PROJECT_SUMMARY.md         # Detailed architecture & design document
├── backend/
│   ├── pom.xml
│   ├── mvnw / mvnw.cmd
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/tracker/MoneyTracker/
│   │   │   │   ├── MoneyTrackerApplication.java
│   │   │   │   ├── auth/         # JWT auth (filter, service, controller, repo)
│   │   │   │   ├── transaction/  # Upload, parse, classify, summarize
│   │   │   │   ├── analytics/    # Spending summary, trends, breakdowns
│   │   │   │   ├── goal/         # Spending goals + progress tracking
│   │   │   │   ├── notification/    # Alerts and goal warnings
│   │   │   │   ├── ai/           # LLM client, prompts, services
│   │   │   │   ├── configs/      # Security, async config
│   │   │   │   ├── error/        # Global exception handler
│   │   │   │   └── exception/    # Custom exceptions
│   │   │   └── resources/
│   │   │       ├── application.yaml
│   │   │       ├── application-dev.yaml
│   │   │       ├── application-prod.yaml
│   │   │       └── sql/schema.sql
│   │   └── test/                 # Unit + integration tests
│   └── database.md               # Database design rationale
├── frontend/
│   ├── package.json
│   ├── vite.config.js
│   ├── .env.local
│   ├── .env.production
│   └── src/
│       ├── main.jsx
│       ├── App.jsx
│       ├── routes.jsx
│       ├── api/                  # Service modules (client, auth, transaction, etc.)
│       ├── components/           # Dock, ProtectedRoute, AIChat
│       ├── context/              # AuthContext, AIChatContext
│       └── pages/                # Home, Analytics, Goals, Coach, Auth
└── .gitignore
```
