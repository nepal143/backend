package com.placementgo.backend.autoapply.controller;

import com.placementgo.backend.autoapply.dto.AutoApplyConfigRequest;
import com.placementgo.backend.autoapply.dto.JobLeadDto;
import com.placementgo.backend.autoapply.entity.AutoApplyConfig;
import com.placementgo.backend.autoapply.entity.AutoApplyJobLead;
import com.placementgo.backend.autoapply.enums.LeadStatus;
import com.placementgo.backend.autoapply.repository.AutoApplyConfigRepository;
import com.placementgo.backend.autoapply.repository.AutoApplyJobLeadRepository;
import com.placementgo.backend.autoapply.service.ApplicationTemplateService;
import com.placementgo.backend.autoapply.service.AutoApplyOrchestrator;
import com.placementgo.backend.resume.repository.ResumeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/autoapply")
@RequiredArgsConstructor
public class AutoApplyController {

    private final AutoApplyConfigRepository configRepo;
    private final AutoApplyJobLeadRepository leadRepo;
    private final AutoApplyOrchestrator orchestrator;
    private final ApplicationTemplateService templateService;
    private final ResumeRepository resumeRepo;

    // ── Config ────────────────────────────────────────────────────────────────

    @GetMapping("/config")
    public ResponseEntity<AutoApplyConfig> getConfig(@AuthenticationPrincipal UUID userId) {
        return configRepo.findByUserId(userId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/config")
    public ResponseEntity<AutoApplyConfig> saveConfig(@RequestBody AutoApplyConfigRequest req,
                                                       @AuthenticationPrincipal UUID userId) {
        AutoApplyConfig config = configRepo.findByUserId(userId)
                .orElse(AutoApplyConfig.builder().userId(userId).build());

        if (req.getTargetJobTitles() != null)     config.setTargetJobTitles(req.getTargetJobTitles());
        if (req.getPreferredLocations() != null)  config.setPreferredLocations(req.getPreferredLocations());
        if (req.getBlacklistedCompanies() != null) config.setBlacklistedCompanies(req.getBlacklistedCompanies());
        if (req.getExperienceLevel() != null)     config.setExperienceLevel(req.getExperienceLevel());
        config.setAutoApplyEnabled(req.isAutoApplyEnabled());
        config.setEmailApplyEnabled(req.isEmailApplyEnabled());
        if (req.getMaxApplicationsPerDay() > 0)   config.setMaxApplicationsPerDay(req.getMaxApplicationsPerDay());
        if (req.getMinAiMatchScore() > 0)          config.setMinAiMatchScore(req.getMinAiMatchScore());
        if (req.getResumeId() != null)             config.setResumeId(req.getResumeId());

        return ResponseEntity.ok(configRepo.save(config));
    }

    // ── Scan ─────────────────────────────────────────────────────────────────

    /** Trigger an on-demand job discovery + apply scan for the current user */
    @PostMapping("/scan")
    public ResponseEntity<Map<String, Object>> scan(@AuthenticationPrincipal UUID userId) {
        Map<String, Object> result = orchestrator.runManualScan(userId);
        return ResponseEntity.ok(result);
    }

    // ── Leads ─────────────────────────────────────────────────────────────────

    @GetMapping("/leads")
    public ResponseEntity<Page<JobLeadDto>> getLeads(@AuthenticationPrincipal UUID userId,
                                                      @RequestParam(defaultValue = "0") int page,
                                                      @RequestParam(defaultValue = "20") int size,
                                                      @RequestParam(required = false) String status) {
        // If filtering by status
        if (status != null) {
            try {
                LeadStatus ls = LeadStatus.valueOf(status.toUpperCase());
                Page<JobLeadDto> filtered = leadRepo
                        .findByUserIdOrderByDiscoveredAtDesc(userId, PageRequest.of(page, size))
                        .map(this::toDto);
                return ResponseEntity.ok(filtered);
            } catch (IllegalArgumentException ignored) {}
        }
        Page<JobLeadDto> leads = leadRepo
                .findByUserIdOrderByDiscoveredAtDesc(userId, PageRequest.of(page, size))
                .map(this::toDto);
        return ResponseEntity.ok(leads);
    }

    @GetMapping("/leads/{leadId}")
    public ResponseEntity<JobLeadDto> getLead(@PathVariable UUID leadId,
                                               @AuthenticationPrincipal UUID userId) {
        return leadRepo.findByIdAndUserId(leadId, userId)
                .map(l -> ResponseEntity.ok(toDto(l)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/leads/{leadId}/apply")
    public ResponseEntity<JobLeadDto> markApplied(@PathVariable UUID leadId,
                                                    @AuthenticationPrincipal UUID userId) {
        return ResponseEntity.ok(orchestrator.markManuallyApplied(userId, leadId));
    }

    @PostMapping("/leads/{leadId}/skip")
    public ResponseEntity<Void> skip(@PathVariable UUID leadId,
                                      @AuthenticationPrincipal UUID userId) {
        orchestrator.skipLead(userId, leadId);
        return ResponseEntity.noContent().build();
    }

    /** Re-generate the application template using the user's latest resume */
    @PostMapping("/leads/{leadId}/regenerate-template")
    public ResponseEntity<JobLeadDto> regenerateTemplate(@PathVariable UUID leadId,
                                                          @AuthenticationPrincipal UUID userId) {
        AutoApplyJobLead lead = leadRepo.findByIdAndUserId(leadId, userId)
                .orElse(null);
        if (lead == null) return ResponseEntity.notFound().build();

        String parsedResumeJson = resumeRepo.findTopByUserIdOrderByCreatedAtDesc(userId)
                .map(r -> r.getParsedJson())
                .orElse(null);

        String newTemplate = templateService.generate(
                parsedResumeJson, lead.getJobTitle(), lead.getCompany(), lead.getJobDescription());
        lead.setApplicationTemplate(newTemplate);
        leadRepo.save(lead);
        return ResponseEntity.ok(toDto(lead));
    }

    /** Stats summary */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Long>> stats(@AuthenticationPrincipal UUID userId) {
        return ResponseEntity.ok(Map.of(
                "pending",        leadRepo.countByUserIdAndStatus(userId, LeadStatus.PENDING_REVIEW),
                "autoApplied",    leadRepo.countByUserIdAndStatus(userId, LeadStatus.EMAIL_SENT)
                                + leadRepo.countByUserIdAndStatus(userId, LeadStatus.AUTO_APPLIED),
                "manualRequired", leadRepo.countByUserIdAndStatus(userId, LeadStatus.MANUAL_REQUIRED),
                "manuallyApplied", leadRepo.countByUserIdAndStatus(userId, LeadStatus.MANUALLY_APPLIED),
                "skipped",        leadRepo.countByUserIdAndStatus(userId, LeadStatus.SKIPPED)
        ));
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private JobLeadDto toDto(AutoApplyJobLead l) {
        return JobLeadDto.builder()
                .id(l.getId())
                .jobTitle(l.getJobTitle())
                .company(l.getCompany())
                .location(l.getLocation())
                .jobDescription(l.getJobDescription())
                .applyUrl(l.getApplyUrl())
                .applyEmail(l.getApplyEmail())
                .applyMethod(l.getApplyMethod())
                .status(l.getStatus())
                .aiMatchScore(l.getAiMatchScore())
                .matchReasons(l.getMatchReasons())
                .applicationTemplate(l.getApplicationTemplate())
                .source(l.getSource())
                .discoveredAt(l.getDiscoveredAt())
                .appliedAt(l.getAppliedAt())
                .build();
    }
}
