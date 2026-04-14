package com.placementgo.backend.autoapply.dto;

import lombok.Builder;
import lombok.Data;

/** Lightweight event pushed over SSE to the browser. */
@Data
@Builder
public class SseEvent {
    private String type;    // mirrors AppNotification.type
    private String title;
    private String message;
    private String metadata; // JSON
}
