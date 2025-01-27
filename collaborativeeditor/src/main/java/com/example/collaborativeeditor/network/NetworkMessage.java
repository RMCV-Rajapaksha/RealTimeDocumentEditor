package com.example.collaborativeeditor.network;

import lombok.Data;

@Data
public class NetworkMessage {
    public static final byte MESSAGE_TYPE_EDIT = 1;
    public static final byte MESSAGE_TYPE_JOIN = 2;
    public static final byte MESSAGE_TYPE_LEAVE = 3;

    private byte type;
    private String documentId;
    private String content;
    private String userId;
    private long timestamp;

    // Header format: [type(1 byte)][length(4 bytes)][payload...]
    public byte[] serialize() {
        String payload = String.format("%s|%s|%s|%d", documentId, content, userId, timestamp);
        byte[] payloadBytes = payload.getBytes();
        byte[] message = new byte[5 + payloadBytes.length];

        message[0] = type;
        message[1] = (byte) ((payloadBytes.length >> 24) & 0xFF);
        message[2] = (byte) ((payloadBytes.length >> 16) & 0xFF);
        message[3] = (byte) ((payloadBytes.length >> 8) & 0xFF);
        message[4] = (byte) (payloadBytes.length & 0xFF);

        System.arraycopy(payloadBytes, 0, message, 5, payloadBytes.length);
        return message;
    }
}