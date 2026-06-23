package com.tracker.MoneyTracker.ai.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * HTTP client for communicating with a locally-hosted Ollama LLM.
 * <p>
 * Ollama exposes a REST API at {@code /api/generate} that accepts a prompt
 * and returns a generated text response. This client sends a non-streaming
 * request and extracts the response text.
 * <p>
 * The client is intentionally simple — no retry logic, no streaming — to
 * keep the MVP straightforward and easy to test.
 * <p>
 * Not a {@code @Component} — instantiated by {@code AiLlmConfig} so that
 * only the selected implementation is exposed as an {@code AiLlmClient} bean.
 */
public class OllamaClient implements AiLlmClient {

    private static final Logger log = LoggerFactory.getLogger(OllamaClient.class);
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;

    @Value("${ollama.base-url:http://localhost:11434}")
    private String baseUrl = "http://localhost:11434";

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    @Value("${ollama.model:llama3.2}")
    private String model = "llama3.2";

    public void setModel(String model) {
        this.model = model;
    }

    @Value("${ollama.temperature:0.7}")
    private double temperature = 0.7;

    public void setTemperature(double temperature) {
        this.temperature = temperature;
    }

    @Value("${ollama.max-tokens:2048}")
    private int maxTokens = 2048;

    public void setMaxTokens(int maxTokens) {
        this.maxTokens = maxTokens;
    }

    public OllamaClient() {
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
                .build();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Sends a prompt to the Ollama LLM and returns the generated text.
     *
     * @param prompt the full prompt to send
     * @return the LLM's text response
     * @throws OllamaException if the request fails or Ollama returns an error
     */
    @Override
    public String generate(String prompt) {
        log.debug("Sending prompt to Ollama (model={}): {}", model,
                prompt.substring(0, Math.min(100, prompt.length())));

        Map<String, Object> options = new HashMap<>();
        options.put("temperature", temperature);
        options.put("num_predict", maxTokens);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", model);
        requestBody.put("prompt", prompt);
        requestBody.put("stream", false);
        requestBody.put("options", options);

        String json;
        try {
            json = objectMapper.writeValueAsString(requestBody);
        } catch (Exception e) {
            throw new OllamaException("Failed to serialize Ollama request", e);
        }

        Request request = new Request.Builder()
                .url(baseUrl + "/api/generate")
                .post(RequestBody.create(json, JSON))
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String body = response.body() != null ? response.body().string() : "empty";
                log.error("Ollama returned HTTP {}: {}", response.code(), body);
                throw new OllamaException("Ollama returned HTTP " + response.code() + ": " + body);
            }

            String responseBody = response.body() != null ? response.body().string() : "";
            JsonNode root = objectMapper.readTree(responseBody);

            if (root.has("error")) {
                throw new OllamaException("Ollama error: " + root.get("error").asText());
            }

            return root.path("response").asText();
        } catch (IOException e) {
            log.error("Failed to communicate with Ollama at {}", baseUrl, e);
            throw new OllamaException("Failed to connect to Ollama at " + baseUrl, e);
        }
    }

    /**
     * Exception thrown when Ollama communication fails.
     */
    public static class OllamaException extends RuntimeException {
        public OllamaException(String message) {
            super(message);
        }

        public OllamaException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
