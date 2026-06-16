package com.tracker.MoneyTracker.ai.service;

import com.tracker.MoneyTracker.ai.client.OllamaClient;
import com.tracker.MoneyTracker.ai.dto.ChatResponse;
import com.tracker.MoneyTracker.ai.entity.AiChatMessage;
import com.tracker.MoneyTracker.ai.repository.AiChatMessageRepository;
import com.tracker.MoneyTracker.analytics.AnalyticsService;
import com.tracker.MoneyTracker.analytics.dto.SpendingSummary;
import com.tracker.MoneyTracker.goal.GoalService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("AiChatService")
class AiChatServiceTest {

    @Mock
    private OllamaClient ollamaClient;

    @Mock
    private AiChatMessageRepository chatRepository;

    @Mock
    private AnalyticsService analyticsService;

    @Mock
    private GoalService goalService;

    private AiChatService sut;

    @BeforeEach
    void setUp() {
        sut = new AiChatService(ollamaClient, chatRepository, analyticsService, goalService);

        // Stub common dependencies used during prompt building
        when(analyticsService.getSpendingSummary(any())).thenReturn(
                new SpendingSummary(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, "N/A", 0));
        when(goalService.evaluateAllGoals(any())).thenReturn(List.of());
        when(chatRepository.findTop20ByUserIdOrderByCreatedAtDesc(any())).thenReturn(List.of());
        when(chatRepository.save(any(AiChatMessage.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Nested
    @DisplayName("chat")
    class ChatTests {

        @Test
        @DisplayName("Should return AI reply when valid question provided")
        void shouldReturnAiReply_WhenValidQuestion() {
            // Arrange
            String userId = "user-123";
            String question = "How can I save more money?";
            String aiReply = "You should reduce dining out expenses by 20%.";

            when(ollamaClient.generate(anyString())).thenReturn(aiReply);

            // Act
            ChatResponse response = sut.chat(userId, question);

            // Assert
            assertThat(response.userId()).isEqualTo(userId);
            assertThat(response.message()).isEqualTo(question);
            assertThat(response.reply()).isEqualTo(aiReply);
            assertThat(response.createdAt()).isNotNull();
        }

        @Test
        @DisplayName("Should save both user message and AI response")
        void shouldSaveBothMessages_AndAiResponse() {
            // Arrange
            String userId = "user-123";
            String question = "Can I afford a PS5?";
            when(ollamaClient.generate(anyString())).thenReturn("Yes, with planning.");

            // Act
            sut.chat(userId, question);

            // Assert — 2 saves: user message + assistant response
            ArgumentCaptor<AiChatMessage> captor = ArgumentCaptor.forClass(AiChatMessage.class);
            verify(chatRepository, times(2)).save(captor.capture());

            List<AiChatMessage> saved = captor.getAllValues();
            assertThat(saved).hasSize(2);
            assertThat(saved.get(0).getRole()).isEqualTo("USER");
            assertThat(saved.get(0).getContent()).isEqualTo(question);
            assertThat(saved.get(1).getRole()).isEqualTo("ASSISTANT");
            assertThat(saved.get(1).getContent()).isEqualTo("Yes, with planning.");
        }

        @Test
        @DisplayName("Should include spending context in the prompt sent to Ollama")
        void shouldIncludeSpendingContext_InPrompt() {
            // Arrange
            String userId = "user-456";
            SpendingSummary summary = new SpendingSummary(
                    BigDecimal.valueOf(60000), BigDecimal.valueOf(45000),
                    BigDecimal.valueOf(15000), "Shopping", 100);
            when(analyticsService.getSpendingSummary(userId)).thenReturn(summary);
            when(ollamaClient.generate(anyString())).thenReturn("Reduce shopping expenses.");

            // Act
            sut.chat(userId, "How do I save more?");

            // Assert
            ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
            verify(ollamaClient).generate(captor.capture());

            String prompt = captor.getValue();
            assertThat(prompt).contains("60000");   // income
            assertThat(prompt).contains("Shopping"); // top category
            assertThat(prompt).contains("financial coach");
        }

        @Test
        @DisplayName("Should throw OllamaException when Ollama is unavailable")
        void shouldThrowOllamaException_WhenOllamaUnavailable() {
            // Arrange
            when(ollamaClient.generate(anyString()))
                    .thenThrow(new OllamaClient.OllamaException("Connection refused"));

            // Act & Assert
            assertThatThrownBy(() -> sut.chat("user-123", "Hello"))
                    .isInstanceOf(OllamaClient.OllamaException.class)
                    .hasMessageContaining("Connection refused");
        }
    }
}
