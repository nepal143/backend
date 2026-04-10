package com.placementgo.backend.jobs.service;

import com.placementgo.backend.auth.model.User;
import com.placementgo.backend.auth.repository.UserRepository;
import com.placementgo.backend.jobs.dto.ApplyRequest;
import com.placementgo.backend.jobs.dto.ApplyResponse;
import com.placementgo.backend.jobs.dto.JobDto;
import com.placementgo.backend.jobs.entity.JobApplication;
import com.placementgo.backend.jobs.entity.JobPosting;
import com.placementgo.backend.jobs.repository.JobApplicationRepository;
import com.placementgo.backend.jobs.repository.JobPostingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class JobApplicationService {

    private final JobApplicationRepository jobApplicationRepository;
    private final JobPostingRepository jobPostingRepository;
    private final UserRepository userRepository;

    @Transactional
    public ApplyResponse apply(User user, ApplyRequest request) {
        JobPosting jobPosting = null;

        // Check if job exists in our database
        if (request.getJobId() != null) {
            jobPosting = jobPostingRepository.findById(request.getJobId()).orElse(null);
        }

        // If it doesn't exist but we have external details, save it as a cached job posting
        if (jobPosting == null && request.getExternalJobDetails() != null) {
            JobDto externalDto = request.getExternalJobDetails();
            
            // Prevent duplicates from external sources
            Optional<JobPosting> existingExt = jobPostingRepository.findByPlatformJobIdAndJobPlatformSource(
                    externalDto.getPlatformJobId(), externalDto.getJobPlatformSource());
            
            if (existingExt.isPresent()) {
                jobPosting = existingExt.get();
            } else {
                jobPosting = new JobPosting();
                jobPosting.setTitle(externalDto.getTitle());
                jobPosting.setCompanyName(externalDto.getCompanyName());
                jobPosting.setLocation(externalDto.getLocation());
                jobPosting.setDescriptionSnippet(externalDto.getDescriptionSnippet());
                jobPosting.setApplyUrl(externalDto.getApplyUrl());
                jobPosting.setJobPlatformSource(externalDto.getJobPlatformSource());
                jobPosting.setPlatformJobId(externalDto.getPlatformJobId());
                jobPosting.setInternal(false);
                jobPosting = jobPostingRepository.save(jobPosting);
            }
        }

        if (jobPosting == null) {
            return ApplyResponse.builder()
                    .success(false)
                    .message("Job not found and no external details provided.")
                    .build();
        }

        // Check if already applied
        if (jobApplicationRepository.existsByUser_IdAndJobPosting_Id(user.getId(), jobPosting.getId())) {
            return ApplyResponse.builder()
                    .success(false)
                    .message("You have already applied to this job.")
                    .build();
        }

        JobApplication application = new JobApplication();
        application.setUser(user);
        application.setJobPosting(jobPosting);
        
        if (jobPosting.isInternal()) {
            application.setStatus("APPLIED");
            jobApplicationRepository.save(application);
            return ApplyResponse.builder()
                    .success(true)
                    .message("Successfully applied to internal job.")
                    .status("APPLIED")
                    .build();
        } else {
            application.setStatus("REDIRECTED");
            jobApplicationRepository.save(application);
            return ApplyResponse.builder()
                    .success(true)
                    .message("Redirecting to external platform. Chrome extension will assist with Auto-Apply.")
                    .status("REDIRECTED")
                    .redirectUrl(jobPosting.getApplyUrl())
                    .requiresExtension(true) // Indicate to frontend that we should trigger Chrome Extension
                    .build();
        }
    }
}
