package com.tracker.MoneyTracker.ai.controller;

import com.tracker.MoneyTracker.ai.dto.*;
import com.tracker.MoneyTracker.ai.entity.AiChatMessage;
import com.tracker.MoneyTracker.ai.repository.AiChatMessageRepository;
import com.tracker.MoneyTracker.ai.service.AffordabilityService;
import com.tracker.MoneyTracker.ai.service.AiChatService;
import com.tracker.MoneyTracker.ai.service.HealthScoreService;
import com.tracker.MoneyTracker.ai.service.RecommendationService;
import com.tracker.MoneyTracker.exception.BadRequestException;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AiCoachController")
class AiCoachControllerTest {

    @Mock
    private AiChatService chatService;

    @Mock
    private RecommendationService recommendationService;

    @Mock
    private HealthScoreService healthScoreService;

    @Mock
    private AffordabilityService affordabilityService;

    @Mock
    private AiChatMessageRepository chatMessageRepository;

    private AiCoachController sut;

    @BeforeEach
    void setUp() {
        sut = new AiCoachController(chatService, recommendationService, healthScoreService, affordabilityService, chatMessageRepository);
    }

    @Nested
    @DisplayName("POST /chat")
    class ChatEndpointTests {

        @Test
        @DisplayName("Should return 200 with chat response when valid request")
        void shouldReturn200_WhenValidChatRequest() {
            // Arrange
            ChatRequest request = new ChatRequest("user-123", "How can I save more?");
            ChatResponse expected = new ChatResponse("user-123", "How can I save more?",
                    "Reduce dining out.", LocalDateTime.now());
            when(chatService.chat("user-123", "How can I save more?")).thenReturn(expected);

            // Act
            ResponseEntity<ChatResponse> response = sut.chat(request);

            // Assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isEqualTo(expected);
        }

        @Test
        @DisplayName("Should return 400 when userId is null")
        void shouldReturn400_WhenUserIdIsNull() {
            ChatRequest request = new ChatRequest(null, "Hello");

            assertThatThrownBy(() -> sut.chat(request))
                    .isInstanceOf(BadRequestException.class);
        }

        @Test
        @DisplayName("Should return 400 when message is blank")
        void shouldReturn400_WhenMessageIsBlank() {
            ChatRequest request = new ChatRequest("user-123", "  ");

            assertThatThrownBy(() -> sut.chat(request))
                    .isInstanceOf(BadRequestException.class);
        }
    }

    @Nested
    @DisplayName("GET /recommendations")
    class RecommendationsEndpointTests {

        @Test
        @DisplayName("Should return 200 with recommendations")
        void shouldReturn200_WithRecommendations() {
            // Arrange
            RecommendationResponse expected = new RecommendationResponse(
                    "user-123", List.of("Save 20% more", "Cancel subscriptions"));
            when(recommendationService.generateRecommendations("user-123")).thenReturn(expected);

            // Act
            ResponseEntity<RecommendationResponse> response = sut.getRecommendations("user-123");

            // Assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isEqualTo(expected);
        }

        @Test
        @DisplayName("Should return 400 when userId is blank")
        void shouldReturn400_WhenUserIdIsBlank() {
            assertThatThrownBy(() -> sut.getRecommendations(""))
                    .isInstanceOf(BadRequestException.class);
        }
    }

    @Nested
    @DisplayName("GET /health-score")
    class HealthScoreEndpointTests {

        @Test
        @DisplayName("Should return 200 with health score")
        void shouldReturn200_WithHealthScore() {
            // Arrange
            HealthScoreResponse expected = new HealthScoreResponse(
                    "user-123", 82, 25.0, 60.0, "Good financial health");
            when(healthScoreService.calculateScore("user-123")).thenReturn(expected);

            // Act
            ResponseEntity<HealthScoreResponse> response = sut.getHealthScore("user-123");

            // Assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isEqualTo(expected);
        }

        @Test
        @DisplayName("Should return 400 when userId is null")
        void shouldReturn400_WhenUserIdIsNull() {
            assertThatThrownBy(() -> sut.getHealthScore(null))
                    .isInstanceOf(BadRequestException.class);
        }
    }

    @Nested
    @DisplayName("POST /affordability")
    class AffordabilityEndpointTests {

        @Test
        @DisplayName("Should return 200 with affordability analysis")
        void shouldReturn200_WithAffordabilityAnalysis() {
            // Arrange
            AffordabilityRequest request = new AffordabilityRequest("user-123", "PS5", BigDecimal.valueOf(45000));
            AffordabilityResponse expected = new AffordabilityResponse(
                    "user-123", "PS5", BigDecimal.valueOf(45000), true, "You can afford this.");
            when(affordabilityService.analyze("user-123", "PS5", BigDecimal.valueOf(45000)))
                    .thenReturn(expected);

            // Act
            ResponseEntity<AffordabilityResponse> response = sut.analyzeAffordability(request);

            // Assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isEqualTo(expected);
        }

        @Test
        @DisplayName("Should return 400 when cost is negative")
        void shouldReturn400_WhenCostIsNegative() {
            AffordabilityRequest request = new AffordabilityRequest("user-123", "PS5", BigDecimal.valueOf(-100));

            assertThatThrownBy(() -> sut.analyzeAffordability(request))
                    .isInstanceOf(BadRequestException.class);
        }

        @Test
        @DisplayName("Should return 400 when itemName is blank")
        void shouldReturn400_WhenItemNameIsBlank() {
            AffordabilityRequest request = new AffordabilityRequest("user-123", "", BigDecimal.valueOf(1000));

            assertThatThrownBy(() -> sut.analyzeAffordability(request))
                    .isInstanceOf(BadRequestException.class);
        }
    }

    @Nested
    @DisplayName("GET /history")
    class HistoryEndpointTests {

        @Test
        @DisplayName("Should return 200 with chat history list")
        void shouldReturn200_WithChatHistory() {
            // Arrange
            AiChatMessage msg1 = new AiChatMessage();
            msg1.setId("msg-1");
            msg1.setUserId("user-123");
            msg1.setRole("USER");
            msg1.setContent("Hello");
            msg1.setCreatedAt(LocalDateTime.now().minusMinutes(5));

            AiChatMessage msg2 = new AiChatMessage();
            msg2.setId("msg-2");
            msg2.setUserId("user-123");
            msg2.setRole("ASSISTANT");
            msg2.setContent("Hi there!");
            msg2.setCreatedAt(LocalDateTime.now().minusMinutes(4));

            when(chatMessageRepository.findByUserIdOrderByCreatedAtAsc("user-123"))
                    .thenReturn(List.of(msg1, msg2));

            // Act
            ResponseEntity<List<ChatHistoryItem>> response = sut.getChatHistory("user-123");

            // Assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).hasSize(2);
            assertThat(response.getBody().get(0).role()).isEqualTo("USER");
            assertThat(response.getBody().get(1).role()).isEqualTo("ASSISTANT");
        }

        @Test
        @DisplayName("Should return empty list when no history exists")
        void shouldReturnEmptyList_WhenNoHistory() {
            when(chatMessageRepository.findByUserIdOrderByCreatedAtAsc("user-123"))
                    .thenReturn(List.of());

            ResponseEntity<List<ChatHistoryItem>> response = sut.getChatHistory("user-123");

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isEmpty();
        }

        @Test
        @DisplayName("Should return 400 when userId is blank")
        void shouldReturn400_WhenUserIdIsBlank() {
            assertThatThrownBy(() -> sut.getChatHistory(""))
                    .isInstanceOf(BadRequestException.class);
        }

        @Test
        @DisplayName("Should return 400 when userId is null")
        void shouldReturn400_WhenUserIdIsNull() {
            assertThatThrownBy(() -> sut.getChatHistory(null))
                    .isInstanceOf(BadRequestException.class);
        }
    }
}
