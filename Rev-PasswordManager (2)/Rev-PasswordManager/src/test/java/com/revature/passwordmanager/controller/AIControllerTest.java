package com.revature.passwordmanager.controller;

import com.revature.passwordmanager.dto.response.PasswordAnalysisResult;
import com.revature.passwordmanager.service.ai.LlmClientService;
import com.revature.passwordmanager.service.ai.PasswordAnalysisService;
import com.revature.passwordmanager.service.ai.SmartVaultCategorizationService;
import com.revature.passwordmanager.service.ai.SecurityChatbotService;
import com.revature.passwordmanager.dto.request.ChatRequest;
import com.revature.passwordmanager.dto.response.ChatResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AIControllerTest {

    @Mock
    private LlmClientService llmClientService;

    @Mock
    private PasswordAnalysisService passwordAnalysisService;

    @Mock
    private SmartVaultCategorizationService categorizationService;

    @Mock
    private SecurityChatbotService chatbotService;

    @InjectMocks
    private AIController aiController;

    @Test
    void checkHealth_WhenHealthy_ReturnsOk() {
        // Arrange
        when(llmClientService.isHealthy()).thenReturn(true);

        // Act
        ResponseEntity<Map<String, Object>> response = aiController.checkHealth();

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("UP", response.getBody().get("status"));
        assertEquals("Ollama LLM is reachable.", response.getBody().get("message"));
    }

    @Test
    void checkHealth_WhenUnhealthy_ReturnsServiceUnavailable() {
        // Arrange
        when(llmClientService.isHealthy()).thenReturn(false);

        // Act
        ResponseEntity<Map<String, Object>> response = aiController.checkHealth();

        // Assert
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
        assertEquals("DOWN", response.getBody().get("status"));
        assertEquals("Failed to connect to Ollama LLM.", response.getBody().get("message"));
    }

    @Test
    void analyzePassword_WhenValidPassword_ReturnsAnalysis() {
        // Arrange
        Map<String, String> request = Map.of("password", "SecureP@ssw0rd");
        PasswordAnalysisResult mockResult = PasswordAnalysisResult.builder()
                .strength("STRONG")
                .vulnerabilities(List.of())
                .suggestions(List.of("Looks good"))
                .build();
        
        when(passwordAnalysisService.analyzePassword("SecureP@ssw0rd")).thenReturn(mockResult);

        // Act
        ResponseEntity<PasswordAnalysisResult> response = aiController.analyzePassword(request);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("STRONG", response.getBody().getStrength());
    }

    @Test
    void analyzePassword_WhenPasswordMissing_ReturnsBadRequest() {
        // Arrange
        Map<String, String> request = Map.of(); // empty mapping

        // Act
        ResponseEntity<PasswordAnalysisResult> response = aiController.analyzePassword(request);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void analyzePassword_WhenPasswordBlank_ReturnsBadRequest() {
        // Arrange
        Map<String, String> request = Map.of("password", "   ");

        // Act
        ResponseEntity<PasswordAnalysisResult> response = aiController.analyzePassword(request);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void chat_WhenValidRequest_ReturnsReply() {
        // Arrange
        ChatRequest request = new ChatRequest("Help me");
        ChatResponse mockResponse = ChatResponse.builder().reply("I can help").build();
        when(chatbotService.chat(request)).thenReturn(mockResponse);

        // Act
        ResponseEntity<ChatResponse> response = aiController.chat(request);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("I can help", response.getBody().getReply());
    }

    @Test
    void chat_WhenMessageMissing_ReturnsBadRequest() {
        // Arrange
        ChatRequest request = new ChatRequest();

        // Act
        ResponseEntity<ChatResponse> response = aiController.chat(request);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void chat_WhenMessageBlank_ReturnsBadRequest() {
        // Arrange
        ChatRequest request = new ChatRequest("   ");

        // Act
        ResponseEntity<ChatResponse> response = aiController.chat(request);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }
}
