# AI Knowledge Assistant - Spring Boot Backend

This directory contains the REST API server for the AI Knowledge Assistant application, built with **Spring Boot** and **Java 21/25**. The backend orchestrates database mappings, handles text extraction from PDFs and TXT files, manages authentication via JWT, handles file uploads to a Supabase Storage bucket, and integrates with the Google Gemini API.

---

## 🚀 Key Features

*   **REST Architecture**: Clean Controller $\rightarrow$ Service $\rightarrow$ Repository layered design.
*   **Hosted Database**: Configured to run on a hosted **Supabase PostgreSQL** instance with automatic schema updates (`ddl-auto: update`).
*   **Supabase Storage**: Automatically checks or creates the public `documents` bucket on startup. Uploads original files to `{userId}/{timestamp}_{filename}` and maintains storage life-cycles (deleting objects when documents are deleted from the database).
*   **Text Extraction**: Parses text content from uploaded files (`.txt` directly, and `.pdf` files utilizing Apache **PDFBox**).
*   **Spring Security & JWT**: Stateless token authentication using JSON Web Tokens.
*   **Gemini API Integration**: Leverages `gemini-3.1-flash-lite` to handle AI conversations. Dynamically appends document content to chat prompts as system instructions when document context is selected.

---

## 🛠️ Technology Stack

*   **Framework**: Spring Boot (v4.0.3)
*   **Database**: PostgreSQL / JPA / Hibernate
*   **Security**: Spring Security & JSON Web Tokens (`jjwt`)
*   **External APIs**: Google Gemini API, Supabase Storage API
*   **Utilities**: Apache PDFBox (parsing PDF text), Jackson ObjectMapper (JSON payloads processing), Lombok (boilerplate reduction).

---

## 📁 Packages Structure

```text
backend/
├── src/
│   ├── main/
│   │   ├── java/com/example/knowledgeassistant/
│   │   │   ├── config/             # CORS and Bean definitions
│   │   │   ├── controller/         # REST API endpoints (Auth, Document, Chat)
│   │   │   ├── dto/                # Request/Response data transfer classes
│   │   │   ├── entity/             # JPA Database model schemas (User, Document, Message, Conversation)
│   │   │   ├── repository/         # Database repositories (Spring Data JPA)
│   │   │   ├── security/           # JWT filtering and authentication manager config
│   │   │   └── service/            # Business logic:
│   │   │       ├── ConversationService.java # Message state and history builder
│   │   │       ├── DocumentService.java     # Text parsing orchestrator
│   │   │       ├── SupabaseStorageService.java # S3 bucket uploader & deleter
│   │   │       ├── GeminiClient.java        # Gemini REST connector
│   │   │       └── UserService.java         # Auth processor
│   │   └── resources/
│   │       └── application.yml     # Application profile properties
│   └── test/                       # Compilation & context integrity tests
├── pom.xml                         # Maven dependencies & build lifecycle definitions
└── mvnw / mvnw.cmd                 # Maven wrapper scripts
```

---

## 🗄️ Database Mappings (JPA Entities)

*   `User`: Holds account credentials (`id`, `username`, `password`, `email`, `role`).
*   `Document`: Represents uploaded files (`id`, `filename`, `file_type`, `file_size`, `extracted_text`, `file_url`, `uploaded_at`).
*   `Conversation`: Organizes user chat sessions (`id`, `title`, `created_at`, `updated_at`).
*   `Message`: Stores historical messages in a session (`id`, `role` [USER/ASSISTANT], `content`, `timestamp`).

---

## ⚙️ Configuration Properties

Database connection and API endpoints are configured dynamically in [application.yml](file:///d:/Thyaga/backend/src/main/resources/application.yml).

Key configs:
*   `spring.datasource.url`: Database host endpoint (`jdbc:postgresql://db.cpdfuspjmwtyxjzhajkt.supabase.co:5432/postgres?sslmode=require`).
*   `supabase.secret-key`: Secret API key used to upload files and bypass Row-Level-Security (RLS) constraints.
*   `gemini.api.key`: Google Gemini API key.
*   `gemini.api.model`: `gemini-3.1-flash-lite` (stable version matching quota limits).

---

## 🏃 Local Setup & Startup

1.  **Navigate to the backend folder**:
    ```powershell
    cd d:\Thyaga\backend
    ```
2.  **Verify compilation & run tests**:
    ```powershell
    .\mvnw.cmd clean test
    ```
3.  **Start the server**:
    ```powershell
    .\mvnw.cmd spring-boot:run
    ```
4.  The server starts and exposes its endpoints at `http://localhost:8080`.
