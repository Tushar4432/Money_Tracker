package com.tracker.MoneyTracker.integration;

import com.tracker.MoneyTracker.MoneyTrackerApplication;
import com.tracker.MoneyTracker.auth.AppUser;
import com.tracker.MoneyTracker.auth.UserRepository;
import com.tracker.MoneyTracker.goal.SpendingGoal;
import com.tracker.MoneyTracker.notification.Notification;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

/**
 * End-to-end application flow integration tests using real services.
 * Tests: upload → goals → analytics → notifications.
 */
@SpringBootTest(classes = MoneyTrackerApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@DisplayName("Application Flow Integration Tests")
class ApplicationFlowIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private UserRepository userRepository;

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
        baseUrl = "http://localhost:" + port + "/money_tracker";
        userId = UUID.randomUUID().toString();
        registerAndLogin("flowuser", "flow@example.com", "flowpass");
    }

    @Nested
    @DisplayName("Transaction upload flow")
    class TransactionUploadFlowTests {

        @Test
        @DisplayName("Should upload CSV and have transactions persisted with categories")
        void shouldUploadCsv_WithClassifiedTransactions() {
            String csvContent = "Date,Details,Debit,Credit,Balance\n"
                    + "01/06/2026,UPI/DR/SWIGGY/HDFC/swiggy@upi,500.00,,9500.00\n"
                    + "02/06/2026,UPI/DR/UBER/HDFC/uber@upi,200.00,,9300.00\n"
                    + "03/06/2026,SALARY CREDIT XYZ CORP,,50000.00,59300.00\n";

            uploadCsv(csvContent);

            HttpHeaders headers = authHeaders();
            ResponseEntity<List> response = restTemplate.exchange(
                    baseUrl + "/api/v1/transaction?userId=" + userId,
                    HttpMethod.GET, new HttpEntity<>(headers), List.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody()).hasSize(3);
        }

        @Test
        @DisplayName("Should return category summary after upload with correct totals")
        void shouldReturnCategorySummary_AfterUpload() {
            String csvContent = "Date,Details,Debit,Credit,Balance\n"
                    + "01/06/2026,UPI/DR/SWIGGY/HDFC/swiggy@upi,500.00,,9500.00\n"
                    + "02/06/2026,UPI/DR/ZOMATO/ICICI/zomato@upi,300.00,,9200.00\n"
                    + "03/06/2026,UPI/DR/UBER/HDFC/uber@upi,200.00,,9000.00\n";

            uploadCsv(csvContent);

            HttpHeaders headers = authHeaders();
            ResponseEntity<Map> response = restTemplate.exchange(
                    baseUrl + "/api/v1/transaction/summary?userId=" + userId,
                    HttpMethod.GET, new HttpEntity<>(headers), Map.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody()).containsKey("FOOD");
            assertThat(response.getBody()).containsKey("TRANSPORT");
        }

        @Test
        @DisplayName("Should reject unsupported file type")
        void shouldRejectUnsupportedFileType() {
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("bank_statements", new ByteArrayResource("test".getBytes()) {
                @Override
                public String getFilename() { return "statement.pdf"; }
            });
            body.add("userId", userId);

            HttpHeaders headers = authHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            ResponseEntity<String> response = restTemplate.exchange(
                    baseUrl + "/api/v1/transaction/upload",
                    HttpMethod.POST, new HttpEntity<>(body, headers), String.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test
        @DisplayName("Should reject upload without userId")
        void shouldRejectUpload_WithoutUserId() {
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("bank_statements", new ByteArrayResource("test".getBytes()) {
                @Override
                public String getFilename() { return "statement.csv"; }
            });

            HttpHeaders headers = authHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            ResponseEntity<String> response = restTemplate.exchange(
                    baseUrl + "/api/v1/transaction/upload",
                    HttpMethod.POST, new HttpEntity<>(body, headers), String.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }
    }

    @Nested
    @DisplayName("Goal management flow")
    class GoalManagementFlowTests {

        @Test
        @DisplayName("Should create, read, update, and delete a spending goal")
        void shouldPerformFullGoalCrud() {
            HttpHeaders headers = authHeaders();

            // 1. Create
            SpendingGoal goal = new SpendingGoal();
            goal.setUserId(userId);
            goal.setCategory("FOOD");
            goal.setTargetAmount(new BigDecimal("5000"));
            goal.setPeriod("MONTHLY");
            goal.setStartDate(LocalDate.of(2026, 6, 1));
            goal.setActive(true);

            ResponseEntity<Map> createResponse = restTemplate.exchange(
                    baseUrl + "/api/v1/goals",
                    HttpMethod.POST,
                    new HttpEntity<>(goal, jsonHeaders()),
                    Map.class);

            assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            String goalId = (String) createResponse.getBody().get("id");

            // 2. Get all goals
            ResponseEntity<List> getResponse = restTemplate.exchange(
                    baseUrl + "/api/v1/goals?userId=" + userId,
                    HttpMethod.GET, new HttpEntity<>(headers), List.class);
            assertThat(getResponse.getBody()).isNotNull();
            assertThat(getResponse.getBody()).isNotEmpty();

            // 3. Get active goals
            ResponseEntity<List> activeResponse = restTemplate.exchange(
                    baseUrl + "/api/v1/goals/active?userId=" + userId,
                    HttpMethod.GET, new HttpEntity<>(headers), List.class);
            assertThat(activeResponse.getBody()).isNotNull();
            assertThat(activeResponse.getBody()).isNotEmpty();

            // 4. Update
            SpendingGoal update = new SpendingGoal();
            update.setTargetAmount(new BigDecimal("7000"));

            ResponseEntity<Map> updateResponse = restTemplate.exchange(
                    baseUrl + "/api/v1/goals/" + goalId,
                    HttpMethod.PUT,
                    new HttpEntity<>(update, jsonHeaders()),
                    Map.class);

            assertThat(updateResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(updateResponse.getBody().get("targetAmount")).isEqualTo(7000);

            // 5. Delete
            ResponseEntity<Void> deleteResponse = restTemplate.exchange(
                    baseUrl + "/api/v1/goals/" + goalId,
                    HttpMethod.DELETE, new HttpEntity<>(headers), Void.class);
            assertThat(deleteResponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

            // 6. Verify deletion
            ResponseEntity<List> afterDelete = restTemplate.exchange(
                    baseUrl + "/api/v1/goals?userId=" + userId,
                    HttpMethod.GET, new HttpEntity<>(headers), List.class);
            assertThat(afterDelete.getBody()).isEmpty();
        }

        @Test
        @DisplayName("Should get goal progress after creating goal and adding transactions")
        void shouldGetGoalProgress() {
            HttpHeaders headers = authHeaders();

            SpendingGoal goal = new SpendingGoal();
            goal.setUserId(userId);
            goal.setCategory("FOOD");
            goal.setTargetAmount(new BigDecimal("1000"));
            goal.setPeriod("MONTHLY");
            goal.setStartDate(LocalDate.of(2026, 6, 1));
            goal.setActive(true);

            ResponseEntity<Map> createResponse = restTemplate.exchange(
                    baseUrl + "/api/v1/goals",
                    HttpMethod.POST,
                    new HttpEntity<>(goal, jsonHeaders()),
                    Map.class);

            String goalId = (String) createResponse.getBody().get("id");

            String csvContent = "Date,Details,Debit,Credit,Balance\n"
                    + "01/06/2026,UPI/DR/SWIGGY/HDFC/swiggy@upi,400.00,,9600.00\n";
            uploadCsv(csvContent);

            ResponseEntity<Map> progressResponse = restTemplate.exchange(
                    baseUrl + "/api/v1/goals/" + goalId + "/progress",
                    HttpMethod.GET, new HttpEntity<>(headers), Map.class);

            assertThat(progressResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(progressResponse.getBody().get("category")).isEqualTo("FOOD");
            assertThat(((Number) progressResponse.getBody().get("percentageUsed")).doubleValue()).isEqualTo(40.0);
        }

        @Test
        @DisplayName("Should return 404 for non-existent goal")
        void shouldReturn404_ForNonExistentGoal() {
            HttpHeaders headers = authHeaders();

            ResponseEntity<String> progressResp = restTemplate.exchange(
                    baseUrl + "/api/v1/goals/nonexistent/progress",
                    HttpMethod.GET, new HttpEntity<>(headers), String.class);
            assertThat(progressResp.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

            ResponseEntity<String> deleteResp = restTemplate.exchange(
                    baseUrl + "/api/v1/goals/nonexistent",
                    HttpMethod.DELETE, new HttpEntity<>(headers), String.class);
            assertThat(deleteResp.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("Notification flow")
    class NotificationFlowTests {

        @Test
        @DisplayName("Should create, read, mark as read, and delete notifications")
        void shouldPerformFullNotificationCrud() {
            HttpHeaders headers = authHeaders();

            // 1. Create
            Notification notification = new Notification();
            notification.setUserId(userId);
            notification.setType("SYSTEM");
            notification.setTitle("Welcome");
            notification.setMessage("Welcome to Money Tracker!");

            ResponseEntity<Map> createResponse = restTemplate.exchange(
                    baseUrl + "/api/v1/notifications",
                    HttpMethod.POST,
                    new HttpEntity<>(notification, jsonHeaders()),
                    Map.class);

            assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            String notifId = (String) createResponse.getBody().get("id");

            // 2. Get all notifications
            ResponseEntity<List> getResponse = restTemplate.exchange(
                    baseUrl + "/api/v1/notifications?userId=" + userId,
                    HttpMethod.GET, new HttpEntity<>(headers), List.class);
            assertThat(getResponse.getBody()).isNotNull();
            assertThat(getResponse.getBody()).isNotEmpty();

            // 3. Get unread
            ResponseEntity<List> unreadResponse = restTemplate.exchange(
                    baseUrl + "/api/v1/notifications/unread?userId=" + userId,
                    HttpMethod.GET, new HttpEntity<>(headers), List.class);
            assertThat(unreadResponse.getBody()).isNotNull();
            assertThat(unreadResponse.getBody()).isNotEmpty();

            // 4. Get unread count
            ResponseEntity<Long> countResponse = restTemplate.exchange(
                    baseUrl + "/api/v1/notifications/unread/count?userId=" + userId,
                    HttpMethod.GET, new HttpEntity<>(headers), Long.class);
            assertThat(countResponse.getBody()).isEqualTo(1L);

            // 5. Mark as read
            ResponseEntity<Map> readResponse = restTemplate.exchange(
                    baseUrl + "/api/v1/notifications/" + notifId + "/read",
                    HttpMethod.PUT, new HttpEntity<>(headers), Map.class);
            assertThat(readResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(readResponse.getBody().get("read")).isEqualTo(true);

            // 6. Verify no more unread
            ResponseEntity<Long> afterReadCount = restTemplate.exchange(
                    baseUrl + "/api/v1/notifications/unread/count?userId=" + userId,
                    HttpMethod.GET, new HttpEntity<>(headers), Long.class);
            assertThat(afterReadCount.getBody()).isEqualTo(0L);

            // 7. Delete
            ResponseEntity<Void> deleteResponse = restTemplate.exchange(
                    baseUrl + "/api/v1/notifications/" + notifId,
                    HttpMethod.DELETE, new HttpEntity<>(headers), Void.class);
            assertThat(deleteResponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

            // 8. Verify deletion
            ResponseEntity<List> afterDelete = restTemplate.exchange(
                    baseUrl + "/api/v1/notifications?userId=" + userId,
                    HttpMethod.GET, new HttpEntity<>(headers), List.class);
            assertThat(afterDelete.getBody()).isEmpty();
        }

        @Test
        @DisplayName("Should create multiple notifications and mark all as read")
        void shouldCreateMultiple_AndMarkAllRead() {
            HttpHeaders headers = authHeaders();

            for (int i = 1; i <= 3; i++) {
                Notification n = new Notification();
                n.setUserId(userId);
                n.setType("BUDGET_TIP");
                n.setTitle("Tip " + i);
                n.setMessage("Message " + i);

                restTemplate.exchange(baseUrl + "/api/v1/notifications",
                        HttpMethod.POST,
                        new HttpEntity<>(n, jsonHeaders()),
                        Map.class);
            }

            ResponseEntity<Long> countResp = restTemplate.exchange(
                    baseUrl + "/api/v1/notifications/unread/count?userId=" + userId,
                    HttpMethod.GET, new HttpEntity<>(headers), Long.class);
            assertThat(countResp.getBody()).isEqualTo(3L);

            restTemplate.exchange(baseUrl + "/api/v1/notifications/read-all?userId=" + userId,
                    HttpMethod.PUT, new HttpEntity<>(headers), Void.class);

            ResponseEntity<List> unreadResp = restTemplate.exchange(
                    baseUrl + "/api/v1/notifications/unread?userId=" + userId,
                    HttpMethod.GET, new HttpEntity<>(headers), List.class);
            assertThat(unreadResp.getBody()).isEmpty();
        }
    }

    @Nested
    @DisplayName("Analytics flow")
    class AnalyticsFlowTests {

        @BeforeEach
        void uploadTestData() {
            String csvContent = "Date,Details,Debit,Credit,Balance\n"
                    + "01/06/2026,SALARY CREDIT XYZ CORP,,50000.00,50000.00\n"
                    + "02/06/2026,UPI/DR/SWIGGY/HDFC/swiggy@upi,500.00,,49500.00\n"
                    + "03/06/2026,UPI/DR/ZOMATO/ICICI/zomato@upi,300.00,,49200.00\n"
                    + "04/06/2026,UPI/DR/UBER/HDFC/uber@upi,800.00,,48400.00\n"
                    + "05/06/2026,UPI/DR/FLIPKART/HDFC/flipkart@upi,2500.00,,45900.00\n"
                    + "06/06/2026,UPI/DR/NETFLIX/HDFC/netflix@upi,699.00,,45201.00\n"
                    + "07/06/2026,UPI/DR/APOLLO/HDFC/pharmacy@upi,450.00,,44751.00\n"
                    + "08/06/2026,UPI/DR/RENT/HDFC/landlord@upi,15000.00,,29751.00\n";
            uploadCsv(csvContent);
        }

        @Test
        @DisplayName("Should return spending summary with correct totals")
        void shouldReturnSpendingSummary() {
            HttpHeaders headers = authHeaders();
            ResponseEntity<Map> response = restTemplate.exchange(
                    baseUrl + "/api/v1/analytics/summary?userId=" + userId,
                    HttpMethod.GET, new HttpEntity<>(headers), Map.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            Map<?, ?> summary = response.getBody();
            assertThat(((Number) summary.get("totalIncome")).doubleValue()).isEqualTo(50000.0);
            assertThat(((Number) summary.get("totalExpense")).doubleValue()).isEqualTo(20249.0);
            assertThat(((Number) summary.get("netSavings")).doubleValue()).isEqualTo(29751.0);
            assertThat(summary.get("topCategory")).isEqualTo("RENT");
            assertThat(summary.get("transactionCount")).isEqualTo(8);
        }

        @Test
        @DisplayName("Should return category breakdown with percentages")
        void shouldReturnCategoryBreakdown() {
            HttpHeaders headers = authHeaders();
            ResponseEntity<List> response = restTemplate.exchange(
                    baseUrl + "/api/v1/analytics/categories?userId=" + userId,
                    HttpMethod.GET, new HttpEntity<>(headers), List.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody()).isNotEmpty();

            Map<?, ?> first = (Map<?, ?>) response.getBody().get(0);
            assertThat(first.get("category")).isEqualTo("RENT");
        }

        @Test
        @DisplayName("Should return monthly trends")
        void shouldReturnMonthlyTrends() {
            HttpHeaders headers = authHeaders();
            ResponseEntity<List> response = restTemplate.exchange(
                    baseUrl + "/api/v1/analytics/trends?userId=" + userId + "&periods=3&period=MONTHLY",
                    HttpMethod.GET, new HttpEntity<>(headers), List.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody()).hasSize(3);
        }

        @Test
        @DisplayName("Should return budget status")
        void shouldReturnBudgetStatus() {
            HttpHeaders headers = authHeaders();
            ResponseEntity<List> response = restTemplate.exchange(
                    baseUrl + "/api/v1/analytics/budget?userId=" + userId,
                    HttpMethod.GET, new HttpEntity<>(headers), List.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody()).isNotEmpty();
        }

        @Test
        @DisplayName("Should return bad request for analytics without userId")
        void shouldReturnBadRequest_WithoutUserId() {
            HttpHeaders headers = authHeaders();
            ResponseEntity<String> resp = restTemplate.exchange(
                    baseUrl + "/api/v1/analytics/summary",
                    HttpMethod.GET, new HttpEntity<>(headers), String.class);
            assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }
    }

    @Nested
    @DisplayName("Complete end-to-end user journey")
    class EndToEndJourneyTests {

        @Test
        @DisplayName("Should complete full journey: register → upload → goals → analytics → notifications")
        void shouldCompleteFullJourney() {
            HttpHeaders headers = authHeaders();

            // Step 1: Upload a bank statement
            String csvContent = "Date,Details,Debit,Credit,Balance\n"
                    + "01/06/2026,SALARY CREDIT XYZ CORP,,50000.00,50000.00\n"
                    + "02/06/2026,UPI/DR/SWIGGY/HDFC/swiggy@upi,500.00,,49500.00\n"
                    + "03/06/2026,UPI/DR/RENT/HDFC/landlord@upi,15000.00,,34500.00\n";
            uploadCsv(csvContent);

            // Step 2: Verify transactions
            ResponseEntity<List> txResp = restTemplate.exchange(
                    baseUrl + "/api/v1/transaction?userId=" + userId,
                    HttpMethod.GET, new HttpEntity<>(headers), List.class);
            assertThat(txResp.getBody()).hasSize(3);

            // Step 3: Create a FOOD budget goal
            SpendingGoal foodGoal = new SpendingGoal();
            foodGoal.setUserId(userId);
            foodGoal.setCategory("FOOD");
            foodGoal.setTargetAmount(new BigDecimal("5000"));
            foodGoal.setPeriod("MONTHLY");
            foodGoal.setStartDate(LocalDate.of(2026, 6, 1));
            foodGoal.setActive(true);

            ResponseEntity<Map> goalResp = restTemplate.exchange(
                    baseUrl + "/api/v1/goals",
                    HttpMethod.POST,
                    new HttpEntity<>(foodGoal, jsonHeaders()),
                    Map.class);
            String goalId = (String) goalResp.getBody().get("id");

            // Step 4: Check analytics
            ResponseEntity<Map> analyticsResp = restTemplate.exchange(
                    baseUrl + "/api/v1/analytics/summary?userId=" + userId,
                    HttpMethod.GET, new HttpEntity<>(headers), Map.class);
            assertThat(((Number) analyticsResp.getBody().get("totalIncome")).doubleValue()).isEqualTo(50000.0);

            // Step 5: Check category breakdown
            ResponseEntity<List> breakdownResp = restTemplate.exchange(
                    baseUrl + "/api/v1/analytics/categories?userId=" + userId,
                    HttpMethod.GET, new HttpEntity<>(headers), List.class);
            assertThat(breakdownResp.getBody()).isNotEmpty();

            // Step 6: Check notifications
            ResponseEntity<List> notifResp = restTemplate.exchange(
                    baseUrl + "/api/v1/notifications?userId=" + userId,
                    HttpMethod.GET, new HttpEntity<>(headers), List.class);
            assertThat(notifResp.getBody()).isNotNull();

            // Step 7: Check goal progress
            ResponseEntity<Map> progressResp = restTemplate.exchange(
                    baseUrl + "/api/v1/goals/" + goalId + "/progress",
                    HttpMethod.GET, new HttpEntity<>(headers), Map.class);
            assertThat(progressResp.getBody().get("category")).isEqualTo("FOOD");
            assertThat(((Number) progressResp.getBody().get("percentageUsed")).doubleValue()).isEqualTo(10.0);

            // Step 8: Upload more transactions that exceed FOOD goal
            String moreCsv = "Date,Details,Debit,Credit,Balance\n"
                    + "10/06/2026,UPI/DR/SWIGGY/HDFC/swiggy@upi,2000.00,,32500.00\n"
                    + "11/06/2026,UPI/DR/ZOMATO/ICICI/zomato@upi,3000.00,,29500.00\n";
            uploadCsv(moreCsv);

            ResponseEntity<Map> exceededResp = restTemplate.exchange(
                    baseUrl + "/api/v1/goals/" + goalId + "/progress",
                    HttpMethod.GET, new HttpEntity<>(headers), Map.class);
            assertThat(((Number) exceededResp.getBody().get("percentageUsed")).doubleValue()).isEqualTo(110.0);
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

    private void uploadCsv(String csvContent) {
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("bank_statements", new ByteArrayResource(csvContent.getBytes()) {
            @Override
            public String getFilename() { return "statement.csv"; }
        });
        body.add("userId", userId);

        HttpHeaders headers = authHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        restTemplate.exchange(baseUrl + "/api/v1/transaction/upload",
                HttpMethod.POST, new HttpEntity<>(body, headers), Void.class);
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
