package com.example.collaborativeeditor.service;

import com.example.collaborativeeditor.model.Document;
import org.springframework.stereotype.Service;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class DocumentService {
    private final Map<String, Document> documents = new ConcurrentHashMap<>();

    public Document createDocument() {
        Document document = new Document();
        documents.put(document.getId(), document);
        return document;
    }

    public Document getDocument(String id) {
        return documents.get(id);
    }

    public void updateDocument(String id, String content, String editor) {
        Document document = documents.get(id);
        if (document != null) {
            document.setContent(content);
            document.setLastEditor(editor);
            document.setLastEditTime(System.currentTimeMillis());
        }
    }
}
