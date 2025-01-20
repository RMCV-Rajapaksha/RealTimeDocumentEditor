package org.example.demo;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class DocumentState implements Serializable {
    private StringBuilder content;
    private Map<String, Integer> userCursors;
    private Map<Integer, EditMessage.TextStyle> styleMap;

    public DocumentState() {
        content = new StringBuilder();
        userCursors = new HashMap<>();
        styleMap = new HashMap<>();
    }

    public synchronized void applyEdit(EditMessage message) {
        switch (message.getType()) {
            case EDIT:
                int position = message.getPosition();
                if (position >= 0 && position <= content.length()) {
                    content.insert(position, message.getContent());
                    updateStyleMap(position, message.getContent().length(), message.getStyle());
                } else {
                    System.err.println("Invalid position: " + position);
                }
                break;
            case CURSOR_MOVE:
                userCursors.put(message.getUserId(), message.getPosition());
                break;
        }
    }

    private void updateStyleMap(int position, int length, EditMessage.TextStyle style) {
        for (int i = position; i < position + length; i++) {
            styleMap.put(i, style);
        }
    }

    // Getters
    public String getContent() {
        return content.toString();
    }

    public Map<String, Integer> getUserCursors() {
        return new HashMap<>(userCursors);
    }

    public EditMessage.TextStyle getStyleAtPosition(int position) {
        return styleMap.getOrDefault(position, EditMessage.TextStyle.NORMAL);
    }
}