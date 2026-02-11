package com.placementgo.backend.referral.dto;

import lombok.Data;

import java.util.UUID;

@Data
public class CreateReferralRequest {
    private UUID resumeId;
    private String jobDescription;
}
