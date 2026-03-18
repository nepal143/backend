package com.placementgo.backend.referral.service;

import com.placementgo.backend.referral.dto.CreateReferralRequest;
import com.placementgo.backend.referral.dto.CreateReferralResponse;
import com.placementgo.backend.referral.entity.ReferralTemplate;
import com.placementgo.backend.referral.enums.TemplateType;
import com.placementgo.backend.referral.repository.ReferralTemplateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReferralService {

    private final ReferralTemplateRepository templateRepository;

    public CreateReferralResponse createReferral(UUID userId, CreateReferralRequest req) {
        String company = req.getCompany();
        String role = req.getRole();

        String linkedinSearchLink = "https://www.linkedin.com/search/results/people/?keywords="
                + URLEncoder.encode(role + " " + company, StandardCharsets.UTF_8);

        UUID referralId = UUID.randomUUID();
        String shareLink = "https://placementgo.com/r/" + referralId;

        Map<TemplateType, String> templateMessages = buildTemplates(company, role);

        List<ReferralTemplate> templateEntities = templateMessages.entrySet().stream()
                .map(entry -> ReferralTemplate.builder()
                        .referralId(referralId)
                        .type(entry.getKey())
                        .message(entry.getValue())
                        .version(1)
                        .build())
                .collect(Collectors.toList());

        templateRepository.saveAll(templateEntities);

        Map<String, String> templatesForResponse = templateMessages.entrySet().stream()
                .collect(Collectors.toMap(
                        e -> e.getKey().name(),
                        Map.Entry::getValue,
                        (a, b) -> a,
                        LinkedHashMap::new
                ));

        return CreateReferralResponse.builder()
                .referralId(referralId)
                .company(company)
                .role(role)
                .linkedinSearchLink(linkedinSearchLink)
                .shareLink(shareLink)
                .templates(templatesForResponse)
                .build();
    }

    public Object getAll(UUID userId) {
        return null;
    }

    public Object getById(UUID id) {
        return templateRepository.findByReferralId(id);
    }

    public Object getByToken(String token) {
        return null;
    }

    private Map<TemplateType, String> buildTemplates(String company, String role) {
        Map<TemplateType, String> map = new LinkedHashMap<>();

        map.put(TemplateType.FIRST_OUTREACH,
            "Hi [Name],\n\n" +
            "I came across your profile and noticed you work at " + company + " as a " + role + ". I'm really inspired by your journey!\n\n" +
            "I'm currently applying for the " + role + " position at " + company + " and would love to hear about your experience there — the culture, the team, and what it's like day-to-day.\n\n" +
            "Would you be open to a quick 10–15 min chat? It would mean a lot to me.\n\n" +
            "Best,\n[Your Name]"
        );

        map.put(TemplateType.REFERRAL_REQUEST,
            "Hi [Name],\n\n" +
            "Hope you're doing well! I'm reaching out because I'm very excited about the " + role + " opening at " + company + " and I believe I'd be a great fit.\n\n" +
            "I have [X years] of experience in [relevant skill/domain] and have worked on [brief achievement]. I'd be grateful if you could refer me internally or point me in the right direction.\n\n" +
            "I've attached my resume for your reference. Happy to share more details if needed!\n\n" +
            "Thank you so much for your time.\n\n" +
            "Best,\n[Your Name]"
        );

        map.put(TemplateType.FOLLOW_UP,
            "Hi [Name],\n\n" +
            "I wanted to follow up on my previous message — I know things get busy!\n\n" +
            "I've officially submitted my application for the " + role + " role at " + company + " and I'm still very excited about the opportunity. If you're open to providing a referral or sharing any tips, I'd truly appreciate it.\n\n" +
            "No worries at all if it's not possible — I just wanted to reach out one more time.\n\n" +
            "Thanks again!\n\n" +
            "Best,\n[Your Name]"
        );

        return map;
    }
}