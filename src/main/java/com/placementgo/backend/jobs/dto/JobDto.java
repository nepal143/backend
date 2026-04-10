package com.placementgo.backend.jobs.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobDto {
    private UUID id;
    private String title;
    private String companyName;
    private String location;
    private String descriptionSnippet;
    private String applyUrl;
    private String jobPlatformSource;
    private String platformJobId;
    private boolean isInternal;
    private String postedAt;
}
