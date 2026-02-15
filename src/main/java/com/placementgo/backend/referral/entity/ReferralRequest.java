package com.placementgo.backend.referral.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Data
public class ReferralRequest {

    @Id
    @GeneratedValue
    private UUID id;

    private UUID userId;

    private UUID resumeId;

    @Column(length = 5000)
    private String jobDescription;

    @Column(unique = true)
    private String shareToken;

    @CreationTimestamp
    private Instant createdAt;
}
