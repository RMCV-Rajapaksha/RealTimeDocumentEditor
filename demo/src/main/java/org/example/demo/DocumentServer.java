package org.example.demo;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DocumentServer {
    private static final int PORT = 8081;
    private final DocumentState documentState;
    private final ConcurrentHashMap<String, ClientHandler> clients;
    private final ExecutorService executorService;

    public DocumentServer() {
        documentState = new DocumentState();
        clients = new ConcurrentHashMap<>();
        executorService = Executors.newCachedThreadPool();
    }

    public void start() {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Server started on port " + PORT);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                String clientId = UUID.randomUUID().toString();
                ClientHandler clientHandler = new ClientHandler(clientId, clientSocket, this);
                clients.put(clientId, clientHandler);
                executorService.execute(clientHandler);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public synchronized void broadcastMessage(EditMessage message, String senderId) {
        // First broadcast the message to all clients
        clients.forEach((clientId, handler) -> {
            if (!clientId.equals(senderId)) {
                handler.sendMessage(message);
            }
        });
        
        // Then apply the edit to the document state
        documentState.applyEdit(message);
    }

    public void removeClient(String clientId) {
        clients.remove(clientId);
        broadcastMessage(new EditMessage(clientId, EditMessage.MessageType.DISCONNECT, "", 0, null), clientId);
    }

    private class ClientHandler implements Runnable {
        private final String clientId;
        private final Socket socket;
        private final DocumentServer server;
        private ObjectOutputStream out;
        private ObjectInputStream in;

        public ClientHandler(String clientId, Socket socket, DocumentServer server) {
            this.clientId = clientId;
            this.socket = socket;
            this.server = server;
        }

        @Override
        public void run() {
            try {
                out = new ObjectOutputStream(socket.getOutputStream());
                in = new ObjectInputStream(socket.getInputStream());

                // Send initial document state
                sendMessage(new EditMessage(clientId, EditMessage.MessageType.CONNECT,
                        documentState.getContent(), 0, null));

                // Handle incoming messages
                while (true) {
                    EditMessage message = (EditMessage) in.readObject();
                    server.broadcastMessage(message, clientId);
                }
            } catch (IOException | ClassNotFoundException e) {
                server.removeClient(clientId);
            }
        }

        public void sendMessage(EditMessage message) {
            try {
                out.writeObject(message);
                out.flush();
            } catch (IOException e) {
                server.removeClient(clientId);
            }
        }
    }

    public static void main(String[] args) {
        new DocumentServer().start();
    }
}