package com.example.collaborativeeditor.network.tcp;

import com.example.collaborativeeditor.network.NetworkMessage;
import com.example.collaborativeeditor.service.DocumentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Set;
import java.util.concurrent.*;

/**
 * TCP server that manages collaborative document editing sessions.
 * Handles multiple client connections and document broadcasting.
 */
@Slf4j
@Component
public class TcpServer {
    // Single thread for accepting new client connections
    private final ExecutorService acceptorThread;
    // Thread pool for handling client communication
    private final ExecutorService workerThreadPool;
    // Maps document IDs to sets of connected clients
    private final ConcurrentHashMap<String, Set<TcpClientHandler>> documentClients;
    // Service for document operations
    private final DocumentService documentService;
    // Server running status flag
    private volatile boolean running;

    /**
     * Initializes the TCP server with necessary components
     * 
     * @param documentService Service for handling document operations
     */
    public TcpServer(DocumentService documentService) {
        this.documentService = documentService;
        // Create single thread executor for accepting connections
        this.acceptorThread = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "tcp-acceptor");
            t.setDaemon(true);
            return t;
        });
        // Create thread pool for handling client communication
        this.workerThreadPool = new ThreadPoolExecutor(
                2, // Core pool size
                10, // Maximum pool size
                60L, // Thread keep-alive time
                TimeUnit.SECONDS, // Time unit for keep-alive
                new LinkedBlockingQueue<>(), // Work queue
                r -> { // Thread factory
                    Thread t = new Thread(r, "tcp-worker-" + r.hashCode());
                    t.setDaemon(true);
                    return t;
                });
        // Initialize concurrent map for tracking clients per document
        this.documentClients = new ConcurrentHashMap<>();
    }

    /**
     * Registers a client handler with a specific document
     * 
     * @param documentId The document being edited
     * @param handler    The client's handler
     */
    public void registerClient(String documentId, TcpClientHandler handler) {
        documentClients.computeIfAbsent(documentId, k -> ConcurrentHashMap.newKeySet()).add(handler);
    }

    /**
     * Removes a client handler from a document's set of clients
     * 
     * @param documentId The document being edited
     * @param handler    The client's handler to remove
     */
    public void removeClient(String documentId, TcpClientHandler handler) {
        Set<TcpClientHandler> clients = documentClients.get(documentId);
        if (clients != null) {
            clients.remove(handler);
            // Remove document entry if no clients remain
            if (clients.isEmpty()) {
                documentClients.remove(documentId);
            }
        }
    }

    /**
     * Starts the TCP server and begins accepting client connections
     */
    public void start() {
        running = true;
        acceptorThread.submit(() -> {
            try (ServerSocket serverSocket = new ServerSocket(8090)) {
                log.info("TCP Server started on port 8090");
                while (running) {
                    // Accept new client connection
                    Socket clientSocket = serverSocket.accept();
                    log.info("New client connected from: {}", clientSocket.getInetAddress());
                    // Create and submit new client handler
                    TcpClientHandler clientHandler = new TcpClientHandler(clientSocket, this);
                    workerThreadPool.submit(clientHandler);
                }
            } catch (Exception e) {
                log.error("Error in TCP server", e);
            }
        });
    }

    /**
     * Broadcasts a message to all clients editing a specific document
     * 
     * @param documentId The target document
     * @param message    The message to broadcast
     * @param sender     The client that sent the message (excluded from broadcast)
     */
    public void broadcastToDocument(String documentId, NetworkMessage message, TcpClientHandler sender) {
        Set<TcpClientHandler> clients = documentClients.get(documentId);
        if (clients != null) {
            clients.forEach(client -> {
                if (client != sender) {
                    client.sendMessage(message);
                }
            });
        }
    }

    /**
     * Returns the document service instance
     * 
     * @return DocumentService instance
     */
    public DocumentService getDocumentService() {
        return documentService;
    }
}