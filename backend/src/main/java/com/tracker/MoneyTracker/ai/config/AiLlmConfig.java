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

    @Value("${groq.api-key:}")
    private String groqApiKey;

    @Value("${groq.base-url:https://api.groq.com}")
    private String groqBaseUrl;

    @Value("${groq.model:llama-3.3-70b-versatile}")
    private String groqModel;

    @Value("${groq.temperature:0.7}")
    private double groqTemperature;

    @Value("${groq.max-tokens:1024}")
    private int groqMaxTokens;

    @Bean
    public AiLlmClient aiLlmClient() {
        if ("groq".equalsIgnoreCase(provider)) {
            log.info("AI provider: Groq (model={})", groqModel);
            GroqClient client = new GroqClient();
            client.setApiKey(groqApiKey);
            client.setBaseUrl(groqBaseUrl);
            client.setModel(groqModel);
            client.setTemperature(groqTemperature);
            client.setMaxTokens(groqMaxTokens);
            return client;
        }
        if ("xai".equalsIgnoreCase(provider)) {
            log.info("AI provider: x.ai / OpenRouter (model={}, url={})", groqModel, groqBaseUrl);
            GroqClient client = new GroqClient();
            client.setApiKey(groqApiKey);
            client.setBaseUrl(groqBaseUrl);
            client.setModel(groqModel);
            client.setTemperature(groqTemperature);
            client.setMaxTokens(groqMaxTokens);
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
