package com.placementgo.backend.referral.controller;

import com.placementgo.backend.referral.service.ReferralService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/r")
@RequiredArgsConstructor
public class PublicReferralController {

    private final ReferralService referralService;

    @GetMapping("/{token}")
    public Object resolve(@PathVariable String token) {
        return referralService.getByToken(token);
    }
}
