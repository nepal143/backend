package com.placementgo.backend.resume.controller;

import com.placementgo.backend.resume.dto.GenerateResumeRequest;
import com.placementgo.backend.resume.dto.GenerateResumeResponse;
import com.placementgo.backend.resume.model.Resume;
import org.springframework.security.core.Authentication;
import com.placementgo.backend.resume.service.ResumeService;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;


@RestController
@RequestMapping("/api/resumes")
@RequiredArgsConstructor
public class ResumeController {

    private final ResumeService resumeService;

    /**
     * Upload resume (PDF / DOCX)
     * User ID comes from API Gateway after JWT validation
     */
    @PostMapping("/upload")
    public ResponseEntity<Resume> uploadResume(
            @RequestParam("file") MultipartFile file,
            Authentication authentication
    ) throws Exception {

        UUID userId = (UUID) authentication.getPrincipal();

        Resume resume = resumeService.uploadResume(userId, file);
        return ResponseEntity.ok(resume);
    }

    /**
     * Get resume by ID (status, metadata)
     */
    @GetMapping("/{resumeId}")
    public ResponseEntity<Resume> getResume(
            @PathVariable UUID resumeId
    ) {
        Resume resume = resumeService.getResumeById(resumeId);
        return ResponseEntity.ok(resume);
    }

    @PostMapping("/generate")
    public ResponseEntity<GenerateResumeResponse> generateResume(
            @RequestBody GenerateResumeRequest request,
            Authentication authentication
    ) {
        UUID userId = (UUID) authentication.getPrincipal();
        return ResponseEntity.ok(
                resumeService.generateResume(userId, request)
        );
    }
}