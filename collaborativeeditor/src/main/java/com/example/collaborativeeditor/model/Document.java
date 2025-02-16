package com.example.collaborativeeditor.model;

import lombok.Data;
import java.util.UUID;

/**
 * Document model class representing a collaborative document
 * Uses Lombok @Data annotation to automatically generate getters, setters,
 * equals, hashCode and toString methods
 */
@Data
public class Document {
    // Unique identifier for the document using UUID
    private String id;

    // The actual text content of the document
    private String content;

    // Username or identifier of the last user who edited the document
    private String lastEditor;

    // Timestamp of the last edit in milliseconds since epoch
    private long lastEditTime;

    /**
     * Default constructor that initializes a new document with:
     * - A random UUID as the document ID
     * - Empty content
     * - Current system time as the creation time
     */
    public Document() {
        this.id = UUID.randomUUID().toString();
        this.content = "";
        this.lastEditTime = System.currentTimeMillis();
    }
}