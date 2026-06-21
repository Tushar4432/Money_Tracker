package com.tracker.MoneyTracker.ai.client;

import org.junit.jupiter.api.*;

import static org.assertj.core.api.Assertions.*;

@DisplayName("OllamaClient")
class OllamaClientTest {

    @Test
    @DisplayName("Should throw OllamaException when Ollama is not running")
    void shouldThrowOllamaException_WhenOllamaNotRunning() {
        OllamaClient client = new OllamaClient();
        // Use a port where Ollama is definitely not running
        client.setBaseUrl("http://localhost:19999");

        assertThatThrownBy(() -> client.generate("test prompt"))
                .isInstanceOf(OllamaClient.OllamaException.class);
    }
}
