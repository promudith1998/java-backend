package com.example.knowledgeassistant.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class DashboardResponse {
    private long totalConversations;
    private long totalMessages;
    private long totalDocuments;
    private String username;
}
