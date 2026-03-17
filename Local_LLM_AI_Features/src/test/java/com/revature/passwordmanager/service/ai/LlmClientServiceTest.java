package com.revature.passwordmanager.service.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.revature.passwordmanager.config.LlmConfig;
import com.revature.passwordmanager.exception.LlmCommunicationException;
import okhttp3.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LlmClientServiceTest {

    private LlmConfig config;

    @Mock
    private OkHttpClient httpClient;

    @Mock
    private Call call;

    private ObjectMapper objectMapper;

    private LlmClientService llmClientService;

    @BeforeEach
    void setUp() {
        config = new LlmConfig();
        config.setBaseUrl("http://localhost:11434");
        config.setModel("phi3");
        config.setTemperature(0.7);

        objectMapper = new ObjectMapper();
        llmClientService = new LlmClientService(config, httpClient, objectMapper);
    }

    @Test
    void isHealthy_WhenReturns200_ReturnsTrue() throws IOException {
        Response response = new Response.Builder()
                .request(new Request.Builder().url("http://localhost:11434").build())
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .body(ResponseBody.create("OK", MediaType.parse("text/plain")))
                .build();

        when(httpClient.newCall(any(Request.class))).thenReturn(call);
        when(call.execute()).thenReturn(response);

        assertTrue(llmClientService.isHealthy());
    }

    @Test
    void isHealthy_WhenThrowsException_ReturnsFalse() throws IOException {
        when(httpClient.newCall(any(Request.class))).thenReturn(call);
        when(call.execute()).thenThrow(new IOException("Connection refused"));

        assertFalse(llmClientService.isHealthy());
    }

    @Test
    void generateCompletion_Success_ReturnsResponseString() throws IOException {
        String mockOllamaResponse = "{\"model\":\"phi3\",\"response\":\"This is the AI response\"}";
        ResponseBody responseBody = ResponseBody.create(mockOllamaResponse, MediaType.parse("application/json"));

        Response response = new Response.Builder()
                .request(new Request.Builder().url("http://localhost:11434/api/generate").build())
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .body(responseBody)
                .build();

        when(httpClient.newCall(any(Request.class))).thenReturn(call);
        when(call.execute()).thenReturn(response);

        String result = llmClientService.generateCompletion("System Prompt", "User Prompt");

        assertEquals("This is the AI response", result);
        
        // Verify the request payload
        ArgumentCaptor<Request> requestCaptor = ArgumentCaptor.forClass(Request.class);
        verify(httpClient).newCall(requestCaptor.capture());
        Request capturedRequest = requestCaptor.getValue();
        assertEquals("http://localhost:11434/api/generate", capturedRequest.url().toString());
    }

    @Test
    void generateCompletion_WhenApiFails_ThrowsException() throws IOException {
        Response response = new Response.Builder()
                .request(new Request.Builder().url("http://localhost:11434/api/generate").build())
                .protocol(Protocol.HTTP_1_1)
                .code(500)
                .message("Internal Server Error")
                .build();

        when(httpClient.newCall(any(Request.class))).thenReturn(call);
        when(call.execute()).thenReturn(response);

        LlmCommunicationException exception = assertThrows(LlmCommunicationException.class, () -> {
            llmClientService.generateCompletion("System Prompt", "User Prompt");
        });

        assertTrue(exception.getMessage().contains("Failed to generate completion"));
    }
}
