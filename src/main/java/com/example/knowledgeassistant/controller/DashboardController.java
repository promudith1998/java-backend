package com.example.knowledgeassistant.controller;

import com.example.knowledgeassistant.dto.DashboardResponse;
import com.example.knowledgeassistant.entity.User;
import com.example.knowledgeassistant.repository.ConversationRepository;
import com.example.knowledgeassistant.repository.DocumentRepository;
import com.example.knowledgeassistant.repository.MessageRepository;
import com.example.knowledgeassistant.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final UserService userService;
    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final DocumentRepository documentRepository;

    @GetMapping
    public ResponseEntity<DashboardResponse> getDashboard(Authentication auth) {
        User user = userService.getUserByUsername(auth.getName());

        DashboardResponse dashboard = DashboardResponse.builder()
                .username(user.getUsername())
                .totalConversations(conversationRepository.countByUserId(user.getId()))
                .totalMessages(messageRepository.countByUserId(user.getId()))
                .totalDocuments(documentRepository.countByUserId(user.getId()))
                .build();

        return ResponseEntity.ok(dashboard);
    }
}
