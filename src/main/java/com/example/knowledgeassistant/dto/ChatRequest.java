package com.example.knowledgeassistant.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ChatRequest {
    @NotBlank(message = "Message content is required")
    private String message;

    private Long documentId; // Optional: for document-based Q&A
}
