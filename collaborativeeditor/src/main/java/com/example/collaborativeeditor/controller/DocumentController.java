package com.example.collaborativeeditor.controller;

import com.example.collaborativeeditor.model.Document;
import com.example.collaborativeeditor.service.DocumentService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

/**
 * Controller class that handles HTTP requests related to document operations
 * Maps to the "/documents" endpoint
 */
@Controller
@RequestMapping("/documents")
public class DocumentController {
    // Service layer dependency for handling document-related business logic
    private final DocumentService documentService;

    /**
     * Constructor injection of DocumentService
     * 
     * @param documentService Service class for document operations
     */
    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    /**
     * Handles GET requests to create a new document
     * Endpoint: /documents/new
     * 
     * @return Redirects to the newly created document's edit page
     */
    @GetMapping("/new")
    public String createDocument() {
        Document document = documentService.createDocument();
        return "redirect:/documents/" + document.getId();
    }

    /**
     * Handles GET requests to view/edit an existing document
     * Endpoint: /documents/{id}
     * 
     * @param id    The unique identifier of the document
     * @param model Spring MVC Model object to pass data to the view
     * @return Returns the editor view or redirects to create new if document not
     *         found
     */
    @GetMapping("/{id}")
    public String getDocument(@PathVariable String id, Model model) {
        Document document = documentService.getDocument(id);
        if (document == null) {
            return "redirect:/documents/new";
        }
        model.addAttribute("document", document);
        return "editor";
    }
}