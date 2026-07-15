package com.example.knowledgeassistant.controller;

import com.example.knowledgeassistant.dto.*;
import com.example.knowledgeassistant.entity.User;
import com.example.knowledgeassistant.service.ConversationService;
import com.example.knowledgeassistant.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/conversations")
@RequiredArgsConstructor
public class ConversationController {

    private final ConversationService conversationService;
    private final UserService userService;

    @GetMapping
    public ResponseEntity<List<ConversationResponse>> getConversations(Authentication auth) {
        User user = userService.getUserByUsername(auth.getName());
        return ResponseEntity.ok(conversationService.getUserConversations(user.getId()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ConversationResponse> getConversation(@PathVariable Long id, Authentication auth) {
        User user = userService.getUserByUsername(auth.getName());
        return ResponseEntity.ok(conversationService.getConversation(id, user.getId()));
    }

    @PostMapping
    public ResponseEntity<ConversationResponse> createConversation(
            @RequestBody Map<String, String> body, Authentication auth) {
        User user = userService.getUserByUsername(auth.getName());
        String title = body.getOrDefault("title", "New Conversation");
        return ResponseEntity.ok(conversationService.createConversation(user, title));
    }

    @PostMapping("/{id}/messages")
    public ResponseEntity<MessageResponse> sendMessage(
            @PathVariable Long id,
            @Valid @RequestBody ChatRequest chatRequest,
            Authentication auth) {
        User user = userService.getUserByUsername(auth.getName());
        return ResponseEntity.ok(conversationService.sendMessage(id, user.getId(), chatRequest));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteConversation(@PathVariable Long id, Authentication auth) {
        User user = userService.getUserByUsername(auth.getName());
        conversationService.deleteConversation(id, user.getId());
        return ResponseEntity.noContent().build();
    }
}
