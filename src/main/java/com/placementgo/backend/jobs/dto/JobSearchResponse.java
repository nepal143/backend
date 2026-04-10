package com.placementgo.backend.jobs.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobSearchResponse {
    private String query;
    private int totalResults;
    private List<JobDto> jobs;
}
