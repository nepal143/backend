package com.placementgo.backend.autoapply.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class NotificationDto {
    private UUID id;
    private String type;
    private String title;
    private String message;
    private String metadata;   // JSON string
    private boolean isRead;
    private LocalDateTime createdAt;
}
