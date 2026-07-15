package com.example.knowledgeassistant.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class GeminiClient {

    private final String apiKey;
    private final String model;
    private final String baseUrl;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public GeminiClient(
            @Value("${gemini.api.key}") String apiKey,
            @Value("${gemini.api.model}") String model,
            @Value("${gemini.api.url}") String baseUrl) {
        this.apiKey = apiKey;
        this.model = model;
        this.baseUrl = baseUrl;
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Send a chat message with conversation history to the Gemini API.
     */
    public String chat(List<Map<String, String>> conversationHistory, String userMessage, String documentContext) {
        try {
            String url = baseUrl + "/" + model + ":generateContent?key=" + apiKey;

            // Build the contents array from conversation history
            List<Map<String, Object>> contents = new ArrayList<>();

            // If there's document context, prepend it as a system-level instruction
            if (documentContext != null && !documentContext.isEmpty()) {
                Map<String, Object> systemContent = new HashMap<>();
                systemContent.put("role", "user");
                List<Map<String, String>> systemParts = new ArrayList<>();
                systemParts.add(Map.of("text",
                        "You are an AI Knowledge Assistant. The user has uploaded a document. " +
                        "Use the following document content to answer questions accurately. " +
                        "If the answer isn't in the document, say so clearly.\n\n" +
                        "DOCUMENT CONTENT:\n" + documentContext));
                systemContent.put("parts", systemParts);
                contents.add(systemContent);

                // Add model acknowledgment
                Map<String, Object> modelAck = new HashMap<>();
                modelAck.put("role", "model");
                modelAck.put("parts", List.of(Map.of("text",
                        "I've read the document. I'll use it to answer your questions accurately.")));
                contents.add(modelAck);
            }

            // Add conversation history
            for (Map<String, String> msg : conversationHistory) {
                Map<String, Object> content = new HashMap<>();
                String role = msg.get("role").equals("USER") ? "user" : "model";
                content.put("role", role);
                content.put("parts", List.of(Map.of("text", msg.get("content"))));
                contents.add(content);
            }

            // Add the current user message
            Map<String, Object> currentMessage = new HashMap<>();
            currentMessage.put("role", "user");
            currentMessage.put("parts", List.of(Map.of("text", userMessage)));
            contents.add(currentMessage);

            // Build request body
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("contents", contents);

            // Add generation config
            Map<String, Object> generationConfig = new HashMap<>();
            generationConfig.put("temperature", 0.7);
            generationConfig.put("maxOutputTokens", 2048);
            requestBody.put("generationConfig", generationConfig);

            String jsonBody = objectMapper.writeValueAsString(requestBody);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                return extractResponseText(response.body());
            } else {
                log.error("Gemini API error: {} - {}", response.statusCode(), response.body());
                return "I'm sorry, I encountered an error processing your request. Please check the API key configuration and try again.";
            }

        } catch (IOException | InterruptedException e) {
            log.error("Error calling Gemini API", e);
            return "I'm sorry, I couldn't process your request at this time. Please ensure the GEMINI_API_KEY environment variable is set correctly.";
        }
    }

    private String extractResponseText(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode candidates = root.path("candidates");
            if (candidates.isArray() && !candidates.isEmpty()) {
                JsonNode content = candidates.get(0).path("content");
                JsonNode parts = content.path("parts");
                if (parts.isArray() && !parts.isEmpty()) {
                    return parts.get(0).path("text").asText();
                }
            }
            return "I received an empty response. Please try again.";
        } catch (Exception e) {
            log.error("Error parsing Gemini response", e);
            return "Error parsing AI response.";
        }
    }
}
