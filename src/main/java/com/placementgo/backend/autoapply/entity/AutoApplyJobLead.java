package com.placementgo.backend.autoapply.entity;

import com.placementgo.backend.autoapply.enums.ApplyMethod;
import com.placementgo.backend.autoapply.enums.LeadStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "auto_apply_job_leads",
       indexes = {
           @Index(name = "idx_lead_user_status", columnList = "userId, status"),
           @Index(name = "idx_lead_external", columnList = "userId, externalJobId, source", unique = true)
       })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AutoApplyJobLead {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID userId;

    private String jobTitle;
    private String company;
    private String location;

    @Column(columnDefinition = "TEXT")
    private String jobDescription;

    @Column(length = 1000)
    private String applyUrl;

    private String applyEmail;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private ApplyMethod applyMethod = ApplyMethod.UNKNOWN;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private LeadStatus status = LeadStatus.PENDING_REVIEW;

    /** 0–100, assigned by AI matching service */
    private int aiMatchScore;

    /** JSON array of strings explaining why the job matches */
    @Column(columnDefinition = "TEXT")
    private String matchReasons;

    /**
     * JSON object with pre-filled fields + AI-generated cover letter.
     * Shape: { coverLetter, subject, resumeUrl, fields: { name, email, ... } }
     */
    @Column(columnDefinition = "TEXT")
    private String applicationTemplate;

    /** API/scraper source label */
    private String source;

    /** ID from source platform – used to prevent duplicate leads */
    private String externalJobId;

    @CreationTimestamp
    private LocalDateTime discoveredAt;

    private LocalDateTime appliedAt;
}
