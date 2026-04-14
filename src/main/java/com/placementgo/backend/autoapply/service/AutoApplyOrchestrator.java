package com.placementgo.backend.autoapply.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.placementgo.backend.autoapply.dto.JobLeadDto;
import com.placementgo.backend.autoapply.entity.AutoApplyConfig;
import com.placementgo.backend.autoapply.entity.AutoApplyJobLead;
import com.placementgo.backend.autoapply.enums.ApplyMethod;
import com.placementgo.backend.autoapply.enums.LeadStatus;
import com.placementgo.backend.autoapply.repository.AutoApplyConfigRepository;
import com.placementgo.backend.autoapply.repository.AutoApplyJobLeadRepository;
import com.placementgo.backend.dashboard.entity.Application;
import com.placementgo.backend.dashboard.entity.ApplicationStatus;
import com.placementgo.backend.dashboard.repository.ApplicationRepository;
import com.placementgo.backend.resume.model.Resume;
import com.placementgo.backend.resume.repository.ResumeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Central coordinator for the auto-apply pipeline.
 *
 * Scheduled pipeline (every 6 hours by default, configurable):
 *  1. Load all users who have autoApplyEnabled = true
 *  2. For each user → fetch their resume → discover jobs
 *  3. AI-score each job vs resume
 *  4. Skip duplicates, skip score-below-threshold, skip blacklisted companies
 *  5. For EMAIL leads → auto-send if emailApplyEnabled; else mark MANUAL_REQUIRED
 *  6. For other leads → mark MANUAL_REQUIRED, generate template
 *  7. Sync applied leads to application tracker
 *  8. Push notifications
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AutoApplyOrchestrator {

    private final AutoApplyConfigRepository configRepo;
    private final AutoApplyJobLeadRepository leadRepo;
    private final ResumeRepository resumeRepo;
    private final ApplicationRepository applicationRepository;

    private final JobDiscoveryService discoveryService;
    private final JobMatchingService matchingService;
    private final ApplicationTemplateService templateService;
    private final AutoApplyEmailService emailService;
    private final NotificationService notificationService;

    private final ObjectMapper objectMapper;

    // ── Scheduled run ─────────────────────────────────────────────────────────

    @Scheduled(cron = "${autoapply.schedule.cron:0 0 */6 * * *}")
    public void runScheduledPipeline() {
        log.info("=== AutoApply scheduled pipeline starting ===");
        List<AutoApplyConfig> activeConfigs = configRepo.findAll().stream()
                .filter(AutoApplyConfig::isAutoApplyEnabled)
                .toList();

        log.info("Found {} users with auto-apply enabled", activeConfigs.size());
        for (AutoApplyConfig config : activeConfigs) {
            try {
                runForUser(config);
            } catch (Exception e) {
                log.error("Pipeline failed for user {}: {}", config.getUserId(), e.getMessage(), e);
            }
        }
        log.info("=== AutoApply scheduled pipeline complete ===");
    }

    /**
     * Triggered manually via the REST API (user clicks "Scan Now").
     */
    @Transactional
    public Map<String, Object> runManualScan(UUID userId) {
        AutoApplyConfig config = configRepo.findByUserId(userId)
                .orElseThrow(() -> new IllegalStateException("Auto-apply not configured for this user"));

        int[] results = runForUser(config);
        return Map.of(
                "discovered", results[0],
                "autoApplied", results[1],
                "manualRequired", results[2]
        );
    }

    // ── Core pipeline ─────────────────────────────────────────────────────────

    /** Returns [discovered, autoApplied, manualRequired] counts */
    private int[] runForUser(AutoApplyConfig config) {
        UUID userId = config.getUserId();
        int discovered = 0, autoApplied = 0, manualRequired = 0;

        // Determine daily quota remaining
        long appliedToday = leadRepo.countAppliedSince(userId, LocalDateTime.now().minusHours(24));
        int remaining = config.getMaxApplicationsPerDay() - (int) appliedToday;
        if (remaining <= 0) {
            log.info("User {} has hit daily application cap", userId);
            return new int[]{0, 0, 0};
        }

        // Load resume
        Optional<Resume> resumeOpt = config.getResumeId() != null
                ? resumeRepo.findById(config.getResumeId())
                : resumeRepo.findTopByUserIdOrderByCreatedAtDesc(userId);

        String parsedResumeJson = resumeOpt.map(Resume::getParsedJson).orElse(null);
        String pdfBase64 = resumeOpt.map(Resume::getGeneratedPdfBase64).orElse(null);
        String resumeFileName = resumeOpt.map(r -> sanitizeFilename(r.getOriginalFileName())).orElse("resume.pdf");

        // Discover jobs for each target title × location combination
        for (String title : config.getTargetJobTitles()) {
            String location = config.getPreferredLocations().isEmpty()
                    ? "Remote"
                    : config.getPreferredLocations().get(0);

            List<JobDiscoveryService.RawJobLead> rawLeads = discoveryService.discover(title, location);
            discovered += rawLeads.size();

            for (JobDiscoveryService.RawJobLead raw : rawLeads) {
                if (remaining <= 0) break;

                // Skip duplicates at DB level
                if (leadRepo.existsByUserIdAndExternalJobIdAndSource(userId, raw.externalId(), raw.source())) {
                    continue;
                }

                // Skip blacklisted companies
                if (isBlacklisted(raw.company(), config.getBlacklistedCompanies())) {
                    continue;
                }

                // AI scoring — if no parsed resume, skip threshold check and show all jobs
                int score = 50;
                List<String> reasons = List.of();
                if (parsedResumeJson != null) {
                    JobMatchingService.MatchResult match =
                            matchingService.score(parsedResumeJson, raw.title(), raw.description());
                    score = match.score();
                    reasons = match.reasons();

                    if (score < config.getMinAiMatchScore()) {
                        log.debug("Skipping '{}' at '{}' – score {} below threshold {}", raw.title(), raw.company(), score, config.getMinAiMatchScore());
                        continue;
                    }
                } else {
                    log.debug("No parsed resume for user {}, skipping AI threshold filter", userId);
                }

                // Generate application template
                String template = templateService.generate(parsedResumeJson, raw.title(), raw.company(), raw.description());

                // Determine apply strategy
                LeadStatus status;
                if (raw.applyMethod() == ApplyMethod.EMAIL
                        && config.isEmailApplyEnabled()
                        && raw.applyEmail() != null) {

                    // ── Auto email apply ──────────────────────────────────────
                    String candidateName = extractCandidateName(template);
                    boolean sent = emailService.sendApplication(
                            raw.applyEmail(), template, candidateName, pdfBase64, resumeFileName);

                    status = sent ? LeadStatus.EMAIL_SENT : LeadStatus.MANUAL_REQUIRED;
                    if (sent) {
                        autoApplied++;
                        remaining--;
                        notificationService.push(userId, "EMAIL_SENT",
                                "Application submitted to " + raw.company(),
                                "Your application for " + raw.title() + " was emailed to " + raw.company() + ".",
                                Map.of("company", raw.company(), "role", raw.title()));
                    }
                } else {
                    // ── Manual required – send notification with template ──────
                    status = LeadStatus.MANUAL_REQUIRED;
                    manualRequired++;
                }

                // Persist lead
                AutoApplyJobLead lead = AutoApplyJobLead.builder()
                        .userId(userId)
                        .jobTitle(raw.title())
                        .company(raw.company())
                        .location(raw.location())
                        .jobDescription(raw.description())
                        .applyUrl(raw.applyUrl())
                        .applyEmail(raw.applyEmail())
                        .applyMethod(raw.applyMethod())
                        .status(status)
                        .aiMatchScore(score)
                        .matchReasons(toJson(reasons))
                        .applicationTemplate(template)
                        .source(raw.source())
                        .externalJobId(raw.externalId())
                        .appliedAt(
                                (status == LeadStatus.EMAIL_SENT) ? LocalDateTime.now() : null
                        )
                        .build();

                leadRepo.save(lead);

                // Notify for manual-required jobs
                if (status == LeadStatus.MANUAL_REQUIRED) {
                    notificationService.push(userId, "MANUAL_REQUIRED",
                            "New job match: " + raw.title() + " at " + raw.company(),
                            "Score: " + score + "/100. Template ready – one click to apply!",
                            Map.of("jobLeadId", lead.getId(), "company", raw.company(),
                                    "role", raw.title(), "score", score));
                }

                // Sync to dashboard tracker if applied
                if (status == LeadStatus.EMAIL_SENT) {
                    syncToDashboard(userId, raw.company(), raw.title(), raw.applyUrl());
                }
            }
        }

        if (discovered > 0) {
            notificationService.push(userId, "JOB_FOUND",
                    "Job scan complete",
                    "Found " + discovered + " job leads. " + autoApplied + " auto-applied, "
                            + manualRequired + " need your attention.",
                    Map.of("total", discovered, "autoApplied", autoApplied, "manualRequired", manualRequired));
        }

        return new int[]{discovered, autoApplied, manualRequired};
    }

    // ── Manual lead actions ───────────────────────────────────────────────────

    /** User marks a lead as manually applied */
    @Transactional
    public com.placementgo.backend.autoapply.dto.JobLeadDto markManuallyApplied(UUID userId, UUID leadId) {
        AutoApplyJobLead lead = leadRepo.findByIdAndUserId(leadId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Lead not found"));
        lead.setStatus(LeadStatus.MANUALLY_APPLIED);
        lead.setAppliedAt(LocalDateTime.now());
        leadRepo.save(lead);

        syncToDashboard(userId, lead.getCompany(), lead.getJobTitle(), lead.getApplyUrl());

        notificationService.push(userId, "AUTO_APPLIED",
                "Application tracked: " + lead.getJobTitle(),
                "Marked as applied to " + lead.getCompany() + ". Added to your tracker.",
                Map.of("jobLeadId", leadId));

        return toDto(lead);
    }

    /** User skips a lead */
    @Transactional
    public void skipLead(UUID userId, UUID leadId) {
        AutoApplyJobLead lead = leadRepo.findByIdAndUserId(leadId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Lead not found"));
        lead.setStatus(LeadStatus.SKIPPED);
        leadRepo.save(lead);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void syncToDashboard(UUID userId, String company, String role, String jobLink) {
        try {
            if (!applicationRepository.existsByUserIdAndJobLinkAndRole(userId, jobLink, role)) {
                applicationRepository.save(Application.builder()
                        .userId(userId)
                        .company(company)
                        .role(role)
                        .jobLink(jobLink)
                        .appliedDate(LocalDate.now())
                        .status(ApplicationStatus.APPLIED)
                        .build());
            }
        } catch (Exception e) {
            log.warn("Could not sync to dashboard tracker: {}", e.getMessage());
        }
    }

    private boolean isBlacklisted(String company, List<String> blacklist) {
        if (company == null || blacklist == null || blacklist.isEmpty()) return false;
        String lc = company.toLowerCase();
        return blacklist.stream().anyMatch(b -> lc.contains(b.toLowerCase()));
    }

    private String extractCandidateName(String templateJson) {
        try {
            return objectMapper.readTree(templateJson)
                    .path("fields").path("fullName").asText("Applicant");
        } catch (Exception e) {
            return "Applicant";
        }
    }

    private String sanitizeFilename(String name) {
        if (name == null || name.isBlank()) return "resume.pdf";
        String base = name.contains(".") ? name.substring(0, name.lastIndexOf('.')) : name;
        return base.replaceAll("[^a-zA-Z0-9_\\-]", "_") + ".pdf";
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return "[]";
        }
    }

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
