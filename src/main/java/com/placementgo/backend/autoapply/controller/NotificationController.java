package com.placementgo.backend.autoapply.controller;

import com.placementgo.backend.autoapply.dto.NotificationDto;
import com.placementgo.backend.autoapply.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    /**
     * SSE endpoint – the browser connects here once and receives all real-time events.
     * EventSource in the frontend auto-reconnects on disconnect.
     */
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@AuthenticationPrincipal UUID userId) {
        return notificationService.subscribe(userId);
    }

    @GetMapping
    public ResponseEntity<Page<NotificationDto>> list(@AuthenticationPrincipal UUID userId,
                                                       @RequestParam(defaultValue = "0") int page,
                                                       @RequestParam(defaultValue = "30") int size) {
        return ResponseEntity.ok(notificationService.getForUser(userId, page, size));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Long>> unreadCount(@AuthenticationPrincipal UUID userId) {
        return ResponseEntity.ok(Map.of("count", notificationService.countUnread(userId)));
    }

    @PostMapping("/mark-all-read")
    public ResponseEntity<Void> markAllRead(@AuthenticationPrincipal UUID userId) {
        notificationService.markAllRead(userId);
        return ResponseEntity.noContent().build();
    }
}
