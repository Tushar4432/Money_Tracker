# Money Tracker — Integration Tests

## Overview

This document describes the integration test suite for the Money Tracker backend application.
Integration tests use **real services** (no mocking) against an **H2 in-memory database** and a
running **Ollama** instance for AI features.

> **Philosophy**: Integration tests verify that the full stack works end-to-end — controllers,
> services, repositories, and database — all working together with real code.

---

## Table of Contents

- [Prerequisites](#prerequisites)
- [Test Files](#test-files)
- [How to Run](#how-to-run)
- [Test Details](#test-details)
  - [Database Integration Tests](#1-database-integration-tests)
  - [Auth Flow Integration Tests](#2-auth-flow-integration-tests)
  - [Application Flow Integration Tests](#3-application-flow-integration-tests)
  - [AI Integration Tests](#4-ai-integration-tests)
- [Configuration](#configuration)
- [API Endpoints Reference](#api-endpoints-reference)
- [Troubleshooting](#troubleshooting)

---

## Prerequisites

| Requirement | Details |
|---|---|
| **Java 21** | JDK 21 or later |
| **Maven** | Via `mvnw` wrapper (included in project) |
| **Ollama** | Required only for AI tests. Must be running on `http://localhost:11434` with the `llama3.2` model loaded. See [Ollama setup](#ollama-setup) below. |
| **Port 8080+** | Tests use random ports, but Ollama needs `localhost:11434` |

### Ollama Setup

```bash
# Start Ollama in Docker
docker run -d --name ollama -p 11434:11434 ollama/ollama

# Pull the required model
curl http://localhost:11434/api/pull -d '{"name":"llama3.2"}'

# Verify the model is loaded
curl http://localhost:11434/api/tags
```

---

## Test Files

| File | Location | Tests | Description |
|---|---|---|---|
| `DatabaseIntegrationTest.java` | `src/test/java/.../integration/` | 27 | Database persistence with real repositories |
| `AuthFlowIntegrationTest.java` | `src/test/java/.../integration/` | 15 | Full auth lifecycle (register/login/access/logout) |
| `ApplicationFlowIntegrationTest.java` | `src/test/java/.../integration/` | 15 | End-to-end app flow (upload/goals/analytics/notifications) |
| `AiIntegrationTest.java` | `src/test/java/.../integration/` | 17 | AI coach with real Ollama (chat/recommendations/health/affordability) |
| **Total** | | **74** | |

**Supporting files:**

| File | Purpose |
|---|---|
| `src/test/resources/application-test.yaml` | H2 database + Ollama config for tests |
| `src/test/resources/schema.sql` | H2-compatible database schema |
| `src/test/resources/test-statement.xlsx` | Sample bank statement for file parser tests |

---

## How to Run

### Run Only Unit Tests (Default)

Integration tests are **excluded by default** to keep the build fast:

```bash
./mvnw test
```

### Run Only Integration Tests

Requires Ollama running on `localhost:11434` with the `llama3.2` model:

```bash
./mvnw test -Drun.integration.tests=true
```

### Run a Specific Integration Test Class

```bash
./mvnw test -Drun.integration.tests=true -Dtest="DatabaseIntegrationTest"
./mvnw test -Drun.integration.tests=true -Dtest="AuthFlowIntegrationTest"
./mvnw test -Drun.integration.tests=true -Dtest="ApplicationFlowIntegrationTest"
./mvnw test -Drun.integration.tests=true -Dtest="AiIntegrationTest"
```

### Run All Tests (Unit + Integration)

```bash
./mvnw test -Drun.integration.tests=true -Dtest="com.tracker.MoneyTracker.**"
```

### Run Unit Tests Only (Excluding Integration Tests Explicitly)

```bash
./mvnw test -Dtest='!com.tracker.MoneyTracker.integration.*'
```

---

## Test Details

### 1. Database Integration Tests

**Class**: `DatabaseIntegrationTest.java`
**Tests**: 27
**Uses**: Real Spring Data JPA repositories against H2 in-memory database

Verifies that data is actually persisted to and retrieved from the database.

#### UserRepository Tests (8 tests)

| Test | What it verifies |
|---|---|
| `shouldPersistAndRetrieveUser_ByUuid` | Save user → find by UUID → data matches |
| `shouldFindUser_ByUsername` | findByUsername returns correct user |
| `shouldFindUser_ByEmail` | findByEmail returns correct user |
| `shouldReturnEmpty_WhenUserNotFound` | Missing users return `Optional.empty()` |
| `shouldPersistMultipleUsers` | Save 3 users → `findAll()` returns 3 |
| `shouldDeleteUser_AndVerifyRemoval` | Delete user → `findById` returns empty |
| `shouldVerifyUniqueUsername` | Schema defines unique username constraint |
| `shouldVerifyUniqueEmail` | Schema defines unique email constraint |

#### TransactionRepository Tests (7 tests)

| Test | What it verifies |
|---|---|
| `shouldPersistAndRetrieveTransaction` | Save → findById → all fields match |
| `shouldFindAllTransactions_ByUser` | Save 3 for same user → `findByUserId` returns 3 |
| `shouldFindTransactions_WithinDateRange` | Save across months → date range query filters correctly |
| `shouldReturnEmptyList_WhenNoTransactions` | No transactions → empty list |
| `shouldDeleteUser_AndVerifyRemoved` | Delete user → user removed (cascade at DB level) |
| `shouldPersistTransaction_WithAllFields` | All fields including `sentTo`, `originalDetail` |
| `shouldSaveAndRetrieveMultipleTransactions` | Batch save 5 → all retrievable |

#### SpendingGoalRepository Tests (5 tests)

| Test | What it verifies |
|---|---|
| `shouldPersistAndRetrieveGoal` | Save → findById → target, category, period match |
| `shouldFindAllGoals_ForUser` | Save 3 goals → `findByUserId` returns 3 |
| `shouldFindOnlyActiveGoals_ForUser` | Mix of active/inactive → `findByUserIdAndActive` filters |
| `shouldFindGoals_ByUserAndCategory` | `findByUserIdAndCategory` returns correct subset |
| `shouldDeleteUser_GoalCascade` | Delete user → user removed (cascade at DB level) |

#### NotificationRepository Tests (5 tests)

| Test | What it verifies |
|---|---|
| `shouldPersistAndRetrieveNotification` | Save → findById → type, title, read status match |
| `shouldFindNotifications_ByUserOrderByCreatedAtDesc` | `findByUserIdOrderByCreatedAtDesc` returns newest first |
| `shouldFindOnlyUnreadNotifications` | `findByUserIdAndReadFalseOrderByCreatedAtDesc` filters read |
| `shouldCountUnreadNotifications` | `countByUserIdAndReadFalse` returns correct count |
| `shouldDeleteUser_NotificationCascade` | Delete user → user removed (cascade at DB level) |

#### Cross-Entity Tests (2 tests)

| Test | What it verifies |
|---|---|
| `shouldPersistCompleteUserProfile` | User + 3 transactions + 2 goals + 2 notifications all persisted |
| `shouldDeleteUser_AllCascade` | Delete user → user removed (all child entities cascade at DB level) |

---

### 2. Auth Flow Integration Tests

**Class**: `AuthFlowIntegrationTest.java`
**Tests**: 15
**Uses**: `RestTemplate` against running server on random port

Tests the complete authentication lifecycle through HTTP requests.

#### Register Tests (4 tests)

| Test | Request | Expected |
|---|---|---|
| `shouldRegisterNewUser` | `POST /api/v1/auth/register` with valid user | 200 OK, user persisted with encoded password |
| `shouldGenerateUuid_IfNotProvided` | Register without UUID | Auto-generated UUID in database |
| `shouldReturn409_WhenUsernameExists` | Register with duplicate username | 409 CONFLICT |
| `shouldReturn409_WhenEmailExists` | Register with duplicate email | 409 CONFLICT |

#### Login Tests (3 tests)

| Test | Request | Expected |
|---|---|---|
| `shouldLoginAndReturnToken` | `POST /api/v1/auth/login` with valid credentials | 200 OK with valid JWT (3 parts) |
| `shouldReturn401_WhenInvalidPassword` | Login with wrong password | 401 UNAUTHORIZED |
| `shouldReturn401_WhenUserNotFound` | Login with non-existent username | 401 UNAUTHORIZED |

#### Protected Endpoint Tests (4 tests)

| Test | Request | Expected |
|---|---|---|
| `shouldAccessProtectedEndpoint_WithValidToken` | GET with Bearer token | 200 OK |
| `shouldReturn403_WithoutToken` | GET without token | 403 FORBIDDEN |
| `shouldReturn403_WithInvalidToken` | GET with bad token | 403 FORBIDDEN |
| `shouldAccessMultipleEndpoints_WithSameToken` | Transaction, goals, notifications, analytics | All 200 OK |

#### Logout & Delete Tests (2 tests)

| Test | Request | Expected |
|---|---|---|
| `shouldLogoutSuccessfully` | `POST /api/v1/auth/logout` | 200 OK, "Logged out" |
| `shouldDeleteUser_AndPreventLogin` | Delete user → try login | 401 UNAUTHORIZED |

#### Full Lifecycle Tests (2 tests)

| Test | Flow | Expected |
|---|---|---|
| `shouldCompleteFullLifecycle` | Register → Login → Access → Logout → Token still works | All steps succeed |
| `shouldSupportMultipleUsers` | Register userA + userB → both login → both tokens work | Independent auth |

---

### 3. Application Flow Integration Tests

**Class**: `ApplicationFlowIntegrationTest.java`
**Tests**: 15
**Uses**: `RestTemplate` against running server, real services

Tests the complete user journey through the application.

#### Transaction Upload Flow Tests (4 tests)

| Test | Request | Expected |
|---|---|---|
| `shouldUploadCsv_WithClassifiedTransactions` | Upload CSV with 3 rows (Swiggy, Uber, Salary) | 3 transactions persisted with categories |
| `shouldReturnCategorySummary_AfterUpload` | Upload CSV → GET `/transaction/summary` | FOOD=800, TRANSPORT=200 |
| `shouldRejectUnsupportedFileType` | Upload PDF file | 400 BAD REQUEST |
| `shouldRejectUpload_WithoutUserId` | Upload without userId param | 400 BAD REQUEST |

#### Goal Management Flow Tests (3 tests)

| Test | Flow | Expected |
|---|---|---|
| `shouldPerformFullGoalCrud` | Create → Get all → Get active → Update amount → Delete → Verify empty | All CRUD operations succeed |
| `shouldGetGoalProgress` | Create FOOD goal (₹1000) → Upload ₹400 Swiggy → Get progress | 40% used |
| `shouldReturn404_ForNonExistentGoal` | Get progress / Delete non-existent goal | 404 NOT FOUND |

#### Notification Flow Tests (2 tests)

| Test | Flow | Expected |
|---|---|---|
| `shouldPerformFullNotificationCrud` | Create → Get all → Get unread → Get count → Mark read → Verify count 0 → Delete → Verify empty | Full notification lifecycle |
| `shouldCreateMultiple_AndMarkAllRead` | Create 3 → Count=3 → Mark all read → Unread empty | Count goes from 3 to 0 |

#### Analytics Flow Tests (5 tests)

| Test | Request | Expected |
|---|---|---|
| `shouldReturnSpendingSummary` | GET `/analytics/summary` | Income=50000, Expense=20249, NetSavings=29751, TopCategory=RENT |
| `shouldReturnCategoryBreakdown` | GET `/analytics/categories` | RENT first (highest), percentages sum to ~100% |
| `shouldReturnMonthlyTrends` | GET `/analytics/trends?periods=3` | 3 monthly data points |
| `shouldReturnBudgetStatus` | GET `/analytics/budget` | Current month spending per category |
| `shouldReturnBadRequest_WithoutUserId` | GET without userId | 400 BAD REQUEST |

#### End-to-End Journey Test (1 test)

| Test | Flow |
|---|---|
| `shouldCompleteFullJourney` | Register → Upload CSV → Verify 3 transactions → Create FOOD goal → Check analytics → Check notifications → Check goal progress (10%) → Upload more transactions → Goal exceeded (110%) |

---

### 4. AI Integration Tests

**Class**: `AiIntegrationTest.java`
**Tests**: 17
**Uses**: `RestTemplate` against real Ollama instance running in Docker

Tests the AI financial coach with the actual LLM.

#### Chat Endpoint Tests (5 tests)

| Test | Request | Expected |
|---|---|---|
| `shouldReturnChatResponse` | `POST /ai/chat` with "How can I save more money?" | 200 OK with non-null AI `reply` |
| `shouldReturnPersonalizedResponse` | Add transactions → chat "What's my financial health?" | AI response references financial data |
| `shouldReturn400_WhenUserIdNull` | Chat with null userId | 400 BAD REQUEST |
| `shouldReturn400_WhenMessageBlank` | Chat with empty message | 400 BAD REQUEST |
| `shouldPersistChatMessages` | Send two messages | Both USER and ASSISTANT messages persisted |

#### Recommendations Endpoint Tests (2 tests)

| Test | Request | Expected |
|---|---|
| `shouldReturnRecommendations` | Add income + expense → GET `/ai/recommendations` | Non-empty list of AI-generated recommendations |
| `shouldReturn400_WhenUserIdBlank` | GET without userId | 400 BAD REQUEST |

#### Health Score Endpoint Tests (3 tests)

| Test | Data | Expected Score |
|---|---|---|
| `shouldCalculateHealthScore` | Income ₹50000, Expense ₹5000 (90% savings rate) | High score (≥60), savingsRate=90% |
| `shouldReturnLowScore_WhenExpensesExceedIncome` | Income ₹10000, Expense ₹15000 | Low score (<40), negative savingsRate |
| `shouldReturn400_WhenUserIdBlank` | GET without userId | 400 BAD REQUEST |

#### Affordability Endpoint Tests (6 tests)

| Test | Request | Expected |
|---|---|---|
| `shouldAnalyzeAffordability_Affordable` | Income ₹50000, check ₹5000 headphones | `affordable=true` with AI analysis |
| `shouldAnalyzeAffordability_NotAffordable` | Income ₹10000 + ₹15000 expense, check ₹5000 item | `affordable=false` with AI analysis |
| `shouldReturn400_WhenUserIdNull` | Check with null userId | 400 BAD REQUEST |
| `shouldReturn400_WhenItemNameBlank` | Check with empty itemName | 400 BAD REQUEST |
| `shouldReturn400_WhenCostIsZero` | Check with zero cost | 400 BAD REQUEST |
| `shouldReturn400_WhenCostIsNegative` | Check with negative cost | 400 BAD REQUEST |

#### Auth Tests (1 test)

| Test | Request | Expected |
|---|---|
| `shouldReturn403_WithoutToken` | All 4 AI endpoints without token | 403 FORBIDDEN for all |

---

## Configuration

### Maven Profile

Integration tests are gated behind the `integration-tests` profile in `pom.xml`:

```xml
<profiles>
    <profile>
        <id>integration-tests</id>
        <activation>
            <property>
                <name>run.integration.tests</name>
                <value>true</value>
            </property>
        </activation>
        <build>
            <plugins>
                <plugin>
                    <groupId>org.apache.maven.plugins</groupId>
                    <artifactId>maven-surefire-plugin</artifactId>
                    <configuration>
                        <includes>
                            <include>**/integration/**</include>
                        </includes>
                    </configuration>
                </plugin>
            </plugins>
        </build>
    </profile>
</profiles>
```

By default, surefire excludes `**/integration/**`:
```xml
<excludes>
    <exclude>**/integration/**</exclude>
</excludes>
```

### Test Application Properties (`application-test.yaml`)

```yaml
spring:
  datasource:
    url: jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=false
    driver-class-name: org.h2.Driver
  jpa:
    hibernate:
      ddl-auto: create-drop
    database-platform: org.hibernate.dialect.H2Dialect

jwt:
  secret: <base64-encoded-secret>
  expiration: 86400000

ollama:
  base-url: http://localhost:11434
  model: llama3.2
  temperature: 0.7
  max-tokens: 512

server:
  servlet:
    context-path: /money_tracker
```

---

## API Endpoints Reference

All endpoints are prefixed with `/money_tracker` (server.servlet.context-path).

### Auth Endpoints

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| POST | `/api/v1/auth/register` | Public | Register new user |
| POST | `/api/v1/auth/login` | Public | Login, returns JWT token |
| POST | `/api/v1/auth/logout` | Public | Clears security context |
| DELETE | `/api/v1/auth/users/{username}` | Public | Delete user |

### Transaction Endpoints

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| POST | `/api/v1/transaction/upload` | Required | Upload bank statement (CSV/XLSX). Form field: `bank_statements` |
| GET | `/api/v1/transaction?userId={id}` | Required | List user transactions |
| GET | `/api/v1/transaction/summary?userId={id}` | Required | Category-wise spending summary |

### Goal Endpoints

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| POST | `/api/v1/goals` | Required | Create spending goal |
| GET | `/api/v1/goals?userId={id}` | Required | List all goals |
| GET | `/api/v1/goals/active?userId={id}` | Required | List active goals |
| PUT | `/api/v1/goals/{goalId}` | Required | Update goal |
| DELETE | `/api/v1/goals/{goalId}` | Required | Delete goal |
| GET | `/api/v1/goals/{goalId}/progress` | Required | Get goal progress |

### Notification Endpoints

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| POST | `/api/v1/notifications` | Required | Create notification |
| GET | `/api/v1/notifications?userId={id}` | Required | List notifications (newest first) |
| GET | `/api/v1/notifications/unread?userId={id}` | Required | List unread notifications |
| GET | `/api/v1/notifications/unread/count?userId={id}` | Required | Count unread notifications |
| PUT | `/api/v1/notifications/{id}/read` | Required | Mark as read |
| PUT | `/api/v1/notifications/read-all?userId={id}` | Required | Mark all as read |
| DELETE | `/api/v1/notifications/{id}` | Required | Delete notification |

### Analytics Endpoints

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| GET | `/api/v1/analytics/summary?userId={id}` | Required | Income/expense/net/top category |
| GET | `/api/v1/analytics/categories?userId={id}` | Required | Per-category breakdown with % |
| GET | `/api/v1/analytics/trends?userId={id}&periods=6` | Required | Monthly income/expense/net trends |
| GET | `/api/v1/analytics/comparison?userId={id}` | Required | Current vs previous period comparison |
| GET | `/api/v1/analytics/budget?userId={id}` | Required | Budget status per category |

### AI Coach Endpoints

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| POST | `/api/v1/ai/chat` | Required | Chat with AI financial coach |
| GET | `/api/v1/ai/recommendations?userId={id}` | Required | Personalized spending recommendations |
| GET | `/api/v1/ai/health-score?userId={id}` | Required | Financial health score (0-100) |
| POST | `/api/v1/ai/affordability` | Required | Can user afford a purchase? |

### cURL Example: Upload Bank Statement

```bash
curl --location 'http://localhost:8080/money_tracker/api/v1/transaction/upload' \
  --header 'Authorization: Bearer <your-jwt-token>' \
  --form 'bank_statements=@"path/to/statement.csv"' \
  --form 'userId=<user-uuid>'
```

---

## Troubleshooting

### Integration tests not running
Make sure you pass the profile flag:
```bash
./mvnw test -Drun.integration.tests=true
```

### Ollama-related test failures
1. Ensure Ollama is running: `curl http://localhost:11434/api/tags`
2. Ensure the model is loaded: the response should include `llama3.2`
3. Pull the model if needed: `curl http://localhost:11434/api/pull -d '{"name":"llama3.2"}'`
4. AI tests may take 2-5 minutes total due to LLM response times

### Port conflicts
Tests use `WebEnvironment.RANDOM_PORT`, so they won't conflict with your running application on port 8080.

### 403 instead of 401
Spring Security returns 403 (Forbidden) for unauthenticated requests by default (not 401). This is correct behavior — the tests reflect this.

### Database not cleaning between tests
The H2 in-memory database is configured with `create-drop`, which recreates the schema for each test class. Each test class gets a fresh database.

### Slow test execution
- Database tests: ~12 seconds (fast, H2 in-memory)
- Auth tests: ~37 seconds (server startup per class)
- Application flow tests: ~19 seconds
- AI tests: ~3-5 minutes (depends on Ollama/LLM response time)
