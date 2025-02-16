package com.example.collaborativeeditor.network.tcp;

import com.example.collaborativeeditor.model.DocumentEdit;
import com.example.collaborativeeditor.service.DocumentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

// ... imports ...

/**
 * Handles individual client connections for collaborative document editing.
 * Each client gets its own instance of this handler running in a separate
 * thread.
 */
@Slf4j
public class ClientHandler implements Runnable {
    // Socket representing the connection to a single client
    private final Socket clientSocket;
    // Service for managing document operations
    private final DocumentService documentService;
    // JSON mapper for serialization/deserialization
    private final ObjectMapper objectMapper;

    /**
     * Creates a new handler for a client connection
     * 
     * @param socket          The client's socket connection
     * @param documentService Service to handle document operations
     * @param objectMapper    JSON mapper for parsing messages
     */
    public ClientHandler(Socket socket, DocumentService documentService, ObjectMapper objectMapper) {
        this.clientSocket = socket;
        this.documentService = documentService;
        this.objectMapper = objectMapper;
    }

    @Override
    public void run() {
        // Create readers and writers for socket communication
        try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)) {

            String inputLine;
            // Continuously read messages from the client
            while ((inputLine = in.readLine()) != null) {
                try {
                    // Parse the incoming JSON message into a DocumentEdit object
                    DocumentEdit edit = objectMapper.readValue(inputLine, DocumentEdit.class);
                    // Apply the edit to the document
                    documentService.updateDocument(edit.getDocumentId(), edit.getContent(), edit.getEditor());
                    // Confirm successful update to the client
                    out.println("Update successful");
                } catch (Exception e) {
                    // Log any errors during message processing
                    log.error("Error processing edit", e);
                    // Inform client of the error
                    out.println("Error processing edit");
                }
            }
        } catch (IOException e) {
            // Log any errors with the socket connection
            log.error("Error handling client connection", e);
        }
    }
}