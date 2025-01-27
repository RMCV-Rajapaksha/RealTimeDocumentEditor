package com.example.collaborativeeditor.network.tcp;

import com.example.collaborativeeditor.network.NetworkMessage;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import com.fasterxml.jackson.databind.ObjectMapper;

@Slf4j
public class TcpClientHandler implements Runnable {
    private final Socket clientSocket;
    private final TcpServer server;
    private final InputStream in;
    private final OutputStream out;
    private final ObjectMapper objectMapper;
    private volatile boolean running;
    private String currentDocumentId;

    public TcpClientHandler(Socket socket, TcpServer server) throws IOException {
        this.clientSocket = socket;
        this.server = server;
        this.in = socket.getInputStream();
        this.out = socket.getOutputStream();
        this.objectMapper = new ObjectMapper();
        this.running = true;
    }

    @Override
    public void run() {
        try {
            while (running) {
                // Read message header
                byte[] header = new byte[5];
                if (readFully(header) < 5) break;

                byte type = header[0];
                int length = ByteBuffer.wrap(header, 1, 4).getInt();

                // Read payload
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
                currentDocumentId = documentId;
                server.registerClient(documentId, this);
                break;
            case NetworkMessage.MESSAGE_TYPE_EDIT:
                server.getDocumentService().updateDocument(documentId, content, userId);
                NetworkMessage editMessage = new NetworkMessage();
                editMessage.setType(type);
                editMessage.setDocumentId(documentId);
                editMessage.setContent(content);
                editMessage.setUserId(userId);
                server.broadcastToDocument(documentId, editMessage, this);
                break;
            case NetworkMessage.MESSAGE_TYPE_LEAVE:
                if (currentDocumentId != null) {
                    server.removeClient(currentDocumentId, this);
                }
                break;
        }
    }

    private int readFully(byte[] buffer) throws IOException {
        int totalRead = 0;
        while (totalRead < buffer.length) {
            int read = in.read(buffer, totalRead, buffer.length - totalRead);
            if (read == -1) return totalRead;
            totalRead += read;
        }
        return totalRead;
    }

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