package com.revature.passwordmanager.service.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.revature.passwordmanager.dto.response.CategorizationResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SmartVaultCategorizationServiceTest {

    @Mock
    private LlmClientService llmClient;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private SmartVaultCategorizationService categorizationService;

    @Test
    void categorizeEntry_WhenValidJsonReturned_ReturnsParsedResult() {
        // Arrange
        String mockJsonResponse = """
            {
              "category": "WORK",
              "tags": ["corporate", "infrastructure"],
              "confidence": 0.95
            }
            """;
        
        when(llmClient.generateCompletion(anyString(), contains("URL: jira.company.com")))
                .thenReturn(mockJsonResponse);

        // Act
        CategorizationResult result = categorizationService.categorizeEntry("jira.company.com", "admin", "Company Jira");

        // Assert
        assertNotNull(result);
        assertEquals("WORK", result.getCategory());
        assertEquals(0.95, result.getConfidence());
        assertEquals(2, result.getTags().size());
        assertEquals("corporate", result.getTags().get(0));
    }

    @Test
    void categorizeEntry_WhenInvalidJsonReturned_HandlesGracefullyAndDefaultsToOther() {
        // Arrange
        String mockBadResponse = "I think this is a personal website, but I cannot give you JSON.";
        
        when(llmClient.generateCompletion(anyString(), anyString()))
                .thenReturn(mockBadResponse);

        // Act
        CategorizationResult result = categorizationService.categorizeEntry("badurl.com", "user", "title");

        // Assert
        assertNotNull(result);
        assertEquals("OTHER", result.getCategory());
        assertEquals(0.0, result.getConfidence());
        assertEquals(1, result.getTags().size());
        assertEquals("error_categorizing", result.getTags().get(0));
    }

    @Test
    void categorizeEntry_WhenLlmClientThrowsException_HandlesGracefullyAndDefaultsToOther() {
        // Arrange
        when(llmClient.generateCompletion(anyString(), anyString()))
                .thenThrow(new RuntimeException("Connection Refused"));

        // Act
        CategorizationResult result = categorizationService.categorizeEntry("error.com", null, null);

        // Assert
        assertNotNull(result);
        assertEquals("OTHER", result.getCategory());
        assertEquals(0.0, result.getConfidence());
        assertTrue(result.getTags().contains("error_categorizing"));
    }
}
