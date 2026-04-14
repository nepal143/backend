package com.placementgo.backend.autoapply.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "app_notifications",
       indexes = @Index(name = "idx_notif_user_read", columnList = "userId, isRead"))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppNotification {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID userId;

    /**
     * JOB_FOUND | EMAIL_SENT | AUTO_APPLIED | MANUAL_REQUIRED | APPLY_FAILED | DAILY_SUMMARY
     */
    @Column(nullable = false)
    private String type;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String message;

    /**
     * JSON – e.g. { "jobLeadId": "...", "company": "...", "role": "..." }
     * Used by the frontend to build deep links.
     */
    @Column(columnDefinition = "TEXT")
    private String metadata;

    @Builder.Default
    private boolean isRead = false;

    @CreationTimestamp
    private LocalDateTime createdAt;
}
