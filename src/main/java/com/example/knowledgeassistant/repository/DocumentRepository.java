package com.example.knowledgeassistant.repository;

import com.example.knowledgeassistant.entity.Document;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface DocumentRepository extends JpaRepository<Document, Long> {
    List<Document> findByUserIdOrderByUploadedAtDesc(Long userId);
    long countByUserId(Long userId);
}
