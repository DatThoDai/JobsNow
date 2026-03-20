package com.JobsNow.backend.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Component
@Slf4j
@RequiredArgsConstructor
public class WebSocketEventListener {

    private final RedisTemplate<String, String> redisTemplate;
    private final SimpMessagingTemplate messagingTemplate;

    @EventListener
    public void handleWebSocketConnectListener(SessionConnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String userIdStr = (String) headerAccessor.getSessionAttributes().get("userId");
        if (userIdStr != null) {
            try {
                log.info("User Connected: {}", userIdStr);
                Long activeSessions = redisTemplate.opsForHash().increment("user:sessions", userIdStr, 1);
                if (activeSessions == 1) {
                    redisTemplate.opsForSet().add("online_users", userIdStr);
                    Map<String, Object> statusMap = new HashMap<>();
                    statusMap.put("userId", Integer.parseInt(userIdStr));
                    statusMap.put("isOnline", true);
                    messagingTemplate.convertAndSend("/topic/user.status", (Object) statusMap);
                }
            } catch (Exception e) {
                log.warn("Redis error during connect: {}", e.getMessage());
            }
        }
    }

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String userIdStr = (String) headerAccessor.getSessionAttributes().get("userId");
        if (userIdStr != null) {
            try {
                log.info("User Disconnected: {}", userIdStr);
                Long activeSessions = redisTemplate.opsForHash().increment("user:sessions", userIdStr, -1);
                if (activeSessions <= 0) {
                    redisTemplate.opsForHash().delete("user:sessions", userIdStr);
                    redisTemplate.opsForSet().remove("online_users", userIdStr);
                    redisTemplate.opsForValue().set("user:last_seen:" + userIdStr, LocalDateTime.now().toString(), Duration.ofDays(7));

                    Map<String, Object> statusMap = new HashMap<>();
                    statusMap.put("userId", Integer.parseInt(userIdStr));
                    statusMap.put("isOnline", false);
                    statusMap.put("lastSeen", LocalDateTime.now().toString());
                    messagingTemplate.convertAndSend("/topic/user.status", (Object) statusMap);
                }
            } catch (Exception e) {
                log.warn("Ignoring Redis error during disconnect (likely application shutdown): {}", e.getMessage());
            }
        }
    }
}
