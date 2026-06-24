package com.tracker.MoneyTracker.ai.config;

import com.tracker.MoneyTracker.ai.client.AiLlmClient;
import com.tracker.MoneyTracker.ai.client.OpenRouterClient;
import com.tracker.MoneyTracker.ai.client.OllamaClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Factory that creates the appropriate {@link AiLlmClient} based on the
 * {@code ai.provider} property.
 * <p>
 * <ul>
 *   <li>{@code ai.provider=ollama} (default) — uses {@link OllamaClient}</li>
 *   <li>{@code ai.provider=groq} — uses {@link GroqClient} (Groq Cloud)</li>
 *   <li>{@code ai.provider=xai} — uses {@link GroqClient} pointed at x.ai / OpenRouter</li>
 * </ul>
 * <p>
 * The concrete clients are <strong>not</strong> {@code @Component}s — they
 * are instantiated solely through this factory. This guarantees a single
 * {@code AiLlmClient} bean and avoids accidental double-registration.
 */
@Configuration
public class AiLlmConfig {

    private static final Logger log = LoggerFactory.getLogger(AiLlmConfig.class);

    @Value("${ai.provider:ollama}")
    private String provider;

    @Value("${ollama.base-url:http://localhost:11434}")
    private String ollamaBaseUrl;

    @Value("${ollama.model:llama3.2}")
    private String ollamaModel;

    @Value("${ollama.temperature:0.7}")
    private double ollamaTemperature;

    @Value("${ollama.max-tokens:2048}")
    private int ollamaMaxTokens;

    @Value("${openrouter.api-key:}")
    private String openRouterApiKey;

    @Value("${openrouter.base-url:https://openrouter.ai/api/v1}")
    private String openRouterBaseUrl;

    @Value("${openrouter.model:llama-3.3-70b-versatile}")
    private String openRouterModel;

    @Value("${openrouter.temperature:0.7}")
    private double openRouterTemperature;

    @Value("${openrouter.max-tokens:1024}")
    private int openRouterMaxTokens;

    @Bean
    public AiLlmClient aiLlmClient() {
        if ("openrouter".equalsIgnoreCase(provider) || "groq".equalsIgnoreCase(provider) || "xai".equalsIgnoreCase(provider)) {
            log.info("AI provider: OpenRouter (model={}, url={})", openRouterModel, openRouterBaseUrl);
            OpenRouterClient client = new OpenRouterClient();
            client.setApiKey(openRouterApiKey);
            client.setBaseUrl(openRouterBaseUrl);
            client.setModel(openRouterModel);
            client.setTemperature(openRouterTemperature);
            client.setMaxTokens(openRouterMaxTokens);
            return client;
        }
        log.info("AI provider: Ollama (model={})", ollamaModel);
        OllamaClient client = new OllamaClient();
        client.setBaseUrl(ollamaBaseUrl);
        client.setModel(ollamaModel);
        client.setTemperature(ollamaTemperature);
        client.setMaxTokens(ollamaMaxTokens);
        return client;
    }
}
