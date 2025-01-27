package com.example.collaborativeeditor.controller;

import com.example.collaborativeeditor.model.Document;
import com.example.collaborativeeditor.service.DocumentService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/documents")
public class DocumentController {
    private final DocumentService documentService;

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    @GetMapping("/new")
    public String createDocument() {
        Document document = documentService.createDocument();
        return "redirect:/documents/" + document.getId();
    }

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
