package com.placementgo.backend.jobs.controller;

import com.placementgo.backend.auth.model.User;
import com.placementgo.backend.auth.repository.UserRepository;
import com.placementgo.backend.jobs.dto.ApplyRequest;
import com.placementgo.backend.jobs.dto.ApplyResponse;
import com.placementgo.backend.jobs.dto.JobSearchResponse;
import com.placementgo.backend.jobs.service.JobApplicationService;
import com.placementgo.backend.jobs.service.JobSearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/premium-jobs")
@RequiredArgsConstructor
@Tag(name = "Premium Jobs", description = "Endpoints for searching and Auto-Applying to jobs from multiple sources")
public class PremiumJobsController {

    private final JobSearchService jobSearchService;
    private final JobApplicationService jobApplicationService;
    private final UserRepository userRepository;

    @GetMapping("/search")
    @Operation(summary = "Search for premium jobs from sources like LinkedIn, Indeed, etc.")
    public ResponseEntity<JobSearchResponse> searchJobs(@RequestParam("query") String query) {
        JobSearchResponse response = jobSearchService.searchPremiumJobs(query);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/apply")
    @Operation(summary = "Auto-Apply to a job or get redirect URL with extension payload")
    public ResponseEntity<ApplyResponse> applyToJob(@RequestBody ApplyRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return ResponseEntity.status(401).body(ApplyResponse.builder()
                    .success(false)
                    .message("User not authenticated.")
                    .build());
        }

        String email = auth.getName();
        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            return ResponseEntity.status(404).body(ApplyResponse.builder()
                    .success(false)
                    .message("User record not found.")
                    .build());
        }

        ApplyResponse response = jobApplicationService.apply(user, request);
        return ResponseEntity.ok(response);
    }
}
