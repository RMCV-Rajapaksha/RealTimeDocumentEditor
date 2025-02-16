package com.example.collaborativeeditor.model;

import lombok.Data;
import java.util.UUID;

@Data
public class Document {
    private String id;
    private String content;
    private String lastEditor;
    private long lastEditTime;
    private String documentType; // Add this field to distinguish between plain text and rich text

    public Document() {
        this.id = UUID.randomUUID().toString();
        this.content = "";
        this.lastEditTime = System.currentTimeMillis();
        this.documentType = "rich-text"; // Default to rich text for Quill editor
    }
}