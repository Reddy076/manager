package com.revature.passwordmanager.service.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.revature.passwordmanager.config.LlmConfig;
import com.revature.passwordmanager.exception.LlmCommunicationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Slf4j
@Service
@RequiredArgsConstructor
public class LlmClientService {

    private final LlmConfig config;
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;

    /**
     * Send prompt to Ollama API and get completion.
     * Enforces JSON format automatically.
     */
    public String generateCompletion(String systemPrompt, String userPrompt) {
        String url = config.getBaseUrl() + "/api/generate";

        // Build JSON payload
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("model", config.getModel());
        
        // Ollama specific format: system prompt needs to be provided if using /api/generate
        payload.put("system", systemPrompt);
        payload.put("prompt", userPrompt);
        payload.put("stream", false);
        payload.put("format", "json");
        
        ObjectNode options = payload.putObject("options");
        options.put("temperature", config.getTemperature());

        try {
            RequestBody body = RequestBody.create(
                    objectMapper.writeValueAsString(payload),
                    MediaType.parse("application/json")
            );

            Request request = new Request.Builder()
                    .url(url)
                    .post(body)
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    log.error("Ollama API failed with status: {}", response.code());
                    throw new LlmCommunicationException("Failed to generate completion: " + response.code());
                }

                if (response.body() == null) {
                    throw new LlmCommunicationException("Empty response body from Ollama");
                }

                String responseBodyStr = response.body().string();
                JsonNode responseNode = objectMapper.readTree(responseBodyStr);
                return responseNode.path("response").asText();
            }
        } catch (IOException e) {
            log.error("Error communicating with Ollama API", e);
            throw new LlmCommunicationException("Error communicating with LLM service", e);
        }
    }

    /**
     * Send prompt to Ollama API and get completion in plain text (no JSON formatting enforced).
     * Useful for conversational chatbots.
     */
    public String generateText(String systemPrompt, String userPrompt) {
        String url = config.getBaseUrl() + "/api/generate";

        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("model", config.getModel());
        
        payload.put("system", systemPrompt);
        payload.put("prompt", userPrompt);
        payload.put("stream", false);
        
        ObjectNode options = payload.putObject("options");
        options.put("temperature", config.getTemperature());

        try {
            RequestBody body = RequestBody.create(
                    objectMapper.writeValueAsString(payload),
                    MediaType.parse("application/json")
            );

            Request request = new Request.Builder()
                    .url(url)
                    .post(body)
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    log.error("Ollama API failed with status: {}", response.code());
                    throw new LlmCommunicationException("Failed to generate text: " + response.code());
                }

                if (response.body() == null) {
                    throw new LlmCommunicationException("Empty response body from Ollama");
                }

                String responseBodyStr = response.body().string();
                JsonNode responseNode = objectMapper.readTree(responseBodyStr);
                return responseNode.path("response").asText();
            }
        } catch (IOException e) {
            log.error("Error communicating with Ollama API", e);
            throw new LlmCommunicationException("Error communicating with LLM service", e);
        }
    }

    /**
     * Checks if Ollama responds on its base URL
     */
    public boolean isHealthy() {
        Request request = new Request.Builder()
                .url(config.getBaseUrl())
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            return response.isSuccessful() || response.code() == 200;
        } catch (IOException e) {
            log.debug("Ollama is not healthy or unreachable", e);
            return false;
        }
    }
}
