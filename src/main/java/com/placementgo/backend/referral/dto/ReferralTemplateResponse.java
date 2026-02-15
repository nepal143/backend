package com.placementgo.backend.referral.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ReferralTemplateResponse {
    private String type;
    private String message;
    private int version;
}
