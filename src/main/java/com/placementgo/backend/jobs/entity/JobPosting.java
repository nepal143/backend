package com.placementgo.backend.jobs.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "job_postings")
@Getter
@Setter
public class JobPosting {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String companyName;

    private String location;

    @Column(length = 2000)
    private String descriptionSnippet;

    @Column(length = 1000)
    private String applyUrl;

    private String jobPlatformSource; // e.g., "LinkedIn", "Indeed"

    private String platformJobId;

    private boolean isInternal; // True if posted directly on PlacementGo

    @Column(nullable = false)
    private Instant createdAt = Instant.now();
}
