package com.placementgo.backend.referral.controller;

import com.placementgo.backend.referral.service.ReferralTemplateService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/referrals/{id}/templates")
@RequiredArgsConstructor
public class ReferralTemplateController {

    private final ReferralTemplateService service;

    @GetMapping
    public Object getTemplates(@PathVariable UUID id) {
        return service.getTemplates(id);
    }
}
