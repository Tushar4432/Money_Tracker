package com.tracker.MoneyTracker.ai.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * HTTP client for communicating with OpenAI-compatible LLM APIs, specifically OpenRouter.
 * <p>
 * Configuration properties:
 * <ul>
 *   <li>{@code openrouter.api-key} — API key</li>
 *   <li>{@code openrouter.base-url} — API base URL, defaults to {@code https://openrouter.ai/api/v1}</li>
 *   <li>{@code openrouter.model} — Model ID</li>
 *   <li>{@code openrouter.temperature} — Sampling temperature, defaults to {@code 0.7}</li>
 *   <li>{@code openrouter.max-tokens} — Max tokens in response, defaults to {@code 1024}</li>
 * </ul>
 */
public class OpenRouterClient implements AiLlmClient {

    private static final Logger log = LoggerFactory.getLogger(OpenRouterClient.class);
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;

    @Value("${openrouter.api-key:}")
    private String apiKey;

    @Value("${openrouter.base-url:https://openrouter.ai/api/v1}")
    private String baseUrl = "https://openrouter.ai/api/v1";

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    @Value("${openrouter.model:llama-3.3-70b-versatile}")
    private String model = "llama-3.3-70b-versatile";

    public void setModel(String model) {
        this.model = model;
    }

    @Value("${openrouter.temperature:0.7}")
    private double temperature = 0.7;

    public void setTemperature(double temperature) {
        this.temperature = temperature;
    }

    @Value("${openrouter.max-tokens:1024}")
    private int maxTokens = 1024;

    public void setMaxTokens(int maxTokens) {
        this.maxTokens = maxTokens;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public OpenRouterClient() {
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(120, TimeUnit.SECONDS)
                .writeTimeout(15, TimeUnit.SECONDS)
                .build();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Sends a prompt to the configured LLM and returns the generated text.
     *
     * @param prompt the full prompt to send
     * @return the LLM's text response
     * @throws OpenRouterException if the request fails or the API returns an error
     */
    @Override
    public String generate(String prompt) {
        log.debug("Sending prompt to LLM at {} (model={}): {}", baseUrl, model,
                prompt.substring(0, Math.min(100, prompt.length())));

        Map<String, Object> userMessage = new HashMap<>();
        userMessage.put("role", "user");
        userMessage.put("content", prompt);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", model);
        requestBody.put("messages", List.of(userMessage));
        requestBody.put("temperature", temperature);
        requestBody.put("max_tokens", maxTokens);
        requestBody.put("stream", false);

        String json;
        try {
            json = objectMapper.writeValueAsString(requestBody);
        } catch (Exception e) {
            throw new OpenRouterException("Failed to serialize request", e);
        }

        // Determine the chat completions path based on the base URL.
        // Strip trailing slash if present.
        String cleanBaseUrl = baseUrl;
        if (cleanBaseUrl.endsWith("/")) {
            cleanBaseUrl = cleanBaseUrl.substring(0, cleanBaseUrl.length() - 1);
        }

        String completionsPath;
        if (cleanBaseUrl.contains("api.x.ai") || cleanBaseUrl.contains("openrouter.ai") || cleanBaseUrl.endsWith("/v1")) {
            completionsPath = "/chat/completions";
        } else {
            completionsPath = "/openai/v1/chat/completions";
        }
        String fullUrl = cleanBaseUrl + completionsPath;

        Request.Builder requestBuilder = new Request.Builder()
                .url(fullUrl)
                .post(RequestBody.create(json, JSON))
                .addHeader("Authorization", "Bearer " + apiKey);

        // OpenRouter recommended headers for analytics and metadata
        if (cleanBaseUrl.contains("openrouter.ai")) {
            requestBuilder.addHeader("HTTP-Referer", "https://github.com/Tushar4432/Money_Tracker");
            requestBuilder.addHeader("X-Title", "Money Tracker");
        }

        Request request = requestBuilder.build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String body = response.body() != null ? response.body().string() : "empty";
                log.error("LLM returned HTTP {}: {}", response.code(), body);
                throw new OpenRouterException("LLM returned HTTP " + response.code() + ": " + body);
            }

            String responseBody = response.body() != null ? response.body().string() : "";
            JsonNode root = objectMapper.readTree(responseBody);

            if (root.has("error")) {
                throw new OpenRouterException("LLM error: " + root.get("error").asText());
            }

            // Extract text from OpenAI-style response: choices[0].message.content
            JsonNode choices = root.path("choices");
            if (choices.isArray() && !choices.isEmpty()) {
                String text = choices.get(0).path("message").path("content").asText();
                log.debug("LLM response: {}", text.substring(0, Math.min(100, text.length())));
                return text;
            }

            throw new OpenRouterException("LLM returned no choices in response: " + responseBody);
        } catch (IOException e) {
            log.error("Failed to communicate with LLM at {}", baseUrl, e);
            throw new OpenRouterException("Failed to connect to LLM at " + baseUrl, e);
        }
    }

    /**
     * Exception thrown when LLM communication fails.
     */
    public static class OpenRouterException extends RuntimeException {
        public OpenRouterException(String message) {
            super(message);
        }

        public OpenRouterException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
