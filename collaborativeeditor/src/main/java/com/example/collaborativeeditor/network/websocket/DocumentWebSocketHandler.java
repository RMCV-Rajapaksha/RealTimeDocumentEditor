package com.example.collaborativeeditor.network.websocket;

import com.example.collaborativeeditor.model.DocumentEdit;
import com.example.collaborativeeditor.service.DocumentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;

@Slf4j
@Component
public class DocumentWebSocketHandler extends TextWebSocketHandler {
    private final ExecutorService messageProcessorPool;
    private final ConcurrentHashMap<String, Set<WebSocketSession>> documentSessions;
    private final ConcurrentHashMap<String, String> sessionUsernames; // Maps session IDs to usernames
    private final ConcurrentHashMap<String, Set<String>> documentUsers; // Maps document IDs to active usernames
    private final DocumentService documentService;
    private final ObjectMapper objectMapper;
    private final ConcurrentHashMap<String, Long> lastUpdateTimes;

    public DocumentWebSocketHandler(DocumentService documentService) {
        this.documentService = documentService;
        this.objectMapper = new ObjectMapper();
        this.messageProcessorPool = Executors.newFixedThreadPool(8);
        this.documentSessions = new ConcurrentHashMap<>();
        this.sessionUsernames = new ConcurrentHashMap<>();
        this.documentUsers = new ConcurrentHashMap<>();
        this.lastUpdateTimes = new ConcurrentHashMap<>();
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        log.info("WebSocket connection established: {}", session.getId());
        session.setTextMessageSizeLimit(65536);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        messageProcessorPool.submit(() -> {
            try {
                Map<String, Object> messageData = objectMapper.readValue(message.getPayload(), Map.class);
                String type = (String) messageData.get("type");
                String documentId = (String) messageData.get("documentId");
                String username = (String) messageData.get("username");

                if ("user_update".equals(type)) {
                    handleUserUpdate(session, documentId, username, (String) messageData.get("action"));
                } else {
                    handleDocumentEdit(session, messageData);
                }
            } catch (Exception e) {
                log.error("Error processing WebSocket message", e);
            }
        });
    }

    private void handleUserUpdate(WebSocketSession session, String documentId, String username, String action)
            throws IOException {
        if ("join".equals(action)) {
            // Store username for this session
            sessionUsernames.put(session.getId(), username);

            // Add session to document's session set
            documentSessions.computeIfAbsent(documentId, k -> ConcurrentHashMap.newKeySet()).add(session);

            // Add username to document's active users set
            documentUsers.computeIfAbsent(documentId, k -> ConcurrentHashMap.newKeySet()).add(username);

            // Broadcast updated user list to all sessions for this document
            broadcastUserList(documentId);
        } else if ("leave".equals(action)) {
            removeUserFromDocument(session, documentId);
        }
    }

    private void handleDocumentEdit(WebSocketSession session, Map<String, Object> edit) throws IOException {
        String documentId = (String) edit.get("documentId");

        // Rate limiting check
        Long lastUpdate = lastUpdateTimes.get(documentId);
        long now = System.currentTimeMillis();
        if (lastUpdate != null && now - lastUpdate < 50) {
            return;
        }
        lastUpdateTimes.put(documentId, now);

        // Update document
        documentService.updateDocument(documentId,
                (String) edit.get("content"),
                (String) edit.get("editor"));

        // Broadcast update to other users
        broadcastUpdate(session, edit);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, org.springframework.web.socket.CloseStatus status) {
        String username = sessionUsernames.remove(session.getId());
        documentSessions.forEach((documentId, sessions) -> {
            if (sessions.remove(session)) {
                Set<String> users = documentUsers.get(documentId);
                if (users != null && username != null) {
                    users.remove(username);
                    try {
                        broadcastUserList(documentId);
                    } catch (IOException e) {
                        log.error("Error broadcasting user list after connection closed", e);
                    }
                }
            }
        });
    }

    private void removeUserFromDocument(WebSocketSession session, String documentId) throws IOException {
        String username = sessionUsernames.get(session.getId());
        if (username != null) {
            Set<String> users = documentUsers.get(documentId);
            if (users != null) {
                users.remove(username);
                broadcastUserList(documentId);
            }
        }
    }

    private void broadcastUserList(String documentId) throws IOException {
        Set<WebSocketSession> sessions = documentSessions.get(documentId);
        Set<String> users = documentUsers.get(documentId);

        if (sessions != null && users != null) {
            Map<String, Object> message = Map.of(
                    "type", "user_update",
                    "documentId", documentId,
                    "users", users);

            String messageStr = objectMapper.writeValueAsString(message);
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

    private void broadcastUpdate(WebSocketSession sender, Map<String, Object> edit) throws IOException {
        String documentId = (String) edit.get("documentId");
        Set<WebSocketSession> sessions = documentSessions.get(documentId);

        if (sessions != null) {
            String message = objectMapper.writeValueAsString(edit);
            TextMessage textMessage = new TextMessage(message);

            for (WebSocketSession session : sessions) {
                if (session.isOpen() && session != sender) {
                    synchronized (session) {
                        session.sendMessage(textMessage);
                    }
                }
            }
        }
    }
}