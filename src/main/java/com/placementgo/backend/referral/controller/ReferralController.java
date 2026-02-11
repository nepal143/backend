package com.placementgo.backend.referral.controller;

import com.placementgo.backend.referral.dto.CreateReferralRequest;
import com.placementgo.backend.referral.service.ReferralService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/referrals")
@RequiredArgsConstructor
public class ReferralController {

    private final ReferralService referralService;

    @PostMapping
    public Object create(@RequestHeader UUID userId,
                         @RequestBody CreateReferralRequest req) {
        return referralService.createReferral(userId, req);
    }

    @GetMapping
    public Object list(@RequestHeader UUID userId) {
        return referralService.getAll(userId);
    }

    @GetMapping("/{id}")
    public Object get(@PathVariable UUID id) {
        return referralService.getById(id);
    }
}
