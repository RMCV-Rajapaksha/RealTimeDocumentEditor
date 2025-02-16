package com.example.collaborativeeditor.model;

import lombok.Data;
import java.util.UUID;

@Data
public class ChatMessage {
    private String id;
    private String documentId;
    private String userId;
    private String username;
    private String content;
    private long timestamp;
    private String messageType; // "CHAT", "USER_JOINED", "USER_LEFT", "SYSTEM"
    private boolean edited;
    private long editedTimestamp;

    public ChatMessage() {
        this.id = UUID.randomUUID().toString();
        this.timestamp = System.currentTimeMillis();
        this.edited = false;
    }

    public static ChatMessage createUserMessage(String documentId, String userId, String username, String content) {
        ChatMessage message = new ChatMessage();
        message.setDocumentId(documentId);
        message.setUserId(userId);
        message.setUsername(username);
        message.setContent(content);
        message.setMessageType("CHAT");
        return message;
    }

    public static ChatMessage createSystemMessage(String documentId, String username, String messageType) {
        ChatMessage message = new ChatMessage();
        message.setDocumentId(documentId);
        message.setUsername(username);
        message.setMessageType(messageType);
        
        String content;
        switch (messageType) {
            case "USER_JOINED":
                content = username + " joined the document";
                break;
            case "USER_LEFT":
                content = username + " left the document";
                break;
            default:
                content = "System notification";
        }
        message.setContent(content);
        
        return message;
    }
}
