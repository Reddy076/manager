package com.revature.passwordmanager.service.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
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
     * Send prompt to OpenAI-compatible API (Groq) and return the assistant's reply.
     * Enforces JSON response format.
     */
    public String generateCompletion(String systemPrompt, String userPrompt) {
        return callChatCompletion(systemPrompt, userPrompt, true);
    }

    /**
     * Send prompt to OpenAI-compatible API (Groq) and return plain text (no JSON forced).
     */
    public String generateText(String systemPrompt, String userPrompt) {
        return callChatCompletion(systemPrompt, userPrompt, false);
    }

    private String callChatCompletion(String systemPrompt, String userPrompt, boolean jsonMode) {
        // OpenAI-compatible chat completions endpoint
        String url = config.getBaseUrl() + "/chat/completions";

        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("model", config.getModel());
        payload.put("temperature", config.getTemperature());
        payload.put("max_tokens", config.getMaxTokens());
        payload.put("stream", false);

        // Build messages array
        ArrayNode messages = payload.putArray("messages");
        ObjectNode systemMsg = messages.addObject();
        systemMsg.put("role", "system");
        systemMsg.put("content", systemPrompt);
        ObjectNode userMsg = messages.addObject();
        userMsg.put("role", "user");
        userMsg.put("content", userPrompt);

        // Instruct Groq to return JSON when needed
        if (jsonMode) {
            ObjectNode responseFormat = payload.putObject("response_format");
            responseFormat.put("type", "json_object");
        }

        try {
            RequestBody body = RequestBody.create(
                    objectMapper.writeValueAsString(payload),
                    MediaType.parse("application/json")
            );

            Request request = new Request.Builder()
                    .url(url)
                    .addHeader("Authorization", "Bearer " + config.getApiKey())
                    .addHeader("Content-Type", "application/json")
                    .post(body)
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    String errorBody = response.body() != null ? response.body().string() : "(empty)";
                    log.error("Groq API failed with status: {} body: {}", response.code(), errorBody);
                    throw new LlmCommunicationException("LLM API returned: " + response.code() + " - " + errorBody);
                }

                if (response.body() == null) {
                    throw new LlmCommunicationException("Empty response body from LLM API");
                }

                String responseBodyStr = response.body().string();
                log.debug("Groq raw response: {}", responseBodyStr);
                JsonNode responseNode = objectMapper.readTree(responseBodyStr);
                // OpenAI format: choices[0].message.content
                return responseNode.path("choices").get(0).path("message").path("content").asText();
            }
        } catch (IOException e) {
            log.error("Error communicating with LLM API", e);
            throw new LlmCommunicationException("Error communicating with LLM service", e);
        }
    }

    /**
     * Checks if the Groq API is reachable by hitting the models endpoint.
     */
    public boolean isHealthy() {
        String url = config.getBaseUrl() + "/models";
        Request request = new Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer " + config.getApiKey())
                .build();
        try (Response response = httpClient.newCall(request).execute()) {
            return response.isSuccessful();
        } catch (IOException e) {
            log.debug("LLM API is not healthy or unreachable", e);
            return false;
        }
    }
}
