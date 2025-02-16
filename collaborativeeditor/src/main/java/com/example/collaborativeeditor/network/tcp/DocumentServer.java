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

/**
 * Server component that handles collaborative document editing sessions.
 * Uses TCP sockets for real-time communication with multiple clients.
 */
@Slf4j // Adds logging capability using SLF4J
@Component // Marks this class as a Spring component for dependency injection
public class DocumentServer {
    // Service for managing document operations and state
    private final DocumentService documentService;
    // Thread pool for handling multiple client connections concurrently
    private final ExecutorService executorService;
    // JSON mapper for serializing/deserializing messages
    private final ObjectMapper objectMapper;
    // Port number the server listens on
    private static final int PORT = 8090;

    /**
     * Initializes the document server and starts listening for connections
     * 
     * @param documentService injected service for document operations
     */
    public DocumentServer(DocumentService documentService) {
        this.documentService = documentService;
        // Creates a thread pool that creates new threads as needed
        this.executorService = Executors.newCachedThreadPool();
        this.objectMapper = new ObjectMapper();
        startServer();
    }

    /**
     * Starts the TCP server in a separate thread.
     * Continuously accepts new client connections and handles each in a separate
     * thread.
     */
    private void startServer() {
        executorService.submit(() -> {
            try (ServerSocket serverSocket = new ServerSocket(PORT)) {
                log.info("Document server started on port {}", PORT);
                // Keep accepting new connections until the thread is interrupted
                while (!Thread.currentThread().isInterrupted()) {
                    // Wait for and accept a new client connection
                    Socket clientSocket = serverSocket.accept();
                    // Create and submit a new handler for this client
                    executorService.submit(new ClientHandler(clientSocket, documentService, objectMapper));
                }
            } catch (IOException e) {
                log.error("Error in document server", e);
            }
        });
    }
}