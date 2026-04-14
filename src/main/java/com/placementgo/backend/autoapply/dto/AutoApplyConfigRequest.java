package com.placementgo.backend.autoapply.dto;

import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class AutoApplyConfigRequest {
    private List<String> targetJobTitles;
    private List<String> preferredLocations;
    private List<String> blacklistedCompanies;
    private String experienceLevel;
    private boolean autoApplyEnabled;
    private boolean emailApplyEnabled;
    private int maxApplicationsPerDay;
    private int minAiMatchScore;
    private UUID resumeId;
}
