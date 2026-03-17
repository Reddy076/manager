package com.revature.passwordmanager.service.ai;

import com.revature.passwordmanager.dto.request.ChatRequest;
import com.revature.passwordmanager.dto.response.ChatResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class SecurityChatbotServiceTest {

    @Mock
    private LlmClientService llmClientService;

    @InjectMocks
    private SecurityChatbotService securityChatbotService;

    private ChatRequest validRequest;

    @BeforeEach
    void setUp() {
        validRequest = new ChatRequest("What is a strong password?");
    }

    @Test
    void chat_ShouldReturnAiReply_WhenLlmSucceeds() {
        when(llmClientService.generateText(anyString(), eq("What is a strong password?")))
                .thenReturn("A strong password is at least 12 characters long.");

        ChatResponse response = securityChatbotService.chat(validRequest);

        assertNotNull(response);
        assertTrue(response.isAiPowered());
        assertEquals("GENERAL", response.getIntent());
        assertEquals("A strong password is at least 12 characters long.", response.getReply());
        assertNotNull(response.getTimestamp());
    }

    @Test
    void chat_ShouldReturnFallbackReply_WhenLlmThrowsException() {
        when(llmClientService.generateText(anyString(), eq("What is a strong password?")))
                .thenThrow(new RuntimeException("Connection Refused"));

        ChatResponse response = securityChatbotService.chat(validRequest);

        assertNotNull(response);
        assertFalse(response.isAiPowered());
        assertEquals("ERROR", response.getIntent());
        assertEquals("I'm sorry, my AI connection is currently offline. Please try again later.", response.getReply());
        assertNotNull(response.getTimestamp());
    }
}
