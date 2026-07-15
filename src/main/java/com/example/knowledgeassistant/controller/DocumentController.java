package com.example.knowledgeassistant.controller;

import com.example.knowledgeassistant.dto.DocumentResponse;
import com.example.knowledgeassistant.entity.User;
import com.example.knowledgeassistant.service.DocumentService;
import com.example.knowledgeassistant.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;
    private final UserService userService;

    @PostMapping("/upload")
    public ResponseEntity<DocumentResponse> uploadDocument(
            @RequestParam("file") MultipartFile file,
            Authentication auth) {
        User user = userService.getUserByUsername(auth.getName());
        return ResponseEntity.ok(documentService.uploadDocument(file, user));
    }

    @GetMapping
    public ResponseEntity<List<DocumentResponse>> getDocuments(Authentication auth) {
        User user = userService.getUserByUsername(auth.getName());
        return ResponseEntity.ok(documentService.getUserDocuments(user.getId()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDocument(@PathVariable Long id, Authentication auth) {
        User user = userService.getUserByUsername(auth.getName());
        documentService.deleteDocument(id, user.getId());
        return ResponseEntity.noContent().build();
    }
}
