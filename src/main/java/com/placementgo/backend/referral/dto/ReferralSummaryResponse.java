package com.placementgo.backend.referral.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class ReferralSummaryResponse {
    private UUID id;
    private String shareLink;
    private Instant createdAt;
}
