package com.example.knowledgeassistant.repository;

import com.example.knowledgeassistant.entity.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ConversationRepository extends JpaRepository<Conversation, Long> {
    List<Conversation> findByUserIdOrderByUpdatedAtDesc(Long userId);
    long countByUserId(Long userId);
}
