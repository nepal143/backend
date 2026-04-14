package com.placementgo.backend.autoapply.dto;

import com.placementgo.backend.autoapply.enums.ApplyMethod;
import com.placementgo.backend.autoapply.enums.LeadStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class JobLeadDto {
    private UUID id;
    private String jobTitle;
    private String company;
    private String location;
    private String jobDescription;
    private String applyUrl;
    private String applyEmail;
    private ApplyMethod applyMethod;
    private LeadStatus status;
    private int aiMatchScore;
    private String matchReasons;       // JSON string – parse on frontend
    private String applicationTemplate; // JSON string with pre-filled fields
    private String source;
    private LocalDateTime discoveredAt;
    private LocalDateTime appliedAt;
}
