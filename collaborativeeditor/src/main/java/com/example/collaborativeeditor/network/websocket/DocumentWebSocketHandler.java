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
import java.util.Set;
import java.util.concurrent.*;

@Slf4j
@Component
public class DocumentWebSocketHandler extends TextWebSocketHandler {
    private final ExecutorService messageProcessorPool;
    private final ConcurrentHashMap<String, Set<WebSocketSession>> documentSessions;
    private final DocumentService documentService;
    private final ObjectMapper objectMapper;

    public DocumentWebSocketHandler(DocumentService documentService) {
        this.documentService = documentService;
        this.objectMapper = new ObjectMapper();
        this.messageProcessorPool = new ThreadPoolExecutor(
                4,
                8,
                60L,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(),
                r -> {
                    Thread t = new Thread(r, "ws-processor-" + r.hashCode());
                    t.setDaemon(true);
                    return t;
                }
        );
        this.documentSessions = new ConcurrentHashMap<>();
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        log.info("WebSocket connection established: {}", session.getId());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        messageProcessorPool.submit(() -> {
            try {
                DocumentEdit edit = parseMessage(message.getPayload());
                documentService.updateDocument(edit.getDocumentId(), edit.getContent(), edit.getEditor());

                // Register session with document
                documentSessions.computeIfAbsent(
                        edit.getDocumentId(),
                        k -> ConcurrentHashMap.newKeySet()
                ).add(session);

                // Broadcast to other sessions
                broadcastUpdate(session, edit);
            } catch (Exception e) {
                log.error("Error processing WebSocket message", e);
            }
        });
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, org.springframework.web.socket.CloseStatus status) {
        // Remove session from all documents
        documentSessions.values().forEach(sessions -> sessions.remove(session));
        // Clean up empty sets
        documentSessions.entrySet().removeIf(entry -> entry.getValue().isEmpty());
    }

    private DocumentEdit parseMessage(String payload) throws IOException {
        return objectMapper.readValue(payload, DocumentEdit.class);
    }

    private String convertToJson(DocumentEdit edit) throws IOException {
        return objectMapper.writeValueAsString(edit);
    }

    private void broadcastUpdate(WebSocketSession sender, DocumentEdit edit) {
        Set<WebSocketSession> sessions = documentSessions.get(edit.getDocumentId());
        if (sessions != null) {
            sessions.forEach(session -> {
                if (session.isOpen() && session != sender) {
                    try {
                        session.sendMessage(new TextMessage(convertToJson(edit)));
                    } catch (IOException e) {
                        log.error("Error broadcasting update", e);
                    }
                }
            });
        }
    }
}