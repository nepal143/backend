package com.placementgo.backend.referral.dto;

import lombok.Builder;
import lombok.Data;

import java.util.Map;
import java.util.UUID;

@Data
@Builder
public class CreateReferralResponse {
    private UUID referralId;
    private String shareLink;
    private String linkedinSearchLink;
    private Map<String, String> templates;
}
