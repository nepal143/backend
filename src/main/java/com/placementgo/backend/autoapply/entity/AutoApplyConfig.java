package com.placementgo.backend.autoapply.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "auto_apply_configs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AutoApplyConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private UUID userId;

    // ── Search preferences ────────────────────────────────────────────────────

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "aac_job_titles", joinColumns = @JoinColumn(name = "config_id"))
    @Column(name = "job_title")
    @Builder.Default
    private List<String> targetJobTitles = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "aac_locations", joinColumns = @JoinColumn(name = "config_id"))
    @Column(name = "location")
    @Builder.Default
    private List<String> preferredLocations = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "aac_blacklisted", joinColumns = @JoinColumn(name = "config_id"))
    @Column(name = "company")
    @Builder.Default
    private List<String> blacklistedCompanies = new ArrayList<>();

    /** Entry / Mid / Senior / Any */
    private String experienceLevel;

    // ── Automation settings ───────────────────────────────────────────────────

    /** Master switch for the scheduled job-discovery + auto-apply pipeline */
    @Builder.Default
    private boolean autoApplyEnabled = false;

    /** Allow auto-sending resume via email when applyEmail is available */
    @Builder.Default
    private boolean emailApplyEnabled = true;

    /** Cap per 24-hour window to avoid spam */
    @Builder.Default
    private int maxApplicationsPerDay = 20;

    /** Minimum AI match score (0–100) required before acting */
    @Builder.Default
    private int minAiMatchScore = 60;

    /** UUID of the Resume entity the user wants to use */
    private UUID resumeId;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
