package com.example.collaborativeeditor.network;

import lombok.Data;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

/**
 * Represents a network message in the collaborative editing system.
 * Handles serialization and deserialization of messages between clients and
 * server.
 */
@Data
public class NetworkMessage {
    // Message type constants
    public static final byte MESSAGE_TYPE_EDIT = 1; // Document edit message
    public static final byte MESSAGE_TYPE_JOIN = 2; // Client joining document
    public static final byte MESSAGE_TYPE_LEAVE = 3; // Client leaving document

    // Message fields
    private byte type; // Type of message (EDIT, JOIN, LEAVE)
    private String documentId; // ID of the document being edited
    private String content; // Content/changes in the message
    private String userId; // ID of the user sending the message
    private long timestamp; // Message timestamp for ordering

    /**
     * Serializes the message into a byte array for network transmission.
     * Message format:
     * - Header: [type(1 byte)][payload length(4 bytes)]
     * - Payload: documentId|content|userId|timestamp
     * 
     * @return byte array containing the serialized message
     */
    public byte[] serialize() {
        // Create payload string with fields separated by '|'
        String payload = String.format("%s|%s|%s|%d", documentId, content, userId, timestamp);
        byte[] payloadBytes = payload.getBytes(StandardCharsets.UTF_8);
        byte[] message = new byte[5 + payloadBytes.length];

        // Add message type to header
        message[0] = type;

        // Add payload length to header (big-endian)
        message[1] = (byte) ((payloadBytes.length >> 24) & 0xFF);
        message[2] = (byte) ((payloadBytes.length >> 16) & 0xFF);
        message[3] = (byte) ((payloadBytes.length >> 8) & 0xFF);
        message[4] = (byte) (payloadBytes.length & 0xFF);

        // Copy payload after header
        System.arraycopy(payloadBytes, 0, message, 5, payloadBytes.length);
        return message;
    }

    /**
     * Deserializes a byte array back into a NetworkMessage object.
     * 
     * @param data The byte array containing the serialized message
     * @return A new NetworkMessage instance
     * @throws IllegalArgumentException if the message format is invalid
     */
    public static NetworkMessage deserialize(byte[] data) {
        try {
            NetworkMessage message = new NetworkMessage();

            // Extract message type from header
            message.setType(data[0]);

            // Extract payload length from header
            int length = ByteBuffer.wrap(data, 1, 4).getInt();

            // Extract and parse payload
            String payload = new String(data, 5, length, StandardCharsets.UTF_8);
            String[] parts = payload.split("\\|");

            if (parts.length != 4) {
                throw new IllegalArgumentException("Invalid message format");
            }

            message.setDocumentId(parts[0]);
            message.setContent(parts[1]);
            message.setUserId(parts[2]);
            message.setTimestamp(Long.parseLong(parts[3]));

            return message;
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to deserialize message", e);
        }
    }

    /**
     * Creates a new edit message
     */
    public static NetworkMessage createEditMessage(String documentId, String content, String userId) {
        NetworkMessage message = new NetworkMessage();
        message.setType(MESSAGE_TYPE_EDIT);
        message.setDocumentId(documentId);
        message.setContent(content);
        message.setUserId(userId);
        message.setTimestamp(System.currentTimeMillis());
        return message;
    }

    /**
     * Creates a new join message
     */
    public static NetworkMessage createJoinMessage(String documentId, String userId) {
        NetworkMessage message = new NetworkMessage();
        message.setType(MESSAGE_TYPE_JOIN);
        message.setDocumentId(documentId);
        message.setUserId(userId);
        message.setTimestamp(System.currentTimeMillis());
        return message;
    }

    /**
     * Creates a new leave message
     */
    public static NetworkMessage createLeaveMessage(String documentId, String userId) {
        NetworkMessage message = new NetworkMessage();
        message.setType(MESSAGE_TYPE_LEAVE);
        message.setDocumentId(documentId);
        message.setUserId(userId);
        message.setTimestamp(System.currentTimeMillis());
        return message;
    }
}