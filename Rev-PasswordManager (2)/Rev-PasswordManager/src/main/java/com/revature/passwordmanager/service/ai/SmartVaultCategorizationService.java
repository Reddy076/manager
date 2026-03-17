package com.revature.passwordmanager.service.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.revature.passwordmanager.dto.response.CategorizationResult;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
@RequiredArgsConstructor
public class SmartVaultCategorizationService {

    private static final Logger logger = LoggerFactory.getLogger(SmartVaultCategorizationService.class);

    private final LlmClientService llmClient;
    private final ObjectMapper objectMapper;

    /**
     * Categorizes a vault entry using the LLM based on URL, username, and title.
     */
    public CategorizationResult categorizeEntry(String url, String username, String title) {
        String systemPrompt = """
            You are an AI categorizer for a secure password manager.
            Your job is to analyze the provided URL, username, and title, and extract:
            1. 'category': Must be ONE of the following precise strings: WORK, PERSONAL, SOCIAL, FINANCE, SHOPPING, DEVELOPMENT, EDUCATION, ENTERTAINMENT, BILLS, OTHER.
            2. 'tags': An array of up to 5 lowercase metadata tags describing the service.
            3. 'confidence': A decimal score between 0.00 and 1.00 indicating your confidence in the category.
            
            You must ONLY output valid JSON format containing exactly the keys 'category', 'tags', and 'confidence'.
            """;

        String userPrompt = String.format("URL: %s\nUsername: %s\nTitle: %s\n\nProvide the JSON categorization.", 
                url != null ? url : "N/A", 
                username != null ? username : "N/A", 
                title != null ? title : "N/A");

        try {
            logger.info("Requesting categorization from LLM for title: {}", title);
            String responseStr = llmClient.generateCompletion(systemPrompt, userPrompt);
            return objectMapper.readValue(responseStr, CategorizationResult.class);
            
        } catch (Exception e) {
            logger.error("Failed to parse Categorization LLM Response.", e);
            return CategorizationResult.builder()
                    .category("OTHER")
                    .tags(Collections.singletonList("error_categorizing"))
                    .confidence(0.0)
                    .build();
        }
    }
}
