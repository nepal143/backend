package com.placementgo.backend.referral.service;

import com.placementgo.backend.referral.dto.ReferralTemplateResponse;
import com.placementgo.backend.referral.repository.ReferralTemplateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReferralTemplateService {

    private final ReferralTemplateRepository repo;

    public List<ReferralTemplateResponse> getTemplates(UUID referralId) {
        return repo.findByReferralRequestId(referralId)
                .stream()
                .map(t -> ReferralTemplateResponse.builder()
                        .type(t.getType().name())
                        .message(t.getMessage())
                        .version(t.getVersion())
                        .build())
                .toList();
    }
}
