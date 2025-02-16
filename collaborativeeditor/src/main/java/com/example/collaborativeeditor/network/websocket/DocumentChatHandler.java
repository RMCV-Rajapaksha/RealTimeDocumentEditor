// DocumentChatHandler.java
package com.example.collaborativeeditor.network.websocket;

import com.example.collaborativeeditor.model.ChatMessage;
import com.example.collaborativeeditor.service.ChatService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class DocumentChatHandler {
    private final ChatService chatService;
    private final ObjectMapper objectMapper;
    private final ConcurrentHashMap<String, Set<WebSocketSession>> documentSessions;

    public DocumentChatHandler(ChatService chatService) {
        this.chatService = chatService;
        this.objectMapper = new ObjectMapper();
        this.documentSessions = new ConcurrentHashMap<>();
    }

    public void handleChatMessage(WebSocketSession session, Map<String, Object> messageData)
            throws IOException {
        String documentId = (String) messageData.get("documentId");
        String userId = (String) messageData.get("userId");
        String username = (String) messageData.get("username");
        String content = (String) messageData.get("content");

        ChatMessage chatMessage = ChatMessage.createUserMessage(
                documentId, userId, username, content);

        chatService.addMessage(documentId, chatMessage);
        broadcastChatMessage(documentId, chatMessage);
    }

    public void handleUserJoin(WebSocketSession session, String documentId, String username)
            throws IOException {
        // Add to active users
        chatService.addUserToRoom(documentId, username);

        // Send chat history
        sendChatHistory(session, documentId);

        // Broadcast join message
        ChatMessage joinMessage = ChatMessage.createSystemMessage(
                documentId, username, "USER_JOINED");
        chatService.addMessage(documentId, joinMessage);
        broadcastChatMessage(documentId, joinMessage);
    }

    public void handleUserLeave(String documentId, String username) throws IOException {
        chatService.removeUserFromRoom(documentId, username);

        ChatMessage leaveMessage = ChatMessage.createSystemMessage(
                documentId, username, "USER_LEFT");
        chatService.addMessage(documentId, leaveMessage);
        broadcastChatMessage(documentId, leaveMessage);
    }

    private void broadcastChatMessage(String documentId, ChatMessage message)
            throws IOException {
        Set<WebSocketSession> sessions = documentSessions.get(documentId);
        if (sessions != null) {
            String messageStr = objectMapper.writeValueAsString(Map.of(
                    "type", "chat",
                    "message", message));
            TextMessage textMessage = new TextMessage(messageStr);

            for (WebSocketSession session : sessions) {
                if (session.isOpen()) {
                    synchronized (session) {
                        session.sendMessage(textMessage);
                    }
                }
            }
        }
    }

    private void sendChatHistory(WebSocketSession session, String documentId)
            throws IOException {
        String historyStr = objectMapper.writeValueAsString(Map.of(
                "type", "chat_history",
                "messages", chatService.getMessageHistory(documentId)));

        synchronized (session) {
            session.sendMessage(new TextMessage(historyStr));
        }
    }

    public void registerSession(String documentId, WebSocketSession session) {
        documentSessions.computeIfAbsent(documentId,
                k -> ConcurrentHashMap.newKeySet()).add(session);
    }

    public void removeSession(String documentId, WebSocketSession session) {
        Set<WebSocketSession> sessions = documentSessions.get(documentId);
        if (sessions != null) {
            sessions.remove(session);
            if (sessions.isEmpty()) {
                documentSessions.remove(documentId);
            }
        }
    }
}