package com.placementgo.backend.jobs.entity;

import com.placementgo.backend.auth.model.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "job_applications")
@Getter
@Setter
public class JobApplication {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_posting_id", nullable = false)
    private JobPosting jobPosting;

    @Column(nullable = false)
    private String status; // e.g., "APPLIED", "REDIRECTED", "PENDING"

    @Column(nullable = false)
    private Instant appliedAt = Instant.now();
}
