package com.tracker.MoneyTracker.ai.client;

/**
 * Abstraction over the underlying LLM provider.
 * <p>
 * The application code (chat, recommendations, affordability, etc.) depends
 * only on this interface.  At runtime Spring injects either
 * {@code OllamaClient} or {@code GroqClient} depending on the configured
 * {@code ai.provider} property.
 */
public interface AiLlmClient {

    /**
     * Sends a prompt to the configured LLM and returns the generated text.
     *
     * @param prompt the full prompt to send
     * @return the LLM's text response
     */
    String generate(String prompt);
}
