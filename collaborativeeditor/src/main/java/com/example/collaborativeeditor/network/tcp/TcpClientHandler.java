package com.example.collaborativeeditor.network.tcp;

import com.example.collaborativeeditor.network.NetworkMessage;
import lombok.extern.slf4j.Slf4j;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Handles TCP socket communication for a single client connection in the collaborative editor.
 * Each instance runs in its own thread and manages message exchange between the client and server.
 */
@Slf4j
public class TcpClientHandler implements Runnable {
    // Socket connection to the client
    private final Socket clientSocket;
    // Reference to the main server for broadcasting and client management
    private final TcpServer server;
    // Input stream for receiving client messages
    private final InputStream in;
    // Output stream for sending messages to client
    private final OutputStream out;
    // JSON serialization/deserialization utility
    private final ObjectMapper objectMapper;
    // Flag to control the message processing loop
    private volatile boolean running;
    // ID of the document this client is currently editing
    private String currentDocumentId;

    /**
     * Creates a new handler for a client connection
     * @param socket The client's socket connection
     * @param server Reference to the main TCP server
     * @throws IOException If streams cannot be obtained from socket
     */
    public TcpClientHandler(Socket socket, TcpServer server) throws IOException {
        this.clientSocket = socket;
        this.server = server;
        this.in = socket.getInputStream();
        this.out = socket.getOutputStream();
        this.objectMapper = new ObjectMapper();
        this.running = true;
    }

    /**
     * Main processing loop that handles incoming messages from the client
     */
    @Override
    public void run() {
        try {
            while (running) {
                // Message format: [1 byte type][4 bytes length][payload]
                byte[] header = new byte[5];
                if (readFully(header) < 5) break;

                // Extract message type and payload length
                byte type = header[0];
                int length = ByteBuffer.wrap(header, 1, 4).getInt();

                // Read the actual message content
                byte[] payload = new byte[length];
                if (readFully(payload) < length) break;

                processMessage(type, payload);
            }
        } catch (IOException e) {
            log.error("Error handling client connection", e);
        } finally {
            cleanup();
        }
    }

    /**
     * Processes incoming messages based on their type
     * Message format: documentId|content|userId
     * @param type The type of message (JOIN, EDIT, LEAVE)
     * @param payload The message content
     */
    private void processMessage(byte type, byte[] payload) throws IOException {
        String payloadStr = new String(payload);
        String[] parts = payloadStr.split("\\|");

        if (parts.length < 3) {
            log.error("Invalid message format");
            return;
        }

        String documentId = parts[0];
        String content = parts[1];
        String userId = parts[2];

        switch (type) {
            case NetworkMessage.MESSAGE_TYPE_JOIN:
                // Register client with document session
                currentDocumentId = documentId;
                server.registerClient(documentId, this);
                break;
            case NetworkMessage.MESSAGE_TYPE_EDIT:
                // Update document and broadcast changes
                server.getDocumentService().updateDocument(documentId, content, userId);
                NetworkMessage editMessage = new NetworkMessage();
                editMessage.setType(type);
                editMessage.setDocumentId(documentId);
                editMessage.setContent(content);
                editMessage.setUserId(userId);
                server.broadcastToDocument(documentId, editMessage, this);
                break;
            case NetworkMessage.MESSAGE_TYPE_LEAVE:
                // Remove client from document session
                if (currentDocumentId != null) {
                    server.removeClient(currentDocumentId, this);
                }
                break;
        }
    }

    /**
     * Ensures complete reading of the requested number of bytes
     * @param buffer Buffer to read into
     * @return Number of bytes actually read
     */
    private int readFully(byte[] buffer) throws IOException {
        int totalRead = 0;
        while (totalRead < buffer.length) {
            int read = in.read(buffer, totalRead, buffer.length - totalRead);
            if (read == -1) return totalRead;
            totalRead += read;
        }
        return totalRead;
    }

    /**
     * Sends a message to the client
     * @param message The message to send
     */
    public void sendMessage(NetworkMessage message) {
        try {
            byte[] data = message.serialize();
            synchronized (out) {
                out.write(data);
                out.flush();
            }
        } catch (IOException e) {
            log.error("Error sending message", e);
            cleanup();
        }
    }

    /**
     * Performs cleanup when connection terminates
     */
    private void cleanup() {
        running = false;
        if (currentDocumentId != null) {
            server.removeClient(currentDocumentId, this);
        }
        try {
            clientSocket.close();
        } catch (IOException e) {
            log.error("Error closing client socket", e);
        }
    }
}