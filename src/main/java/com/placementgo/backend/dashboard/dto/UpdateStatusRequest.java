package com.placementgo.backend.dashboard.dto;

import com.placementgo.backend.dashboard.entity.ApplicationStatus;

import lombok.Data;

@Data
public class UpdateStatusRequest {
    private ApplicationStatus status;
}
