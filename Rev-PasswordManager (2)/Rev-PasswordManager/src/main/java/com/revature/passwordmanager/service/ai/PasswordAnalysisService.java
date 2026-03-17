package com.revature.passwordmanager.service.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.revature.passwordmanager.dto.response.PasswordAnalysisResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;

@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordAnalysisService {

    private final LlmClientService llmClient;
    private final ObjectMapper objectMapper;

    /**
     * Sends a password to the LLM to get a security analysis including strength, vulnerabilities, and suggestions.
     * 
     * @param password The raw password string to analyze.
     * @return PasswordAnalysisResult parsed from the LLM's JSON response.
     */
    public PasswordAnalysisResult analyzePassword(String password) {
        String systemPrompt = """
            You are a cybersecurity expert specializing in password security.
            Analyze the given password conceptually and provide:
            1. Strength rating (Must be exactly one of: VERY_WEAK, WEAK, MODERATE, STRONG, VERY_STRONG)
            2. List of specific vulnerabilities (array of strings)
            3. Actionable improvement suggestions (array of strings, max 3)
            
            Keep responses concise, factual, and user-friendly. Do not include markdown formatting or extra conversational text.
            Format your response strictly as valid JSON with keys: "strength", "vulnerabilities", "suggestions"
            """;

        String userPrompt = "Analyze this password: '" + password + "'";

        try {
            log.debug("Sending password analysis request to LLM");
            // 1. Fetch text response from Ollama
            String llmResponseText = llmClient.generateCompletion(systemPrompt, userPrompt);
            
            // 2. Parse JSON into DTO
            return objectMapper.readValue(llmResponseText, PasswordAnalysisResult.class);
            
        } catch (JsonProcessingException e) {
            log.error("Failed to parse LLM JSON response for password analysis", e);
            return PasswordAnalysisResult.builder()
                    .strength("UNKNOWN")
                    .vulnerabilities(new ArrayList<>(java.util.List.of("Error evaluating password. LLM returned malformed data.")))
                    .suggestions(new ArrayList<>(java.util.List.of("Ensure the AI service is responding with valid JSON.")))
                    .build();
        } catch (Exception e) {
            log.error("Error during password analysis sequence", e);
            throw new RuntimeException("AI Password Analysis Failed", e);
        }
    }
}
