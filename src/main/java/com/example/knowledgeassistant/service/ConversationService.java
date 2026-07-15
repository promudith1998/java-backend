package com.example.knowledgeassistant.service;

import com.example.knowledgeassistant.dto.*;
import com.example.knowledgeassistant.entity.*;
import com.example.knowledgeassistant.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ConversationService {

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final DocumentRepository documentRepository;
    private final GeminiClient geminiClient;

    public List<ConversationResponse> getUserConversations(Long userId) {
        return conversationRepository.findByUserIdOrderByUpdatedAtDesc(userId).stream()
                .map(this::toConversationResponse)
                .collect(Collectors.toList());
    }

    public ConversationResponse getConversation(Long conversationId, Long userId) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new RuntimeException("Conversation not found"));

        if (!conversation.getUser().getId().equals(userId)) {
            throw new RuntimeException("Access denied");
        }

        return toConversationResponse(conversation);
    }

    @Transactional
    public ConversationResponse createConversation(User user, String title) {
        Conversation conversation = Conversation.builder()
                .title(title)
                .user(user)
                .build();

        conversation = conversationRepository.save(conversation);
        return toConversationResponse(conversation);
    }

    @Transactional
    public MessageResponse sendMessage(Long conversationId, Long userId, ChatRequest chatRequest) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new RuntimeException("Conversation not found"));

        if (!conversation.getUser().getId().equals(userId)) {
            throw new RuntimeException("Access denied");
        }

        // Save user message
        Message userMessage = Message.builder()
                .conversation(conversation)
                .role("USER")
                .content(chatRequest.getMessage())
                .build();
        messageRepository.save(userMessage);

        // Build conversation history for AI context
        List<Message> history = messageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId);
        List<Map<String, String>> historyMaps = history.stream()
                .limit(Math.max(0, history.size() - 1)) // Exclude the just-saved user message
                .map(m -> {
                    Map<String, String> map = new HashMap<>();
                    map.put("role", m.getRole());
                    map.put("content", m.getContent());
                    return map;
                })
                .collect(Collectors.toList());

        // Check for document context
        String documentContext = null;
        if (chatRequest.getDocumentId() != null) {
            Document doc = documentRepository.findById(chatRequest.getDocumentId()).orElse(null);
            if (doc != null && doc.getUser().getId().equals(userId)) {
                documentContext = doc.getExtractedText();
            }
        }

        // Call Gemini API
        String aiResponse = geminiClient.chat(historyMaps, chatRequest.getMessage(), documentContext);

        // Save AI response
        Message assistantMessage = Message.builder()
                .conversation(conversation)
                .role("ASSISTANT")
                .content(aiResponse)
                .build();
        assistantMessage = messageRepository.save(assistantMessage);

        // Update conversation timestamp
        conversation.setUpdatedAt(java.time.LocalDateTime.now());
        conversationRepository.save(conversation);

        return MessageResponse.builder()
                .id(assistantMessage.getId())
                .role(assistantMessage.getRole())
                .content(assistantMessage.getContent())
                .createdAt(assistantMessage.getCreatedAt())
                .build();
    }

    @Transactional
    public void deleteConversation(Long conversationId, Long userId) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new RuntimeException("Conversation not found"));

        if (!conversation.getUser().getId().equals(userId)) {
            throw new RuntimeException("Access denied");
        }

        conversationRepository.delete(conversation);
    }

    private ConversationResponse toConversationResponse(Conversation conversation) {
        List<MessageResponse> messages = conversation.getMessages().stream()
                .map(m -> MessageResponse.builder()
                        .id(m.getId())
                        .role(m.getRole())
                        .content(m.getContent())
                        .createdAt(m.getCreatedAt())
                        .build())
                .collect(Collectors.toList());

        return ConversationResponse.builder()
                .id(conversation.getId())
                .title(conversation.getTitle())
                .createdAt(conversation.getCreatedAt())
                .updatedAt(conversation.getUpdatedAt())
                .messages(messages)
                .build();
    }
}
