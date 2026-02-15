package com.placementgo.backend.referral.dto;

import com.placementgo.backend.referral.enums.TemplateType;
import lombok.Data;

@Data
public class RegenerateTemplateRequest {
    private TemplateType type;
}
