package com.placementgo.backend.jobs.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApplyRequest {
    private UUID jobId; // If the job exists in our database
    private JobDto externalJobDetails; // To sync external job before applying, if not in DB
}
