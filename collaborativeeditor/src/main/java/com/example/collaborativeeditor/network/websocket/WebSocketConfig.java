package com.example.collaborativeeditor.network.websocket;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.*;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {
    private final DocumentWebSocketHandler documentHandler;

    public WebSocketConfig(DocumentWebSocketHandler documentHandler) {
        this.documentHandler = documentHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(documentHandler, "/document-ws")
                .setAllowedOrigins("*");
    }
}
