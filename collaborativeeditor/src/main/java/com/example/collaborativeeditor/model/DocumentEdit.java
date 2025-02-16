package com.example.collaborativeeditor.model;

import lombok.Data;

/**
 * Model class representing an edit operation on a document
 * Uses Lombok @Data annotation to automatically generate getters, setters,
 * equals, hashCode and toString methods
 */
@Data
public class DocumentEdit {
    // The unique identifier of the document being edited
    private String documentId;

    // The type of edit operation (e.g., "insert", "delete", "update")
    private String type;

    // The actual content being modified in the document
    private String content;

    // The identifier of the user/system making the edit
    private String editor;

    // Timestamp of when the edit occurred (milliseconds since epoch)
    private long timestamp;

    // Username of the person making the edit (for display purposes)
    private String username;

    // Specific action being performed (e.g., "bold", "italic", "text-change")
    private String action;
}