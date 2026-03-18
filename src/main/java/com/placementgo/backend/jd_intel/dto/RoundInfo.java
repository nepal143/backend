package com.placementgo.backend.jd_intel.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoundInfo {
    private Integer step;
    private String title;
    private String duration;
    private String description;
}
