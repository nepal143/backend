package com.placementgo.backend.referral.controller;

import com.placementgo.backend.referral.service.ReferralService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/referrals/{id}/templates")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class ReferralTemplateController {

    private final ReferralService service;

    @GetMapping
    public Object getTemplates(@PathVariable UUID id) {
        return service.getById(id);  // ✅ ReferralService ka getById use karo
    }
}