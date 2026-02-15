package com.placementgo.backend.referral.service;

import com.placementgo.backend.referral.dto.CreateReferralRequest;
import com.placementgo.backend.referral.dto.CreateReferralResponse;
import com.placementgo.backend.referral.dto.ReferralSummaryResponse;
import com.placementgo.backend.referral.entity.ReferralRequest;
import com.placementgo.backend.referral.entity.ReferralTemplate;
import com.placementgo.backend.referral.enums.TemplateType;
import com.placementgo.backend.referral.repository.ReferralRequestRepository;
import com.placementgo.backend.referral.repository.ReferralTemplateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReferralService {

    private final ReferralRequestRepository requestRepo;
    private final ReferralTemplateRepository templateRepo;
    private final LinkedInLinkService linkedIn;

    public CreateReferralResponse createReferral(UUID userId, CreateReferralRequest req) {

        String token = UUID.randomUUID().toString().replace("-", "").substring(0, 12);

        ReferralRequest r = new ReferralRequest();
        r.setUserId(userId);
        r.setResumeId(req.getResumeId());
        r.setJobDescription(req.getJobDescription());
        r.setShareToken(token);
        requestRepo.save(r);

        templateRepo.save(createTemplate(r, TemplateType.REFERRAL, "Referral message"));
        templateRepo.save(createTemplate(r, TemplateType.CONNECTION, "Connection message"));

        return CreateReferralResponse.builder()
                .referralId(r.getId())
                .shareLink("https://placementgo.in/r/" + token)
                .linkedinSearchLink(linkedIn.generate(req.getJobDescription()))
                .templates(Map.of(
                        "REFERRAL", "Referral message",
                        "CONNECTION", "Connection message"
                ))
                .build();
    }

    private ReferralTemplate createTemplate(ReferralRequest r, TemplateType t, String msg) {
        ReferralTemplate rt = new ReferralTemplate();
        rt.setReferralRequest(r);
        rt.setType(t);
        rt.setMessage(msg);
        rt.setVersion(1);
        return rt;
    }

    public List<ReferralSummaryResponse> getAll(UUID userId) {
        return requestRepo.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(r -> ReferralSummaryResponse.builder()
                        .id(r.getId())
                        .shareLink("https://placementgo.in/r/" + r.getShareToken())
                        .createdAt(r.getCreatedAt())
                        .build())
                .toList();
    }

    public ReferralRequest getById(UUID id) {
        return requestRepo.findById(id).orElseThrow();
    }

    public ReferralRequest getByToken(String token) {
        return requestRepo.findByShareToken(token).orElseThrow();
    }
}
