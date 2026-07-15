package com.example.knowledgeassistant.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Service
@Slf4j
public class SupabaseStorageService {

    private final String supabaseUrl;
    private final String supabaseSecretKey;
    private final String bucketName;
    private final HttpClient httpClient;

    public SupabaseStorageService(
            @Value("${supabase.url}") String supabaseUrl,
            @Value("${supabase.secret-key}") String supabaseSecretKey,
            @Value("${supabase.bucket:documents}") String bucketName) {
        this.supabaseUrl = supabaseUrl;
        this.supabaseSecretKey = supabaseSecretKey;
        this.bucketName = bucketName;
        this.httpClient = HttpClient.newHttpClient();
        
        // Try creating the bucket on startup if it doesn't exist
        createBucketIfNotExists();
    }

    private void createBucketIfNotExists() {
        try {
            String url = supabaseUrl + "/storage/v1/bucket";
            String jsonPayload = "{\"id\":\"" + bucketName + "\",\"name\":\"" + bucketName + "\",\"public\":true}";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", "Bearer " + supabaseSecretKey)
                    .header("apiKey", supabaseSecretKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                log.info("Successfully created/verified Supabase storage bucket: {}", bucketName);
            } else {
                log.info("Bucket verification response: {} - {}", response.statusCode(), response.body());
            }
        } catch (Exception e) {
            log.warn("Failed to create/verify Supabase storage bucket: {}", e.getMessage());
        }
    }

    public String uploadFile(MultipartFile file, Long userId) throws IOException {
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            originalFilename = "unnamed";
        }
        
        // Sanitize path and filename to avoid URI parsing problems
        String sanitizedFilename = originalFilename.replaceAll("[^a-zA-Z0-9._-]", "_");
        String path = userId + "/" + System.currentTimeMillis() + "_" + sanitizedFilename;
        
        // Format object URL correctly
        String url = supabaseUrl + "/storage/v1/object/" + bucketName + "/" + path;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", "Bearer " + supabaseSecretKey)
                .header("apiKey", supabaseSecretKey)
                .header("Content-Type", file.getContentType() != null ? file.getContentType() : "application/octet-stream")
                .POST(HttpRequest.BodyPublishers.ofByteArray(file.getBytes()))
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                String fileUrl = supabaseUrl + "/storage/v1/object/public/" + bucketName + "/" + path;
                log.info("File uploaded successfully to Supabase Storage: {}", fileUrl);
                return fileUrl;
            } else {
                log.error("Failed to upload file to Supabase: {} - {}", response.statusCode(), response.body());
                throw new RuntimeException("Failed to upload file to storage: " + response.body());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Upload was interrupted", e);
        }
    }

    public void deleteFile(String fileUrl) {
        if (fileUrl == null || !fileUrl.contains("/storage/v1/object/public/" + bucketName + "/")) {
            return;
        }
        
        try {
            // Extract file path from url
            String prefix = "/storage/v1/object/public/" + bucketName + "/";
            int index = fileUrl.indexOf(prefix);
            if (index == -1) return;
            String path = fileUrl.substring(index + prefix.length());
            
            String url = supabaseUrl + "/storage/v1/object/" + bucketName;
            String jsonPayload = "{\"prefixes\":[\"" + path + "\"]}";
            
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", "Bearer " + supabaseSecretKey)
                    .header("apiKey", supabaseSecretKey)
                    .header("Content-Type", "application/json")
                    .method("DELETE", HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .build();
            
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                log.info("File deleted successfully from Supabase Storage: {}", path);
            } else {
                log.warn("Failed to delete file from Supabase Storage: {} - {}", response.statusCode(), response.body());
            }
        } catch (Exception e) {
            log.error("Error deleting file from Supabase Storage", e);
        }
    }
}
