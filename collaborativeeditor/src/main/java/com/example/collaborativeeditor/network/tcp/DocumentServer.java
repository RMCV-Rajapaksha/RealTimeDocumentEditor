package com.example.collaborativeeditor.network.tcp;

import com.example.collaborativeeditor.service.DocumentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Component
public class DocumentServer {
    private final DocumentService documentService;
    private final ExecutorService executorService;
    private final ObjectMapper objectMapper;
    private static final int PORT = 8090;

    public DocumentServer(DocumentService documentService) {
        this.documentService = documentService;
        this.executorService = Executors.newCachedThreadPool();
        this.objectMapper = new ObjectMapper();
        startServer();
    }

    private void startServer() {
        executorService.submit(() -> {
            try (ServerSocket serverSocket = new ServerSocket(PORT)) {
                log.info("Document server started on port {}", PORT);
                while (!Thread.currentThread().isInterrupted()) {
                    Socket clientSocket = serverSocket.accept();
                    executorService.submit(new ClientHandler(clientSocket, documentService, objectMapper));
                }
            } catch (IOException e) {
                log.error("Error in document server", e);
            }
        });
    }
}