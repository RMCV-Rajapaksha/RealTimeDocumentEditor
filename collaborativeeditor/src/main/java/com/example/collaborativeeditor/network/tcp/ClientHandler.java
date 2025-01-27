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

@Slf4j
public class ClientHandler implements Runnable {
    private final Socket clientSocket;
    private final DocumentService documentService;
    private final ObjectMapper objectMapper;

    public ClientHandler(Socket socket, DocumentService documentService, ObjectMapper objectMapper) {
        this.clientSocket = socket;
        this.documentService = documentService;
        this.objectMapper = objectMapper;
    }

    @Override
    public void run() {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
             PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)) {

            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                try {
                    DocumentEdit edit = objectMapper.readValue(inputLine, DocumentEdit.class);
                    documentService.updateDocument(edit.getDocumentId(), edit.getContent(), edit.getEditor());
                    out.println("Update successful");
                } catch (Exception e) {
                    log.error("Error processing edit", e);
                    out.println("Error processing edit");
                }
            }
        } catch (IOException e) {
            log.error("Error handling client connection", e);
        }
    }
}
