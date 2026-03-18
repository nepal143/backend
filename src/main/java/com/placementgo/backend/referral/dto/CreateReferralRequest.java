package com.placementgo.backend.referral.dto;

import lombok.Data;

@Data  // ✅ yeh hona chahiye — getCompany(), getRole() yahi banata hai
public class CreateReferralRequest {
    private String company;
    private String role;
}