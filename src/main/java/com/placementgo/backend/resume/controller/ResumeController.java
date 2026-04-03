package com.placementgo.backend.resume.controller;

import com.placementgo.backend.resume.dto.GenerateResumeResponse;
import com.placementgo.backend.resume.dto.ResumeDetailResponse;
import com.placementgo.backend.resume.dto.ResumeSummaryResponse;
import com.placementgo.backend.resume.model.Resume;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import com.placementgo.backend.resume.service.ResumeService;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;
import org.springframework.security.core.userdetails.UserDetails;


@RestController
@RequestMapping("/api/resumes")
@RequiredArgsConstructor
public class ResumeController {

    private final ResumeService resumeService;

    @PostMapping(
            value = "/upload",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<GenerateResumeResponse> uploadAndGenerate(
            @RequestParam("file") MultipartFile file,
            @RequestParam("jobDescription") String jobDescription,
            @RequestParam(value = "template", defaultValue = "classic") String template,
            Authentication authentication
    ) throws Exception {

        UUID userId = UUID.fromString(((UserDetails) authentication.getPrincipal()).getUsername());

        GenerateResumeResponse response =
                resumeService.uploadAndGenerate(userId, file, jobDescription, template);

        return ResponseEntity.ok(response);
    }

    /**
     * List all generated resumes for the authenticated user.
     */
    @GetMapping
    public ResponseEntity<List<ResumeSummaryResponse>> getUserResumes(Authentication authentication) {
        UUID userId = UUID.fromString(((UserDetails) authentication.getPrincipal()).getUsername());
        return ResponseEntity.ok(resumeService.getUserResumes(userId));
    }

    /**
     * Get full detail (including pdfBase64) for a specific resume.
     * Only returns the resume if it belongs to the authenticated user.
     */
    @GetMapping("/{resumeId}")
    public ResponseEntity<ResumeDetailResponse> getResume(
            @PathVariable UUID resumeId,
            Authentication authentication
    ) {
        UUID userId = UUID.fromString(((UserDetails) authentication.getPrincipal()).getUsername());
        Resume resume = resumeService.getResumeById(resumeId);

        if (!resume.getUserId().equals(userId)) {
            return ResponseEntity.status(403).build();
        }

        return ResponseEntity.ok(new ResumeDetailResponse(resume));
    }

    /**
     * Delete a specific resume. Only allowed if it belongs to the authenticated user.
     */
    @DeleteMapping("/{resumeId}")
    public ResponseEntity<Void> deleteResume(
            @PathVariable UUID resumeId,
            Authentication authentication
    ) {
        UUID userId = UUID.fromString(((UserDetails) authentication.getPrincipal()).getUsername());
        resumeService.deleteResume(resumeId, userId);
        return ResponseEntity.noContent().build();
    }
}