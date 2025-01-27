package com.example.collaborativeeditor.model;

import lombok.Data;

@Data
public class DocumentEdit {
    private String documentId;
    private String type; //newly added by chatgpt
    private String content;
    private String editor;
    private long timestamp;
    private String username;//newly added by chatgpt
    private String action;//newly added by chatgpt
}