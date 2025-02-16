// ChatService.java
package com.example.collaborativeeditor.service;

import com.example.collaborativeeditor.model.ChatMessage;
import com.example.collaborativeeditor.model.ChatRoom;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.ArrayList;
import java.util.Collections;

@Slf4j
@Service
public class ChatService {
    private final ConcurrentHashMap<String, ChatRoom> chatRooms;
    private static final int MAX_CHAT_HISTORY = 200;

    public ChatService() {
        this.chatRooms = new ConcurrentHashMap<>();
    }

    public void addMessage(String documentId, ChatMessage message) {
        ChatRoom room = chatRooms.computeIfAbsent(documentId, 
            id -> new ChatRoom(id));
        
        room.getMessageHistory().add(message);
        room.setLastActivityAt(System.currentTimeMillis());

        // Trim history if needed
        if (room.getMessageHistory().size() > MAX_CHAT_HISTORY) {
            List<ChatMessage> messages = room.getMessageHistory();
            room.setMessageHistory(new ArrayList<>(
                messages.subList(messages.size() - MAX_CHAT_HISTORY, messages.size())
            ));
        }
    }

    public List<ChatMessage> getMessageHistory(String documentId) {
        ChatRoom room = chatRooms.get(documentId);
        return room != null ? room.getMessageHistory() : Collections.emptyList();
    }

    public void addUserToRoom(String documentId, String username) {
        ChatRoom room = chatRooms.computeIfAbsent(documentId, 
            id -> new ChatRoom(id));
        room.getActiveUsers().add(username);
    }

    public void removeUserFromRoom(String documentId, String username) {
        ChatRoom room = chatRooms.get(documentId);
        if (room != null) {
            room.getActiveUsers().remove(username);
            if (room.getActiveUsers().isEmpty()) {
                // Optional: Remove empty chat rooms after some time
                cleanupEmptyRoom(documentId);
            }
        }
    }

    public List<String> getActiveUsers(String documentId) {
        ChatRoom room = chatRooms.get(documentId);
        return room != null ? room.getActiveUsers() : Collections.emptyList();
    }

    private void cleanupEmptyRoom(String documentId) {
        ChatRoom room = chatRooms.get(documentId);
        if (room != null && room.getActiveUsers().isEmpty()) {
            // Remove room if it's been inactive for more than 24 hours
            long inactiveTime = System.currentTimeMillis() - room.getLastActivityAt();
            if (inactiveTime > 24 * 60 * 60 * 1000) {
                chatRooms.remove(documentId);
                log.info("Removed inactive chat room: {}", documentId);
            }
        }
    }
}