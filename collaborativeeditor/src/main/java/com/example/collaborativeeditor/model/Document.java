package com.example.collaborativeeditor.model;

import lombok.Data;
import java.util.UUID;

@Data
public class Document {
    private String id;
    private String content;
    private String lastEditor;
    private long lastEditTime;

    public Document() {
        this.id = UUID.randomUUID().toString();
        this.content = "";
        this.lastEditTime = System.currentTimeMillis();
    }
}