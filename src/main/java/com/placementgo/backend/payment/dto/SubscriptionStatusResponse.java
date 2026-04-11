package com.placementgo.backend.payment.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class SubscriptionStatusResponse {
    private boolean isPremium;
    private String plan;
    private String status;
    private Instant activatedAt;
    private Instant expiresAt;
}
