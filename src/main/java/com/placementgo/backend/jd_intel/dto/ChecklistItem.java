package com.placementgo.backend.jd_intel.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChecklistItem {
    private String title;
    private String description;
    private Boolean completed;
    private Integer priority;
}
