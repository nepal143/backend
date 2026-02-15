package com.placementgo.backend.dashboard.dto;

import com.placementgo.backend.dashboard.entity.ApplicationStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class ApplicationResponse {

    private UUID id;
    private String company;
    private String role;
    private String jobLink;
    private LocalDate appliedDate;
    private ApplicationStatus status;
    private LocalDateTime createdAt;
}
