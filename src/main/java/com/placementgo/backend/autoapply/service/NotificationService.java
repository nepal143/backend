package com.placementgo.backend.autoapply.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.placementgo.backend.autoapply.dto.NotificationDto;
import com.placementgo.backend.autoapply.dto.SseEvent;
import com.placementgo.backend.autoapply.entity.AppNotification;
import com.placementgo.backend.autoapply.repository.AppNotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
@RequiredArgsConstructor
public class NotificationService {

    private final AppNotificationRepository notificationRepository;
    private final ObjectMapper objectMapper;

    // Registered SSE connections: userId → emitter
    private final Map<UUID, SseEmitter> emitters = new ConcurrentHashMap<>();

    // ── SSE subscription ──────────────────────────────────────────────────────

    public SseEmitter subscribe(UUID userId) {
        // 30-minute timeout; frontend reconnects automatically
        SseEmitter emitter = new SseEmitter(30 * 60 * 1000L);

        emitters.put(userId, emitter);

        emitter.onCompletion(() -> emitters.remove(userId));
        emitter.onTimeout(() -> {
            emitter.complete();
            emitters.remove(userId);
        });
        emitter.onError(e -> emitters.remove(userId));

        // Send a keep-alive comment so the connection doesn't time out prematurely
        try {
            emitter.send(SseEmitter.event().comment("connected"));
        } catch (IOException e) {
            emitters.remove(userId);
        }

        return emitter;
    }

    // ── Push + persist  ───────────────────────────────────────────────────────

    /**
     * Persists a notification and pushes it to the user's SSE stream if connected.
     */
    public void push(UUID userId, String type, String title, String message, Object metadataObj) {
        String metadataJson = toJson(metadataObj);

        AppNotification saved = notificationRepository.save(
                AppNotification.builder()
                        .userId(userId)
                        .type(type)
                        .title(title)
                        .message(message)
                        .metadata(metadataJson)
                        .build()
        );

        SseEmitter emitter = emitters.get(userId);
        if (emitter != null) {
            try {
                SseEvent event = SseEvent.builder()
                        .type(type)
                        .title(title)
                        .message(message)
                        .metadata(metadataJson)
                        .build();
                emitter.send(SseEmitter.event()
                        .id(saved.getId().toString())
                        .name(type)
                        .data(objectMapper.writeValueAsString(event)));
            } catch (IOException e) {
                log.warn("SSE send failed for user {}, removing emitter", userId);
                emitters.remove(userId);
            }
        }
    }

    // ── Queries ───────────────────────────────────────────────────────────────

    public Page<NotificationDto> getForUser(UUID userId, int page, int size) {
        return notificationRepository
                .findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(page, size))
                .map(this::toDto);
    }

    public long countUnread(UUID userId) {
        return notificationRepository.countByUserIdAndIsRead(userId, false);
    }

    public void markAllRead(UUID userId) {
        notificationRepository.markAllReadForUser(userId);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private NotificationDto toDto(AppNotification n) {
        return NotificationDto.builder()
                .id(n.getId())
                .type(n.getType())
                .title(n.getTitle())
                .message(n.getMessage())
                .metadata(n.getMetadata())
                .isRead(n.isRead())
                .createdAt(n.getCreatedAt())
                .build();
    }

    private String toJson(Object obj) {
        if (obj == null) return null;
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return "{}";
        }
    }
}
