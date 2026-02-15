package com.placementgo.backend.dashboard.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class CreateApplicationRequest {
    private String company;
    private String role;
    private String jobLink;
    private LocalDate appliedDate;
}
