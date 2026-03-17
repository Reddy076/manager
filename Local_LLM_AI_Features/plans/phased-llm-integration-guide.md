# Local LLM Phased Integration Guide

This document breaks down the integration of your local Ollama instance into 6 small, testable phases. Each phase can be verified independently before moving to the next.

---

## Phase 1: Infrastructure & Backend Connectivity
**Goal**: Establish the "plumbing" between the Spring Boot backend and the Ollama Docker container.

1.  **Modify `pom.xml`**:
    *   Add `okhttp` for high-performance HTTP requests.
    *   Add `jackson-databind` for parsing LLM's JSON responses.
2.  **Modify `application.properties`**:
    *   Add the `llm.*` properties pointing to `http://localhost:11434`.
3.  **Connectivity Check**:
    *   Run a simple `curl` test from within the backend environment to ensure it can reach the Ollama API.

---

## Phase 2: Core AI Client Layer
**Goal**: Create the reusable Java services that talk to the LLM.

1.  **Create `LlmConfig.java`**: Map properties from the previous phase to a Java bean.
2.  **Create `LlmClientService.java`**: 
    *   This service will handle the HTTP post requests to Ollama's `/api/chat` or `/api/generate`.
    *   Implement a `isHealthy()` method that checks if Ollama responds on its base URL.
3.  **Expose Health Check**: Create `/api/ai/health` in a new `AIController` to verify this layer from a browser.

---

## Phase 3: Feature A - Password Analysis
**Goal**: Add the first AI-powered security feature.

1.  **Backend Logic**:
    *   Create `PasswordAnalysisService` with a specialized "Cybersecurity Expert" system prompt.
    *   Expose `POST /api/ai/analyze-password`.
2.  **Frontend Component**:
    *   Create a simple UI where users can paste a password (like `qwerty`) and see a list of AI-generated vulnerabilities and suggestions.

---

## Phase 4: Feature B - Smart Categorization
**Goal**: Automate password organization using AI.

1.  **Backend Logic**:
    *   Create `SmartVaultCategorizationService`.
    *   Prompt the LLM to return categories (e.g., "Finance") and tags based on a URL.
2.  **Vault Integration**:
    *   Update the "Add Entry" dialog in the Angular app.
    *   Add an "Auto-Tag" button next to the URL input.

---

## Phase 5: Feature C - Security Chatbot
**Goal**: Create an interactive security assistant.

1.  **Session Management**:
    *   Implement `SecurityChatbotService` with a basic memory map to store recent messages for each user.
2.  **Chat UI**:
    *   Create a new Chatbot component in Angular.
    *   Implement a message-bubble interface that communicates with the LLM in a dialogue format.

---

## Phase 6: Final Polish & Security
**Goal**: Secure the endpoints and finalize the UX.

1.  **Spring Security**:
    *   Ensure all new `/api/ai/**` endpoints (except health) require a valid JWT.
2.  **Error Handling**:
    *   Add graceful error messages in the frontend if Ollama is offline or timing out.
3.  **Verification**: 
    *   Perform a final walk-through of all three features to ensure consistency.
