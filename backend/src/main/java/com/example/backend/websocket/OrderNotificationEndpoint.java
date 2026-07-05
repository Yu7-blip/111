package com.example.backend.websocket;

import jakarta.websocket.*;
import jakarta.websocket.server.ServerEndpoint;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@ServerEndpoint("/ws/order/{userId}")
public class OrderNotificationEndpoint {

    private static final Map<String, Session> sessions = new ConcurrentHashMap<>();

    @OnOpen
    public void onOpen(Session session) {
        String userId = getUserId(session);
        if (userId != null) {
            sessions.put(userId, session);
            log.info("WebSocket connected: userId={}", userId);
        }
    }

    @OnClose
    public void onClose(Session session) {
        String userId = getUserId(session);
        if (userId != null) {
            sessions.remove(userId);
            log.info("WebSocket disconnected: userId={}", userId);
        }
    }

    @OnError
    public void onError(Session session, Throwable error) {
        log.error("WebSocket error: userId={}, msg={}", getUserId(session), error.getMessage());
    }

    public static void sendToUser(String userId, String message) {
        Session session = sessions.get(userId);
        if (session != null && session.isOpen()) {
            try {
                session.getBasicRemote().sendText(message);
            } catch (IOException e) {
                log.error("WebSocket send failed: userId={}", userId, e);
            }
        }
    }

    public static boolean isUserOnline(String userId) {
        Session session = sessions.get(userId);
        return session != null && session.isOpen();
    }

    private String getUserId(Session session) {
        String path = session.getRequestURI().getPath();
        // Extract userId from /ws/order/{userId}
        int lastSlash = path.lastIndexOf('/');
        return lastSlash >= 0 ? path.substring(lastSlash + 1) : null;
    }
}
