# Collaborative Document Editor - Network & Threading Technical Documentation

## Overview
This document provides a detailed analysis of the networking protocols and thread handling mechanisms implemented in the collaborative document editor project. The system supports real-time document editing through both WebSocket and TCP protocols, with careful consideration given to thread safety and scalability.

## Network Architecture

### 1. Dual Protocol Support
The system implements two distinct networking protocols:
- WebSocket (Primary Protocol)
- TCP Socket (Secondary Protocol)

This dual-protocol approach provides flexibility and ensures backward compatibility.

### 2. WebSocket Implementation

#### Key Components
- `DocumentWebSocketHandler`: Manages WebSocket connections and message routing
- `WebSocketConfig`: Configures WebSocket endpoints and settings

#### Message Flow
1. **Connection Establishment**
```java
@Override
public void afterConnectionEstablished(WebSocketSession session) {
    log.info("WebSocket connection established: {}", session.getId());
    session.setTextMessageSizeLimit(65536); // 64KB message limit
}
```

2. **Message Processing**
```java
@Override
protected void handleTextMessage(WebSocketSession session, TextMessage message) {
    messageProcessorPool.submit(() -> {
        try {
            Map<String, Object> messageData = objectMapper.readValue(message.getPayload(), Map.class);
            String type = (String) messageData.get("type");
            
            if ("user_update".equals(type)) {
                handleUserUpdate(session, documentId, username, action);
            } else {
                handleDocumentEdit(session, messageData);
            }
        } catch (Exception e) {
            log.error("Error processing WebSocket message", e);
        }
    });
}
```

#### Rate Limiting
The WebSocket implementation includes built-in rate limiting:
```java
Long lastUpdate = lastUpdateTimes.get(documentId);
long now = System.currentTimeMillis();
if (lastUpdate != null && now - lastUpdate < 50) { // 50ms minimum between updates
    return;
}
```

### 3. TCP Socket Implementation

#### Components
- `TcpServer`: Manages TCP connections and client handling
- `TcpClientHandler`: Handles individual TCP client connections
- `NetworkMessage`: Encapsulates message protocol

#### Message Protocol
The TCP implementation uses a custom binary protocol:
```
[Type (1 byte)][Length (4 bytes)][Payload (Length bytes)]
```

Message Types:
- `MESSAGE_TYPE_EDIT` (1): Document edit operations
- `MESSAGE_TYPE_JOIN` (2): Client joining a document
- `MESSAGE_TYPE_LEAVE` (3): Client leaving a document

#### Message Serialization
```java
public byte[] serialize() {
    String payload = String.format("%s|%s|%s|%d", 
        documentId, content, userId, timestamp);
    byte[] payloadBytes = payload.getBytes(StandardCharsets.UTF_8);
    byte[] message = new byte[5 + payloadBytes.length];
    
    // Add message type
    message[0] = type;
    
    // Add payload length (big-endian)
    message[1] = (byte) ((payloadBytes.length >> 24) & 0xFF);
    message[2] = (byte) ((payloadBytes.length >> 16) & 0xFF);
    message[3] = (byte) ((payloadBytes.length >> 8) & 0xFF);
    message[4] = (byte) (payloadBytes.length & 0xFF);
    
    // Add payload
    System.arraycopy(payloadBytes, 0, message, 5, payloadBytes.length);
    return message;
}
```

## Thread Handling

### 1. Thread Pool Architecture

#### WebSocket Thread Pools
```java
private final ExecutorService messageProcessorPool = Executors.newFixedThreadPool(8);
```
- Fixed thread pool for processing WebSocket messages
- 8 threads handle concurrent message processing
- Prevents thread explosion under high load

#### TCP Thread Pools
```java
// Acceptor thread for new connections
private final ExecutorService acceptorThread = Executors.newSingleThreadExecutor();

// Worker thread pool for client handling
private final ExecutorService workerThreadPool = new ThreadPoolExecutor(
    2,                 // Core pool size
    10,               // Maximum pool size
    60L,              // Thread keep-alive time
    TimeUnit.SECONDS, // Time unit
    new LinkedBlockingQueue<>()
);
```

### 2. Thread Safety Mechanisms

#### Concurrent Collections
The system uses thread-safe collections throughout:
```java
// Document-to-clients mapping
private final ConcurrentHashMap<String, Set<TcpClientHandler>> documentClients;

// Session tracking
private final ConcurrentHashMap<String, Set<WebSocketSession>> documentSessions;
private final ConcurrentHashMap<String, String> sessionUsernames;
```

#### Synchronized Blocks
Critical sections are protected:
```java
// Synchronized message sending
synchronized (session) {
    session.sendMessage(textMessage);
}

// Synchronized document updates
synchronized (document) {
    document.setContent(content);
    document.setLastEditTime(System.currentTimeMillis());
}
```

### 3. Resource Management

#### Connection Cleanup
```java
private void cleanup() {
    running = false;
    if (currentDocumentId != null) {
        server.removeClient(currentDocumentId, this);
    }
    try {
        clientSocket.close();
    } catch (IOException e) {
        log.error("Error closing client socket", e);
    }
}
```

#### Session Management
```java
@Override
public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
    String username = sessionUsernames.remove(session.getId());
    documentSessions.forEach((documentId, sessions) -> {
        if (sessions.remove(session)) {
            Set<String> users = documentUsers.get(documentId);
            if (users != null && username != null) {
                users.remove(username);
                try {
                    broadcastUserList(documentId);
                } catch (IOException e) {
                    log.error("Error broadcasting user list", e);
                }
            }
        }
    });
}
```

## Performance Considerations

### 1. Message Broadcasting
- Efficient message broadcasting to document participants
- Excludes sender from broadcast to prevent echo
- Thread-safe implementation using synchronized blocks

### 2. Rate Limiting
- Prevents message flooding
- 50ms minimum interval between updates
- Configurable through system properties

### 3. Resource Limits
- WebSocket message size limit: 64KB
- Configurable thread pool sizes
- Connection timeouts and keep-alive settings

## Best Practices & Recommendations

1. **Monitor Thread Pool Usage**
   - Watch for thread pool saturation
   - Adjust pool sizes based on server capacity
   - Implement thread pool metrics

2. **Network Security**
   - Implement SSL/TLS for WebSocket connections
   - Add authentication mechanisms
   - Consider message encryption

3. **Error Handling**
   - Implement proper error recovery
   - Add circuit breakers for network operations
   - Monitor and log connection issues

4. **Scalability**
   - Consider implementing clustering
   - Add load balancing support
   - Implement proper connection pooling

## Conclusion
The system implements a robust and scalable architecture for real-time collaborative editing. The dual-protocol approach provides flexibility, while careful thread handling ensures reliable performance under load. Regular monitoring and tuning of thread pools and network parameters is recommended for optimal performance.
