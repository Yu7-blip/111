package com.example.backend.websocket;

import com.example.backend.entity.Order;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final ObjectMapper objectMapper;

    public void notifyOrderStatusChange(Order order, String statusText) {
        Map<String, Object> data = new HashMap<>();
        data.put("type", "order_status");
        data.put("orderId", order.getId());
        data.put("orderNo", order.getOrderNo());
        data.put("status", order.getStatus());
        data.put("statusText", statusText);
        data.put("timestamp", LocalDateTime.now().toString());

        try {
            String msg = objectMapper.writeValueAsString(data);
            // Notify the user
            OrderNotificationEndpoint.sendToUser(String.valueOf(order.getUserId()), msg);
            log.info("WebSocket notified: userId={}, orderNo={}, status={}", order.getUserId(), order.getOrderNo(), statusText);
        } catch (Exception e) {
            log.error("WebSocket notification failed: {}", e.getMessage());
        }
    }
}
