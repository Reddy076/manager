package com.revature.passwordmanager.service.ai;

import com.revature.passwordmanager.dto.request.ChatRequest;
import com.revature.passwordmanager.dto.response.ChatResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class SecurityChatbotService {

    private final LlmClientService llmClientService;

    public ChatResponse chat(ChatRequest request) {
        String systemPrompt = "You are a highly knowledgeable and friendly Cybersecurity Expert assisting a user of the Rev Password Manager application. " +
                "Your primary goal is to provide concise, accurate, and helpful advice regarding password security, cybersecurity best practices, " +
                "and how to properly manage credentials. Do not answer questions that are entirely unrelated to technology, security, or software development. " +
                "Format your responses in Markdown for readability.";

        try {
            String aiReply = llmClientService.generateText(systemPrompt, request.getMessage());
            
            return ChatResponse.builder()
                    .reply(aiReply)
                    .intent("GENERAL")
                    .aiPowered(true)
                    .timestamp(LocalDateTime.now())
                    .build();

        } catch (Exception e) {
            return ChatResponse.builder()
                    .reply("I'm sorry, my AI connection is currently offline. Please try again later.")
                    .intent("ERROR")
                    .aiPowered(false)
                    .timestamp(LocalDateTime.now())
                    .build();
        }
    }
}
