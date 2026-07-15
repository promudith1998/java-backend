package com.example.knowledgeassistant.service;

import com.example.knowledgeassistant.dto.DocumentResponse;
import com.example.knowledgeassistant.entity.Document;
import com.example.knowledgeassistant.entity.User;
import com.example.knowledgeassistant.repository.DocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final SupabaseStorageService supabaseStorageService;

    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB

    public DocumentResponse uploadDocument(MultipartFile file, User user) {
        // Validate file
        if (file.isEmpty()) {
            throw new RuntimeException("File is empty");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new RuntimeException("File size exceeds 5MB limit");
        }

        String filename = file.getOriginalFilename();
        String fileType = getFileType(filename);

        if (!fileType.equals("pdf") && !fileType.equals("txt")) {
            throw new RuntimeException("Only PDF and TXT files are supported");
        }

        // Extract text
        String extractedText;
        try {
            extractedText = extractText(file, fileType);
        } catch (IOException e) {
            log.error("Error extracting text from file: {}", filename, e);
            throw new RuntimeException("Failed to extract text from file");
        }

        if (extractedText == null || extractedText.isBlank()) {
            throw new RuntimeException("No text could be extracted from the file");
        }

        // Upload file to Supabase Storage
        String fileUrl;
        try {
            fileUrl = supabaseStorageService.uploadFile(file, user.getId());
        } catch (IOException e) {
            log.error("Error uploading file to Supabase Storage: {}", filename, e);
            throw new RuntimeException("Failed to upload file to storage");
        }

        // Save document
        Document document = Document.builder()
                .filename(filename)
                .fileType(fileType)
                .fileSize(file.getSize())
                .extractedText(extractedText)
                .fileUrl(fileUrl)
                .user(user)
                .build();

        document = documentRepository.save(document);

        return toDocumentResponse(document);
    }

    public List<DocumentResponse> getUserDocuments(Long userId) {
        return documentRepository.findByUserIdOrderByUploadedAtDesc(userId).stream()
                .map(this::toDocumentResponse)
                .collect(Collectors.toList());
    }

    public void deleteDocument(Long documentId, Long userId) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Document not found"));

        if (!document.getUser().getId().equals(userId)) {
            throw new RuntimeException("Access denied");
        }

        // Delete from Supabase Storage
        if (document.getFileUrl() != null) {
            supabaseStorageService.deleteFile(document.getFileUrl());
        }

        documentRepository.delete(document);
    }

    private String extractText(MultipartFile file, String fileType) throws IOException {
        if (fileType.equals("pdf")) {
            try (PDDocument pdDocument = Loader.loadPDF(file.getBytes())) {
                PDFTextStripper stripper = new PDFTextStripper();
                return stripper.getText(pdDocument);
            }
        } else {
            // TXT file
            return new String(file.getBytes());
        }
    }

    private String getFileType(String filename) {
        if (filename == null) return "";
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex == -1) return "";
        return filename.substring(dotIndex + 1).toLowerCase();
    }

    private DocumentResponse toDocumentResponse(Document document) {
        return DocumentResponse.builder()
                .id(document.getId())
                .filename(document.getFilename())
                .fileType(document.getFileType())
                .fileSize(document.getFileSize())
                .fileUrl(document.getFileUrl())
                .uploadedAt(document.getUploadedAt())
                .build();
    }
}
