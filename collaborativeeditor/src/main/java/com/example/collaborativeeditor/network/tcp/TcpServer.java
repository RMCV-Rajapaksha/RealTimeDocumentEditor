package com.example.collaborativeeditor.network.tcp;

import com.example.collaborativeeditor.network.NetworkMessage;
import com.example.collaborativeeditor.service.DocumentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class TcpServer {
    private final ExecutorService acceptorThread;
    private final ExecutorService workerThreadPool;
    private final ConcurrentHashMap<String, Set<TcpClientHandler>> documentClients;
    private final DocumentService documentService;
    private volatile boolean running;

    public TcpServer(DocumentService documentService) {
        this.documentService = documentService;
        this.acceptorThread = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "tcp-acceptor");
            t.setDaemon(true);
            return t;
        });
        this.workerThreadPool = new ThreadPoolExecutor(
                2,
                10,
                60L,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(),
                r -> {
                    Thread t = new Thread(r, "tcp-worker-" + r.hashCode());
                    t.setDaemon(true);
                    return t;
                }
        );
        this.documentClients = new ConcurrentHashMap<>();
    }

    public void registerClient(String documentId, TcpClientHandler handler) {
        documentClients.computeIfAbsent(documentId, k -> ConcurrentHashMap.newKeySet()).add(handler);
    }

    public void removeClient(String documentId, TcpClientHandler handler) {
        Set<TcpClientHandler> clients = documentClients.get(documentId);
        if (clients != null) {
            clients.remove(handler);
            if (clients.isEmpty()) {
                documentClients.remove(documentId);
            }
        }
    }

    public void start() {
        running = true;
        acceptorThread.submit(() -> {
            try (ServerSocket serverSocket = new ServerSocket(8090)) {
                log.info("TCP Server started on port 8090");
                while (running) {
                    Socket clientSocket = serverSocket.accept();
                    log.info("New client connected from: {}", clientSocket.getInetAddress());
                    TcpClientHandler clientHandler = new TcpClientHandler(clientSocket, this);
                    workerThreadPool.submit(clientHandler);
                }
            } catch (Exception e) {
                log.error("Error in TCP server", e);
            }
        });
    }

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

    public DocumentService getDocumentService() {
        return documentService;
    }
}