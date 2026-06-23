package com.tracker.MoneyTracker.ai.config;

import com.tracker.MoneyTracker.ai.client.AiLlmClient;
import com.tracker.MoneyTracker.ai.client.GroqClient;
import com.tracker.MoneyTracker.ai.client.OllamaClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Selects the active LLM client based on the {@code ai.provider} property.
 * <p>
 * <ul>
 *   <li>{@code ai.provider=ollama} (default) — uses {@link OllamaClient}</li>
 *   <li>{@code ai.provider=groq} — uses {@link GroqClient} (Groq Cloud)</li>
 *   <li>{@code ai.provider=xai} — uses {@link GroqClient} pointed at x.ai (Grok)</li>
 * </ul>
 * <p>
 * Both Groq and x.ai use the same OpenAI-compatible chat completions protocol,
 * so {@link GroqClient} handles both. The difference is only the base URL and
 * model name configured in the respective profile.
 */
@Configuration
public class AiLlmConfig {

    private static final Logger log = LoggerFactory.getLogger(AiLlmConfig.class);

    @Value("${ai.provider:ollama}")
    private String provider;

    @Bean
    public AiLlmClient aiLlmClient(OllamaClient ollamaClient, GroqClient groqClient) {
        if ("groq".equalsIgnoreCase(provider)) {
            log.info("AI provider: Groq (model configured in groq.model)");
            return groqClient;
        }
        if ("xai".equalsIgnoreCase(provider)) {
            log.info("AI provider: x.ai / Grok (model configured in xai.model, reusing GroqClient)");
            return groqClient;
        }
        log.info("AI provider: Ollama (model configured in ollama.model)");
        return ollamaClient;
    }
}
