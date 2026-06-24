# MoneyTracker API Endpoints

**Base URL:** `http://localhost:8080/money_tracker`  
**API Version:** `v1`  
**Authentication:** JWT Bearer token (obtained via `/api/v1/auth/login`)

All endpoints are prefixed with `/api/v1`.

---

## Table of Contents

- [Authentication](#authentication)
- [Transactions](#transactions)
- [Goals](#goals)
- [AI Coach](#ai-coach)
- [Notifications](#notifications)
- [Analytics](#analytics)

---

## Authentication

Base path: `/api/v1/auth`

### POST `/api/v1/auth/register`

Register a new user.

**Request Body:**

```json
{
  "uuid": "string (optional, auto-generated if blank)",
  "username": "string",
  "password": "string",
  "email": "string"
}
```

**Responses:**

| Status | Description | Body |
|--------|-------------|------|
| `200 OK` | User registered successfully | `"User registered"` |

---

### POST `/api/v1/auth/login`

Authenticate a user and obtain a JWT token.

**Request Body:**

```json
{
  "username": "string",
  "password": "string"
}
```

**Responses:**

| Status | Description | Body |
|--------|-------------|------|
| `200 OK` | Login successful | `{ "token": "string", "userId": "string", "username": "string", "email": "string" }` |

---

### GET `/api/v1/auth/me`

Get the currently authenticated user's profile.

**Authentication:** Required (JWT)

**Responses:**

| Status | Description | Body |
|--------|-------------|------|
| `200 OK` | Profile retrieved | `{ "userId": "string", "username": "string", "email": "string", "createdAt": "string" }` |
| `401 Unauthorized` | Not authenticated | `{ "error": "Not authenticated" }` |

---

### POST `/api/v1/auth/logout`

Log out the current user by clearing the security context.

**Responses:**

| Status | Description | Body |
|--------|-------------|------|
| `200 OK` | Logged out successfully | `"Logged out"` |

---

### DELETE `/api/v1/auth/users/{username}`

Delete a user account by username.

**Path Parameters:**

| Parameter | Type | Description |
|-----------|------|-------------|
| `username` | string | The username of the user to delete |

**Responses:**

| Status | Description | Body |
|--------|-------------|------|
| `200 OK` | User deleted | `"User deleted"` |

---

## Transactions

Base path: `/api/v1/transaction`

### POST `/api/v1/transaction/upload`

Upload a bank statement file (CSV or XLSX/XLS) for processing.

**Content-Type:** `multipart/form-data`

**Request Parameters:**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `bank_statements` | file | Yes | Bank statement file (`.csv`, `.xlsx`, or `.xls`) |
| `userId` | string | Yes | The ID of the user uploading the statement |

**Responses:**

| Status | Description | Body |
|--------|-------------|------|
| `201 Created` | File uploaded and processed successfully | *(empty)* |
| `400 Bad Request` | Invalid input or unsupported file type | Error details |

---

### GET `/api/v1/transaction`

Retrieve all transactions for a user.

**Query Parameters:**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `userId` | string | Yes | The user whose transactions to fetch |

**Responses:**

| Status | Description | Body |
|--------|-------------|------|
| `200 OK` | List of transactions | `Transaction[]` |
| `400 Bad Request` | userId is missing | Error details |

---

### GET `/api/v1/transaction/summary`

Get a category-wise spending summary for a user.

**Query Parameters:**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `userId` | string | Yes | The user whose summary to fetch |

**Responses:**

| Status | Description | Body |
|--------|-------------|------|
| `200 OK` | Category to amount mapping | `{ "category": amount, ... }` (BigDecimal values) |
| `400 Bad Request` | userId is missing | Error details |

---

## Goals

Base path: `/api/v1/goals`

### POST `/api/v1/goals`

Create a new spending goal.

**Request Body:** (`SpendingGoal`)

```json
{
  "userId": "string",
  "category": "string",
  "targetAmount": "number",
  "period": "string (e.g., MONTHLY, WEEKLY)",
  "startDate": "string (ISO date)",
  "endDate": "string (ISO date)"
}
```

**Responses:**

| Status | Description | Body |
|--------|-------------|------|
| `201 Created` | Goal created | `SpendingGoal` object |

---

### GET `/api/v1/goals`

Get all spending goals for a user.

**Query Parameters:**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `userId` | string | Yes | The user whose goals to fetch |

**Responses:**

| Status | Description | Body |
|--------|-------------|------|
| `200 OK` | List of goals | `SpendingGoal[]` |
| `400 Bad Request` | userId is missing | Error details |

---

### GET `/api/v1/goals/active`

Get all **active** spending goals for a user.

**Query Parameters:**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `userId` | string | Yes | The user whose active goals to fetch |

**Responses:**

| Status | Description | Body |
|--------|-------------|------|
| `200 OK` | List of active goals | `SpendingGoal[]` |
| `400 Bad Request` | userId is missing | Error details |

---

### PUT `/api/v1/goals/{goalId}`

Update an existing spending goal.

**Path Parameters:**

| Parameter | Type | Description |
|-----------|------|-------------|
| `goalId` | string | The ID of the goal to update |

**Request Body:** (`SpendingGoal`)

```json
{
  "category": "string",
  "targetAmount": "number",
  "period": "string",
  "startDate": "string",
  "endDate": "string"
}
```

**Responses:**

| Status | Description | Body |
|--------|-------------|------|
| `200 OK` | Goal updated | Updated `SpendingGoal` object |

---

### DELETE `/api/v1/goals/{goalId}`

Delete a spending goal.

**Path Parameters:**

| Parameter | Type | Description |
|-----------|------|-------------|
| `goalId` | string | The ID of the goal to delete |

**Responses:**

| Status | Description | Body |
|--------|-------------|------|
| `204 No Content` | Goal deleted successfully | *(empty)* |

---

### GET `/api/v1/goals/{goalId}/progress`

Get the progress of a specific spending goal.

**Path Parameters:**

| Parameter | Type | Description |
|-----------|------|-------------|
| `goalId` | string | The ID of the goal |

**Responses:**

| Status | Description | Body |
|--------|-------------|------|
| `200 OK` | Goal progress details | `GoalProgress` object |

---

## AI Coach

Base path: `/api/v1/ai`

The AI Coach feature uses an LLM (Ollama / Llama 3.2) to provide personalized financial guidance.

### POST `/api/v1/ai/chat`

Chat with the AI financial coach. Sends the user's message along with their financial context to the LLM.

**Request Body:** (`ChatRequest`)

```json
{
  "userId": "string (required)",
  "message": "string (required)"
}
```

**Responses:**

| Status | Description | Body |
|--------|-------------|------|
| `200 OK` | AI response | `ChatResponse { userId, message, reply, createdAt }` |
| `400 Bad Request` | userId or message is blank | Error details |

---

### GET `/api/v1/ai/recommendations`

Get personalized AI-generated spending recommendations.

**Query Parameters:**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `userId` | string | Yes | The user to generate recommendations for |

**Responses:**

| Status | Description | Body |
|--------|-------------|------|
| `200 OK` | AI recommendations | `RecommendationResponse { userId, recommendations: string[] }` |
| `400 Bad Request` | userId is blank | Error details |

---

### GET `/api/v1/ai/health-score`

Get the user's financial health score (0–100).

**Query Parameters:**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `userId` | string | Yes | The user whose health score to calculate |

**Responses:**

| Status | Description | Body |
|--------|-------------|------|
| `200 OK` | Health score breakdown | `HealthScoreResponse { userId, score: int, savingsRate: double, goalProgress: double, breakdown: string }` |
| `400 Bad Request` | userId is blank | Error details |

---

### POST `/api/v1/ai/affordability`

Analyze whether the user can afford a specific purchase.

**Request Body:** (`AffordabilityRequest`)

```json
{
  "userId": "string (required)",
  "itemName": "string (required)",
  "cost": "number (required, must be positive)"
}
```

**Responses:**

| Status | Description | Body |
|--------|-------------|------|
| `200 OK` | Affordability analysis | `AffordabilityResponse { userId, itemName, cost, affordable: boolean, analysis: string }` |
| `400 Bad Request` | Invalid input (missing userId, itemName, or cost ≤ 0) | Error details |

---

## Notifications

Base path: `/api/v1/notifications`

### POST `/api/v1/notifications`

Create a new notification.

**Request Body:** (`Notification`)

```json
{
  "userId": "string",
  "message": "string",
  "type": "string",
  "read": "boolean"
}
```

**Responses:**

| Status | Description | Body |
|--------|-------------|------|
| `201 Created` | Notification created | `Notification` object |

---

### GET `/api/v1/notifications`

Get all notifications for a user.

**Query Parameters:**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `userId` | string | Yes | The user whose notifications to fetch |

**Responses:**

| Status | Description | Body |
|--------|-------------|------|
| `200 OK` | List of notifications | `Notification[]` |
| `400 Bad Request` | userId is missing | Error details |

---

### GET `/api/v1/notifications/unread`

Get all **unread** notifications for a user.

**Query Parameters:**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `userId` | string | Yes | The user whose unread notifications to fetch |

**Responses:**

| Status | Description | Body |
|--------|-------------|------|
| `200 OK` | List of unread notifications | `Notification[]` |
| `400 Bad Request` | userId is missing | Error details |

---

### PUT `/api/v1/notifications/{notificationId}/read`

Mark a specific notification as read.

**Path Parameters:**

| Parameter | Type | Description |
|-----------|------|-------------|
| `notificationId` | string | The ID of the notification to mark as read |

**Responses:**

| Status | Description | Body |
|--------|-------------|------|
| `200 OK` | Notification marked as read | Updated `Notification` object |

---

### PUT `/api/v1/notifications/read-all`

Mark **all** notifications as read for a user.

**Query Parameters:**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `userId` | string | Yes | The user whose notifications to mark as read |

**Responses:**

| Status | Description | Body |
|--------|-------------|------|
| `204 No Content` | All notifications marked as read | *(empty)* |
| `400 Bad Request` | userId is missing | Error details |

---

### GET `/api/v1/notifications/unread/count`

Get the count of unread notifications for a user.

**Query Parameters:**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `userId` | string | Yes | The user whose unread count to fetch |

**Responses:**

| Status | Description | Body |
|--------|-------------|------|
| `200 OK` | Unread notification count | `Long` |
| `400 Bad Request` | userId is missing | Error details |

---

### DELETE `/api/v1/notifications/{notificationId}`

Delete a notification.

**Path Parameters:**

| Parameter | Type | Description |
|-----------|------|-------------|
| `notificationId` | string | The ID of the notification to delete |

**Responses:**

| Status | Description | Body |
|--------|-------------|------|
| `204 No Content` | Notification deleted | *(empty)* |

---

## Analytics

Base path: `/api/v1/analytics`

### GET `/api/v1/analytics/summary`

Get an overall spending summary for a user.

**Query Parameters:**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `userId` | string | Yes | The user whose summary to fetch |

**Responses:**

| Status | Description | Body |
|--------|-------------|------|
| `200 OK` | Spending summary | `SpendingSummary` object |
| `400 Bad Request` | userId is missing | *(empty body)* |

---

### GET `/api/v1/analytics/categories`

Get a breakdown of spending by category, optionally filtered by date range.

**Query Parameters:**

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `userId` | string | Yes | - | The user whose category breakdown to fetch |
| `start` | date (ISO) | No | - | Start date filter (e.g., `2025-01-01`) |
| `end` | date (ISO) | No | - | End date filter (e.g., `2025-12-31`) |

**Responses:**

| Status | Description | Body |
|--------|-------------|------|
| `200 OK` | Category breakdown | `CategoryBreakdown[]` |
| `400 Bad Request` | userId is missing | *(empty body)* |

---

### GET `/api/v1/analytics/trends`

Get monthly (or other period) spending trends.

**Query Parameters:**

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `userId` | string | Yes | - | The user whose trends to fetch |
| `periods` | int | No | `6` | Number of periods to return |
| `period` | string | No | `MONTHLY` | Period type (e.g., `MONTHLY`, `WEEKLY`) |

**Responses:**

| Status | Description | Body |
|--------|-------------|------|
| `200 OK` | Monthly trends | `MonthlyTrend[]` |
| `400 Bad Request` | userId is missing | *(empty body)* |

---

### GET `/api/v1/analytics/comparison`

Get spending comparison (e.g., current period vs. previous period).

**Query Parameters:**

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `userId` | string | Yes | - | The user whose comparison to fetch |
| `period` | string | No | `MONTHLY` | Period type (e.g., `MONTHLY`, `WEEKLY`) |

**Responses:**

| Status | Description | Body |
|--------|-------------|------|
| `200 OK` | Spending comparison | `SpendingComparison` object |
| `400 Bad Request` | userId is missing | *(empty body)* |

---

### GET `/api/v1/analytics/budget`

Get the budget status for a user (how spending compares to set goals/budgets).

**Query Parameters:**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `userId` | string | Yes | The user whose budget status to fetch |

**Responses:**

| Status | Description | Body |
|--------|-------------|------|
| `200 OK` | Budget status list | `BudgetStatus[]` |
| `400 Bad Request` | userId is missing | *(empty body)* |

---

## Summary

| # | Method | Endpoint | Description |
|---|--------|----------|-------------|
| 1 | `POST` | `/api/v1/auth/register` | Register a new user |
| 2 | `POST` | `/api/v1/auth/login` | Login and get JWT token |
| 3 | `GET` | `/api/v1/auth/me` | Get current user profile |
| 4 | `POST` | `/api/v1/auth/logout` | Logout current user |
| 5 | `DELETE` | `/api/v1/auth/users/{username}` | Delete a user |
| 6 | `POST` | `/api/v1/transaction/upload` | Upload bank statement |
| 7 | `GET` | `/api/v1/transaction` | Get user transactions |
| 8 | `GET` | `/api/v1/transaction/summary` | Get category spending summary |
| 9 | `POST` | `/api/v1/goals` | Create a spending goal |
| 10 | `GET` | `/api/v1/goals` | Get user goals |
| 11 | `GET` | `/api/v1/goals/active` | Get active goals |
| 12 | `PUT` | `/api/v1/goals/{goalId}` | Update a goal |
| 13 | `DELETE` | `/api/v1/goals/{goalId}` | Delete a goal |
| 14 | `GET` | `/api/v1/goals/{goalId}/progress` | Get goal progress |
| 15 | `POST` | `/api/v1/ai/chat` | Chat with AI coach |
| 16 | `GET` | `/api/v1/ai/recommendations` | Get AI recommendations |
| 17 | `GET` | `/api/v1/ai/health-score` | Get financial health score |
| 18 | `POST` | `/api/v1/ai/affordability` | Affordability analysis |
| 19 | `POST` | `/api/v1/notifications` | Create a notification |
| 20 | `GET` | `/api/v1/notifications` | Get user notifications |
| 21 | `GET` | `/api/v1/notifications/unread` | Get unread notifications |
| 22 | `PUT` | `/api/v1/notifications/{notificationId}/read` | Mark notification as read |
| 23 | `PUT` | `/api/v1/notifications/read-all` | Mark all notifications as read |
| 24 | `GET` | `/api/v1/notifications/unread/count` | Get unread notification count |
| 25 | `DELETE` | `/api/v1/notifications/{notificationId}` | Delete a notification |
| 26 | `GET` | `/api/v1/analytics/summary` | Spending summary |
| 27 | `GET` | `/api/v1/analytics/categories` | Category breakdown |
| 28 | `GET` | `/api/v1/analytics/trends` | Spending trends |
| 29 | `GET` | `/api/v1/analytics/comparison` | Spending comparison |
| 30 | `GET` | `/api/v1/analytics/budget` | Budget status |

**Total: 30 endpoints** across 6 controllers.

---

*Application: MoneyTracker | Server Port: 8080 | Context Path: `/money_tracker`*  
*AI Provider: Ollama (Llama 3.2) | JWT Expiration: 24 hours*
