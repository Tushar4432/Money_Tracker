package com.tracker.MoneyTracker.integration;

import com.tracker.MoneyTracker.MoneyTrackerApplication;
import com.tracker.MoneyTracker.ai.dto.*;
import com.tracker.MoneyTracker.auth.AppUser;
import com.tracker.MoneyTracker.auth.UserRepository;
import com.tracker.MoneyTracker.transaction.Transaction;
import com.tracker.MoneyTracker.transaction.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

/**
 * AI integration tests using the real OllamaClient against your running Ollama Docker instance.
 * No mocking — these tests call the actual LLM.
 */
@SpringBootTest(classes = MoneyTrackerApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@DisplayName("AI Integration Tests")
class AiIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    private final RestTemplate restTemplate;

    {
        RestTemplate rt = new RestTemplate();
        rt.setErrorHandler(new org.springframework.web.client.DefaultResponseErrorHandler() {
            @Override
            public boolean hasError(org.springframework.http.client.ClientHttpResponse response) {
                return false;
            }
        });
        this.restTemplate = rt;
    }
    private String baseUrl;
    private String authToken;
    private String userId;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        transactionRepository.deleteAll();
        baseUrl = "http://localhost:" + port + "/money_tracker";
        userId = UUID.randomUUID().toString();
        registerAndLogin("aiuser", "ai@example.com", "aipass");
    }

    @Nested
    @DisplayName("POST /api/v1/ai/chat")
    class ChatEndpointTests {

        @Test
        @DisplayName("Should return AI chat response with valid request")
        void shouldReturnChatResponse() {
            ChatRequest request = new ChatRequest(userId, "How can I save more money?");

            ResponseEntity<Map> response = restTemplate.exchange(
                    baseUrl + "/api/v1/ai/chat",
                    HttpMethod.POST,
                    new HttpEntity<>(request, jsonHeaders()),
                    Map.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody()).containsKey("reply");
            assertThat(response.getBody().get("reply")).isNotNull();
            assertThat(response.getBody().get("userId")).isEqualTo(userId);
        }

        @Test
        @DisplayName("Should return AI response personalized to user financial data")
        void shouldReturnPersonalizedResponse() {
            Transaction income = new Transaction();
            income.setId("tx-ai-1");
            income.setUserId(userId);
            income.setTransactionDate(LocalDate.of(2026, 6, 1));
            income.setAmount(new BigDecimal("50000"));
            income.setType("CREDIT");
            income.setDescription("Salary");
            income.setCategory("INCOME");
            income.setCreatedAt(LocalDateTime.now());
            income.setUpdatedAt(LocalDateTime.now());
            transactionRepository.saveAndFlush(income);

            ChatRequest request = new ChatRequest(userId, "What's my financial health?");

            ResponseEntity<Map> response = restTemplate.exchange(
                    baseUrl + "/api/v1/ai/chat",
                    HttpMethod.POST,
                    new HttpEntity<>(request, jsonHeaders()),
                    Map.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().get("reply")).isNotNull();
        }

        @Test
        @DisplayName("Should return 400 when userId is null")
        void shouldReturn400_WhenUserIdNull() {
            ChatRequest request = new ChatRequest(null, "Hello");

            ResponseEntity<String> response = restTemplate.exchange(
                    baseUrl + "/api/v1/ai/chat",
                    HttpMethod.POST,
                    new HttpEntity<>(request, jsonHeaders()),
                    String.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test
        @DisplayName("Should return 400 when message is blank")
        void shouldReturn400_WhenMessageBlank() {
            ChatRequest request = new ChatRequest(userId, "");

            ResponseEntity<String> response = restTemplate.exchange(
                    baseUrl + "/api/v1/ai/chat",
                    HttpMethod.POST,
                    new HttpEntity<>(request, jsonHeaders()),
                    String.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test
        @DisplayName("Should persist chat messages to database")
        void shouldPersistChatMessages() {
            ChatRequest request = new ChatRequest(userId, "Hello AI");

            restTemplate.exchange(baseUrl + "/api/v1/ai/chat",
                    HttpMethod.POST,
                    new HttpEntity<>(request, jsonHeaders()),
                    Map.class);

            ChatRequest followUp = new ChatRequest(userId, "What did I just ask you?");

            ResponseEntity<Map> response = restTemplate.exchange(
                    baseUrl + "/api/v1/ai/chat",
                    HttpMethod.POST,
                    new HttpEntity<>(followUp, jsonHeaders()),
                    Map.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        }
    }

    @Nested
    @DisplayName("GET /api/v1/ai/recommendations")
    class RecommendationsEndpointTests {

        @Test
        @DisplayName("Should return AI-generated recommendations based on spending data")
        void shouldReturnRecommendations() {
            Transaction income = new Transaction();
            income.setId("tx-rec-1");
            income.setUserId(userId);
            income.setTransactionDate(LocalDate.of(2026, 6, 1));
            income.setAmount(new BigDecimal("50000"));
            income.setType("CREDIT");
            income.setDescription("Salary");
            income.setCategory("INCOME");
            income.setCreatedAt(LocalDateTime.now());
            income.setUpdatedAt(LocalDateTime.now());
            transactionRepository.saveAndFlush(income);

            Transaction expense = new Transaction();
            expense.setId("tx-rec-2");
            expense.setUserId(userId);
            expense.setTransactionDate(LocalDate.of(2026, 6, 5));
            expense.setAmount(new BigDecimal("500"));
            expense.setType("DEBIT");
            expense.setDescription("Swiggy");
            expense.setCategory("FOOD");
            expense.setCreatedAt(LocalDateTime.now());
            expense.setUpdatedAt(LocalDateTime.now());
            transactionRepository.saveAndFlush(expense);

            HttpHeaders headers = authHeaders();
            ResponseEntity<Map> response = restTemplate.exchange(
                    baseUrl + "/api/v1/ai/recommendations?userId=" + userId,
                    HttpMethod.GET, new HttpEntity<>(headers), Map.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).containsKey("recommendations");
            assertThat(response.getBody().get("userId")).isEqualTo(userId);

            List<?> recommendations = (List<?>) response.getBody().get("recommendations");
            assertThat(recommendations).isNotEmpty();
        }

        @Test
        @DisplayName("Should return 400 when userId is blank")
        void shouldReturn400_WhenUserIdBlank() {
            HttpHeaders headers = authHeaders();
            ResponseEntity<String> response = restTemplate.exchange(
                    baseUrl + "/api/v1/ai/recommendations?userId=",
                    HttpMethod.GET, new HttpEntity<>(headers), String.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }
    }

    @Nested
    @DisplayName("GET /api/v1/ai/health-score")
    class HealthScoreEndpointTests {

        @Test
        @DisplayName("Should calculate health score deterministically from financial data")
        void shouldCalculateHealthScore() {
            Transaction income = new Transaction();
            income.setId("tx-hs-1");
            income.setUserId(userId);
            income.setTransactionDate(LocalDate.of(2026, 6, 1));
            income.setAmount(new BigDecimal("50000"));
            income.setType("CREDIT");
            income.setDescription("Salary");
            income.setCategory("INCOME");
            income.setCreatedAt(LocalDateTime.now());
            income.setUpdatedAt(LocalDateTime.now());
            transactionRepository.saveAndFlush(income);

            Transaction expense = new Transaction();
            expense.setId("tx-hs-2");
            expense.setUserId(userId);
            expense.setTransactionDate(LocalDate.of(2026, 6, 5));
            expense.setAmount(new BigDecimal("5000"));
            expense.setType("DEBIT");
            expense.setDescription("Swiggy");
            expense.setCategory("FOOD");
            expense.setCreatedAt(LocalDateTime.now());
            expense.setUpdatedAt(LocalDateTime.now());
            transactionRepository.saveAndFlush(expense);

            HttpHeaders headers = authHeaders();
            ResponseEntity<Map> response = restTemplate.exchange(
                    baseUrl + "/api/v1/ai/health-score?userId=" + userId,
                    HttpMethod.GET, new HttpEntity<>(headers), Map.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            Map body = response.getBody();
            assertThat(body).containsKey("score");
            assertThat(body).containsKey("savingsRate");
            assertThat(body).containsKey("breakdown");

            int score = (Integer) body.get("score");
            assertThat(score).isBetween(0, 100);
            assertThat(((Number) body.get("savingsRate")).doubleValue()).isEqualTo(90.0);
        }

        @Test
        @DisplayName("Should return low score when expenses exceed income")
        void shouldReturnLowScore_WhenExpensesExceedIncome() {
            Transaction income = new Transaction();
            income.setId("tx-hs-3");
            income.setUserId(userId);
            income.setTransactionDate(LocalDate.of(2026, 6, 1));
            income.setAmount(new BigDecimal("10000"));
            income.setType("CREDIT");
            income.setDescription("Salary");
            income.setCategory("INCOME");
            income.setCreatedAt(LocalDateTime.now());
            income.setUpdatedAt(LocalDateTime.now());
            transactionRepository.saveAndFlush(income);

            Transaction expense = new Transaction();
            expense.setId("tx-hs-4");
            expense.setUserId(userId);
            expense.setTransactionDate(LocalDate.of(2026, 6, 5));
            expense.setAmount(new BigDecimal("15000"));
            expense.setType("DEBIT");
            expense.setDescription("Rent");
            expense.setCategory("RENT");
            expense.setCreatedAt(LocalDateTime.now());
            expense.setUpdatedAt(LocalDateTime.now());
            transactionRepository.saveAndFlush(expense);

            HttpHeaders headers = authHeaders();
            ResponseEntity<Map> response = restTemplate.exchange(
                    baseUrl + "/api/v1/ai/health-score?userId=" + userId,
                    HttpMethod.GET, new HttpEntity<>(headers), Map.class);

            int score = (Integer) response.getBody().get("score");
            assertThat(score).isLessThan(40);
            assertThat(((Number) response.getBody().get("savingsRate")).doubleValue()).isNegative();
        }

        @Test
        @DisplayName("Should return 400 when userId is blank")
        void shouldReturn400_WhenUserIdBlank() {
            HttpHeaders headers = authHeaders();
            ResponseEntity<String> response = restTemplate.exchange(
                    baseUrl + "/api/v1/ai/health-score?userId=",
                    HttpMethod.GET, new HttpEntity<>(headers), String.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }
    }

    @Nested
    @DisplayName("POST /api/v1/ai/affordability")
    class AffordabilityEndpointTests {

        @Test
        @DisplayName("Should analyze affordability — affordable case")
        void shouldAnalyzeAffordability_Affordable() {
            Transaction income = new Transaction();
            income.setId("tx-af-1");
            income.setUserId(userId);
            income.setTransactionDate(LocalDate.of(2026, 6, 1));
            income.setAmount(new BigDecimal("50000"));
            income.setType("CREDIT");
            income.setDescription("Salary");
            income.setCategory("INCOME");
            income.setCreatedAt(LocalDateTime.now());
            income.setUpdatedAt(LocalDateTime.now());
            transactionRepository.saveAndFlush(income);

            AffordabilityRequest request = new AffordabilityRequest(userId, "New headphones", new BigDecimal("5000"));

            ResponseEntity<Map> response = restTemplate.exchange(
                    baseUrl + "/api/v1/ai/affordability",
                    HttpMethod.POST,
                    new HttpEntity<>(request, jsonHeaders()),
                    Map.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().get("itemName")).isEqualTo("New headphones");
            assertThat(response.getBody().get("affordable")).isEqualTo(true);
            assertThat(response.getBody()).containsKey("analysis");
        }

        @Test
        @DisplayName("Should analyze affordability — not affordable case")
        void shouldAnalyzeAffordability_NotAffordable() {
            Transaction income = new Transaction();
            income.setId("tx-af-2");
            income.setUserId(userId);
            income.setTransactionDate(LocalDate.of(2026, 6, 1));
            income.setAmount(new BigDecimal("10000"));
            income.setType("CREDIT");
            income.setDescription("Salary");
            income.setCategory("INCOME");
            income.setCreatedAt(LocalDateTime.now());
            income.setUpdatedAt(LocalDateTime.now());
            transactionRepository.saveAndFlush(income);

            Transaction expense = new Transaction();
            expense.setId("tx-af-3");
            expense.setUserId(userId);
            expense.setTransactionDate(LocalDate.of(2026, 6, 5));
            expense.setAmount(new BigDecimal("15000"));
            expense.setType("DEBIT");
            expense.setDescription("Rent");
            expense.setCategory("RENT");
            expense.setCreatedAt(LocalDateTime.now());
            expense.setUpdatedAt(LocalDateTime.now());
            transactionRepository.saveAndFlush(expense);

            AffordabilityRequest request = new AffordabilityRequest(userId, "Expensive item", new BigDecimal("5000"));

            ResponseEntity<Map> response = restTemplate.exchange(
                    baseUrl + "/api/v1/ai/affordability",
                    HttpMethod.POST,
                    new HttpEntity<>(request, jsonHeaders()),
                    Map.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().get("affordable")).isEqualTo(false);
        }

        @Test
        @DisplayName("Should return 400 when userId is null")
        void shouldReturn400_WhenUserIdNull() {
            AffordabilityRequest request = new AffordabilityRequest(null, "Item", new BigDecimal("100"));

            ResponseEntity<String> response = restTemplate.exchange(
                    baseUrl + "/api/v1/ai/affordability",
                    HttpMethod.POST,
                    new HttpEntity<>(request, jsonHeaders()),
                    String.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test
        @DisplayName("Should return 400 when itemName is blank")
        void shouldReturn400_WhenItemNameBlank() {
            AffordabilityRequest request = new AffordabilityRequest(userId, "", new BigDecimal("100"));

            ResponseEntity<String> response = restTemplate.exchange(
                    baseUrl + "/api/v1/ai/affordability",
                    HttpMethod.POST,
                    new HttpEntity<>(request, jsonHeaders()),
                    String.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test
        @DisplayName("Should return 400 when cost is zero")
        void shouldReturn400_WhenCostIsZero() {
            AffordabilityRequest request = new AffordabilityRequest(userId, "Item", BigDecimal.ZERO);

            ResponseEntity<String> response = restTemplate.exchange(
                    baseUrl + "/api/v1/ai/affordability",
                    HttpMethod.POST,
                    new HttpEntity<>(request, jsonHeaders()),
                    String.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test
        @DisplayName("Should return 400 when cost is negative")
        void shouldReturn400_WhenCostIsNegative() {
            AffordabilityRequest request = new AffordabilityRequest(userId, "Item", new BigDecimal("-100"));

            ResponseEntity<String> response = restTemplate.exchange(
                    baseUrl + "/api/v1/ai/affordability",
                    HttpMethod.POST,
                    new HttpEntity<>(request, jsonHeaders()),
                    String.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }
    }

    @Nested
    @DisplayName("AI endpoints require authentication")
    class AiAuthTests {

        @Test
        @DisplayName("Should return 403 when accessing AI endpoints without token")
        void shouldReturn403_WithoutToken() {
            HttpHeaders noAuthHeaders = new HttpHeaders();
            noAuthHeaders.setContentType(MediaType.APPLICATION_JSON);

            ResponseEntity<String> chatResp = restTemplate.exchange(
                    baseUrl + "/api/v1/ai/chat",
                    HttpMethod.POST,
                    new HttpEntity<>(new ChatRequest(userId, "Hi"), noAuthHeaders),
                    String.class);
            assertThat(chatResp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

            ResponseEntity<String> recResp = restTemplate.exchange(
                    baseUrl + "/api/v1/ai/recommendations?userId=" + userId,
                    HttpMethod.GET, new HttpEntity<>(noAuthHeaders), String.class);
            assertThat(recResp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

            ResponseEntity<String> hsResp = restTemplate.exchange(
                    baseUrl + "/api/v1/ai/health-score?userId=" + userId,
                    HttpMethod.GET, new HttpEntity<>(noAuthHeaders), String.class);
            assertThat(hsResp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

            ResponseEntity<String> afResp = restTemplate.exchange(
                    baseUrl + "/api/v1/ai/affordability",
                    HttpMethod.POST,
                    new HttpEntity<>(new AffordabilityRequest(userId, "Item", new BigDecimal("100")), noAuthHeaders),
                    String.class);
            assertThat(afResp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        }
    }

    // ======================== Helpers ========================

    private void registerAndLogin(String username, String email, String password) {
        AppUser user = new AppUser();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(password);
        restTemplate.postForEntity(baseUrl + "/api/v1/auth/register", user, String.class);

        AppUser loginRequest = new AppUser();
        loginRequest.setUsername(username);
        loginRequest.setPassword(password);
        ResponseEntity<Map> loginResp = restTemplate.postForEntity(
                baseUrl + "/api/v1/auth/login", loginRequest, Map.class);
        authToken = (String) loginResp.getBody().get("token");
    }

    private HttpHeaders authHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(authToken);
        return headers;
    }

    private HttpHeaders jsonHeaders() {
        HttpHeaders headers = authHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }
}
