package com.tracker.MoneyTracker.ai.client;

import org.junit.jupiter.api.*;

import static org.assertj.core.api.Assertions.*;

@DisplayName("OpenRouterClient")
class OpenRouterClientTest {

    @Test
    @DisplayName("Should throw OpenRouterException when OpenRouter is not reachable or API key is invalid")
    void shouldThrowOpenRouterException_WhenApiFails() {
        OpenRouterClient client = new OpenRouterClient();
        // Use a port where nothing is listening to force a connection failure
        client.setBaseUrl("http://localhost:19999");
        client.setApiKey("invalid-key");

        assertThatThrownBy(() -> client.generate("test prompt"))
                .isInstanceOf(OpenRouterClient.OpenRouterException.class);
    }
}
