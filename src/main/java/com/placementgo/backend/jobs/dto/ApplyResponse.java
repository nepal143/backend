package com.placementgo.backend.jobs.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApplyResponse {
    private boolean success;
    private String message;
    private String status; // APPLIED, REDIRECTED
    private String redirectUrl; // If it's an external job that we need the extension/user to open
    private boolean requiresExtension; // True if Option B indicates we need the extension to auto-fill
}
