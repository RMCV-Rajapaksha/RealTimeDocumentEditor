
// ChatRoom.java
package com.example.collaborativeeditor.model;

import lombok.Data;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Data
public class ChatRoom {
    private String documentId;
    private List<ChatMessage> messageHistory;
    private List<String> activeUsers;
    private long createdAt;
    private long lastActivityAt;

    public ChatRoom(String documentId) {
        this.documentId = documentId;
        this.messageHistory = new CopyOnWriteArrayList<>();
        this.activeUsers = new CopyOnWriteArrayList<>();
        this.createdAt = System.currentTimeMillis();
        this.lastActivityAt = this.createdAt;
    }
}