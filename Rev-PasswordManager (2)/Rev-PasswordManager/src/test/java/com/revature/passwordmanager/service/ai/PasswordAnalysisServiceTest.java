package com.revature.passwordmanager.service.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.revature.passwordmanager.dto.response.PasswordAnalysisResult;
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
class PasswordAnalysisServiceTest {

    @Mock
    private LlmClientService llmClient;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private PasswordAnalysisService passwordAnalysisService;

    @Test
    void analyzePassword_WhenValidJsonReturned_ReturnsParsedResult() {
        // Arrange
        String mockJsonResponse = """
            {
              "strength": "WEAK",
              "vulnerabilities": ["Too short", "No special characters"],
              "suggestions": ["Add numbers", "Add symbols"]
            }
            """;
        
        when(llmClient.generateCompletion(anyString(), contains("Analyze this password: 'password123'")))
                .thenReturn(mockJsonResponse);

        // Act
        PasswordAnalysisResult result = passwordAnalysisService.analyzePassword("password123");

        // Assert
        assertNotNull(result);
        assertEquals("WEAK", result.getStrength());
        assertEquals(2, result.getVulnerabilities().size());
        assertEquals("Too short", result.getVulnerabilities().get(0));
        assertEquals(2, result.getSuggestions().size());
    }

    @Test
    void analyzePassword_WhenInvalidJsonReturned_HandlesGracefully() {
        // Arrange
        String mockBadResponse = "Here is your analysis: It's a weak password. I can't format this as JSON.";
        
        when(llmClient.generateCompletion(anyString(), anyString()))
                .thenReturn(mockBadResponse);

        // Act
        PasswordAnalysisResult result = passwordAnalysisService.analyzePassword("badpassword");

        // Assert
        assertNotNull(result);
        assertEquals("UNKNOWN", result.getStrength());
        assertTrue(result.getVulnerabilities().get(0).contains("malformed data"));
    }

    @Test
    void analyzePassword_WhenLlmClientThrowsException_ThrowsRuntimeException() {
        // Arrange
        when(llmClient.generateCompletion(anyString(), anyString()))
                .thenThrow(new RuntimeException("Connection Refused"));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            passwordAnalysisService.analyzePassword("anypassword");
        });
        
        assertTrue(exception.getMessage().contains("AI Password Analysis Failed"));
    }
}
