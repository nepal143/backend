package Resume.controller;

import Resume.model.Resume;;
import Resume.service.ResumeService;

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
            @RequestHeader("X-User-Id") UUID userId,
            @RequestParam("file") MultipartFile file
    ) throws Exception {

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
}