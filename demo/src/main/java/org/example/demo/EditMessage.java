package org.example.demo;

import java.io.Serializable;
import java.util.UUID;

// Message class for communication between client and server
public class EditMessage implements Serializable {
    private String userId;
    private MessageType type;
    private String content;
    private int position;
    private TextStyle style;

    public enum MessageType {
        CONNECT, DISCONNECT, EDIT, STYLE_CHANGE, CURSOR_MOVE
    }

    public enum TextStyle {
        NORMAL, BOLD, ITALIC, UNDERLINE
    }

    // Constructor
    public EditMessage(String userId, MessageType type, String content, int position, TextStyle style) {
        this.userId = userId;
        this.type = type;
        this.content = content;
        this.position = position;
        this.style = style;
    }

    // Getters and setters
    public String getUserId() { return userId; }
    public MessageType getType() { return type; }
    public String getContent() { return content; }
    public int getPosition() { return position; }
    public TextStyle getStyle() { return style; }
}