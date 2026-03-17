package com.revature.passwordmanager.controller;

import com.revature.passwordmanager.dto.response.CategorizationResult;
import com.revature.passwordmanager.dto.response.PasswordAnalysisResult;
import com.revature.passwordmanager.service.ai.LlmClientService;
import com.revature.passwordmanager.service.ai.PasswordAnalysisService;
import com.revature.passwordmanager.service.ai.SmartVaultCategorizationService;
import com.revature.passwordmanager.service.ai.SecurityChatbotService;
import com.revature.passwordmanager.dto.request.ChatRequest;
import com.revature.passwordmanager.dto.response.ChatResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AIController {

    private final LlmClientService llmClientService;
    private final PasswordAnalysisService passwordAnalysisService;
    private final SmartVaultCategorizationService categorizationService;
    private final SecurityChatbotService chatbotService;

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> checkHealth() {
        boolean healthy = llmClientService.isHealthy();
        
        if (healthy) {
            return ResponseEntity.ok(Map.of(
                    "status", "UP",
                    "message", "Ollama LLM is reachable."
            ));
        } else {
            return ResponseEntity.status(503).body(Map.of(
                    "status", "DOWN",
                    "message", "Failed to connect to Ollama LLM."
            ));
        }
    }

    @PostMapping("/analyze-password")
    public ResponseEntity<PasswordAnalysisResult> analyzePassword(@RequestBody Map<String, String> request) {
        String password = request.get("password");
        if (password == null || password.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        
        PasswordAnalysisResult result = passwordAnalysisService.analyzePassword(password);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/categorize-entry")
    public ResponseEntity<CategorizationResult> categorizeEntry(@RequestBody Map<String, String> request) {
        String url = request.get("url");
        String username = request.get("username");
        String title = request.get("title");

        if (title == null || title.isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        CategorizationResult result = categorizationService.categorizeEntry(url, username, title);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/chat")
    public ResponseEntity<ChatResponse> chat(@RequestBody ChatRequest request) {
        if (request == null || request.getMessage() == null || request.getMessage().isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        ChatResponse response = chatbotService.chat(request);
        return ResponseEntity.ok(response);
    }
}
