package com.placementgo.backend.resume.service;

import com.placementgo.backend.resume.dto.GenerateResumeResponse;
import com.placementgo.backend.resume.model.Resume;
import com.placementgo.backend.resume.model.ResumeStatus;
import com.placementgo.backend.resume.repository.ResumeRepository;
import com.placementgo.backend.resume.util.FileValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Comparator;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ResumeService {

    private final ResumeRepository resumeRepository;
    private final ResumeParsingService parsingService;
    private final AiResumeGenerator aiResumeGenerator;

    public GenerateResumeResponse uploadAndGenerate(
            UUID userId,
            MultipartFile file,
            String jobDescription
    ) throws Exception {

        log.info("🚀 Starting resume upload + generation pipeline for user: {}", userId);

        FileValidator.validate(file);

        if (jobDescription == null || jobDescription.isBlank()) {
            throw new RuntimeException("Job description is required");
        }

        // ✅ Save only metadata (NO file storage)
        Resume resume = new Resume();
        resume.setUserId(userId);
        resume.setOriginalFileName(file.getOriginalFilename());
        resume.setStatus(ResumeStatus.UPLOADED);
        resume.setCreatedAt(LocalDateTime.now());

        resumeRepository.save(resume);

        log.info("📄 Processing resume in-memory (no disk storage).");

        // ✅ Parse directly from memory
        String parsedJson;
        try (InputStream inputStream = file.getInputStream()) {
            parsedJson = parsingService.parse(inputStream);
        }

        log.info("✅ Resume parsed successfully.");

        int maxRetries = 2;
        String latexContent = null;
        String base64Pdf = null;

        for (int attempt = 1; attempt <= maxRetries; attempt++) {

            log.info("🔁 AI generation attempt {}/{}", attempt, maxRetries);

            Path runtimeDir = null;

            try {

                // 1️⃣ Generate LaTeX
                latexContent = aiResumeGenerator.generateOptimizedJson(
                        parsedJson,
                        jobDescription
                );

                log.info("🤖 AI returned LaTeX (length: {})", latexContent.length());

                // 2️⃣ Create temp runtime directory
                runtimeDir = Files.createTempDirectory("latex-runtime-");

                // Write main.tex
                Path texFile = runtimeDir.resolve("main.tex");
                Files.writeString(texFile, latexContent, StandardCharsets.UTF_8);

                // Copy required class file
                ClassPathResource classResource =
                        new ClassPathResource("tempDir/tccv.cls");

                try (InputStream is = classResource.getInputStream()) {
                    Files.copy(
                            is,
                            runtimeDir.resolve("tccv.cls"),
                            StandardCopyOption.REPLACE_EXISTING
                    );
                }

                // 3️⃣ Compile using pdflatex
                ProcessBuilder processBuilder = new ProcessBuilder(
                        "pdflatex",
                        "-interaction=nonstopmode",
                        "-halt-on-error",
                        "main.tex"
                );

                processBuilder.directory(runtimeDir.toFile());
                processBuilder.redirectErrorStream(true);

                Process process = processBuilder.start();

                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(process.getInputStream())
                )) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        log.info("📄 pdflatex: {}", line);
                    }
                }

                int exitCode = process.waitFor();

                if (exitCode != 0) {
                    log.warn("❌ Compilation failed on attempt {}.", attempt);
                    continue;
                }

                // 4️⃣ Read generated PDF
                Path pdfPath = runtimeDir.resolve("main.pdf");

                if (!Files.exists(pdfPath)) {
                    log.warn("❌ PDF not generated on attempt {}.", attempt);
                    continue;
                }

                byte[] pdfBytes = Files.readAllBytes(pdfPath);
                base64Pdf = Base64.getEncoder().encodeToString(pdfBytes);

                log.info("✅ Compilation successful on attempt {}.", attempt);

                break; // SUCCESS

            } catch (Exception e) {
                log.warn("⚠️ Attempt {} failed: {}", attempt, e.getMessage());
            } finally {
                // ✅ Cleanup temp directory
                if (runtimeDir != null && Files.exists(runtimeDir)) {
                    Files.walk(runtimeDir)
                            .sorted(Comparator.reverseOrder())
                            .forEach(path -> {
                                try {
                                    Files.delete(path);
                                } catch (Exception ignored) {
                                }
                            });
                }
            }
        }

        if (base64Pdf == null) {
            log.error("❌ All {} attempts failed. Giving up.", maxRetries);
            throw new RuntimeException(
                    "AI resume generation failed after " + maxRetries + " attempts."
            );
        }

        log.info("🎉 Resume generation pipeline completed successfully.");

        return new GenerateResumeResponse(latexContent, base64Pdf);
    }

    public Resume getResumeById(UUID resumeId) {
        return resumeRepository.findById(resumeId)
                .orElseThrow(() -> new RuntimeException("Resume not found"));
    }
}