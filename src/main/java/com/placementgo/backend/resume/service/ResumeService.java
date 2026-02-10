package com.placementgo.backend.resume.service;

import com.placementgo.backend.resume.dto.GenerateResumeRequest;
import com.placementgo.backend.resume.dto.GenerateResumeResponse;
import com.placementgo.backend.resume.model.Resume;
import com.placementgo.backend.resume.model.ResumeStatus;
import com.placementgo.backend.resume.util.FileValidator;
import com.placementgo.backend.resume.repository.ResumeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ResumeService {

    private final ResumeRepository resumeRepository;
    private final ResumeParsingService parsingService;
    private final AiResumeGenerator aiResumeGenerator;

    public Resume uploadResume(UUID userId, MultipartFile file) throws Exception {

        FileValidator.validate(file);
        Path uploadDir = Path.of("uploads");
        Files.createDirectories(uploadDir);

        Path filePath = uploadDir.resolve(UUID.randomUUID() + "_" + file.getOriginalFilename());
        Files.write(filePath, file.getBytes());

        Resume resume = new Resume();
        resume.setUserId(userId);
        resume.setOriginalFileName(file.getOriginalFilename());
        resume.setStoredFilePath(filePath.toString());
        resume.setStatus(ResumeStatus.UPLOADED);
        resume.setCreatedAt(LocalDateTime.now());

        Resume savedResume = resumeRepository.save(resume);

        parsingService.parseAsync(savedResume.getId());

        return savedResume;
    }

    public Resume getResumeById(UUID resumeId) {
        return resumeRepository.findById(resumeId)
                .orElseThrow(() -> new RuntimeException("Resume not found"));
    }


    public GenerateResumeResponse generateResume(
            UUID userId,
            GenerateResumeRequest request
    ) {

        // 1️⃣ Validate input
        if (request.resumeId == null || request.jobDescription == null || request.jobDescription.isBlank()) {
            throw new RuntimeException("ResumeId and job description are required");
        }

        // 2️⃣ Fetch resume
        Resume resume = resumeRepository.findById(request.resumeId)
                .orElseThrow(() -> new RuntimeException("Resume not found"));

        // 3️⃣ Ownership check (VERY IMPORTANT)
        if (!resume.getUserId().equals(userId)) {
            throw new RuntimeException("Forbidden");
        }

        // 4️⃣ Resume must be parsed first
        if (resume.getStatus() != ResumeStatus.PARSED || resume.getParsedJson() == null) {
            throw new RuntimeException("Resume not parsed yet");
        }

        // 5️⃣ Call AI generator
        String generatedJson = aiResumeGenerator.generate(
                resume.getParsedJson(),
                request.jobDescription
        );

        // 6️⃣ Return response (no DB mutation yet)
        return new GenerateResumeResponse(generatedJson);
    }
}
