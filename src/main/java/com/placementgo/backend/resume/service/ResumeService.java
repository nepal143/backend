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

        Path uploadDir = Path.of("uploads");
        Files.createDirectories(uploadDir);

        Path filePath = uploadDir.resolve(
                UUID.randomUUID() + "_" + file.getOriginalFilename()
        );

        Files.write(filePath, file.getBytes());

        Resume resume = new Resume();
        resume.setUserId(userId);
        resume.setOriginalFileName(file.getOriginalFilename());
        resume.setStoredFilePath(filePath.toString());
        resume.setStatus(ResumeStatus.UPLOADED);
        resume.setCreatedAt(LocalDateTime.now());

        resumeRepository.save(resume);

        log.info("📁 File stored and metadata saved.");

        // Parse resume
        String parsedJson = parsingService.parse(filePath);
        log.info("✅ Resume parsed.");

        int maxRetries = 2;
        String latexContent = null;
        String base64Pdf = null;

        for (int attempt = 1; attempt <= maxRetries; attempt++) {

            log.info("🔁 AI generation attempt {}/{}", attempt, maxRetries);

            try {

                // 1️⃣ Generate LaTeX
                latexContent = aiResumeGenerator.generateOptimizedJson(
                        parsedJson,
                        jobDescription
                );

                log.info("🤖 AI returned LaTeX (length: {})", latexContent.length());

                // 2️⃣ Create temp runtime folder
                Path runtimeDir = Files.createTempDirectory("latex-runtime-");

                // Write .tex
                Path texFile = runtimeDir.resolve("main.tex");
                Files.writeString(texFile, latexContent, StandardCharsets.UTF_8);

                // Copy class file
                ClassPathResource classResource = new ClassPathResource("tempDir/tccv.cls");
                try (InputStream is = classResource.getInputStream()) {
                    Files.copy(is, runtimeDir.resolve("tccv.cls"),
                            StandardCopyOption.REPLACE_EXISTING);
                }

                // 3️⃣ Compile
                ProcessBuilder processBuilder = new ProcessBuilder(
                        "pdflatex",
                        "-interaction=nonstopmode",
                        "-halt-on-error",
                        "main.tex"
                );

                processBuilder.directory(runtimeDir.toFile());
                processBuilder.redirectErrorStream(true);

                Process process = processBuilder.start();

                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(process.getInputStream())
                );

                StringBuilder logOutput = new StringBuilder();
                String line;

                while ((line = reader.readLine()) != null) {
                    logOutput.append(line).append("\n");
                    log.info("📄 pdflatex: {}", line);
                }

                int exitCode = process.waitFor();

                if (exitCode != 0) {
                    log.warn("❌ Compilation failed on attempt {}.", attempt);
                    continue; // retry
                }

                // 4️⃣ Read PDF
                Path pdfPath = runtimeDir.resolve("main.pdf");

                if (!Files.exists(pdfPath)) {
                    log.warn("❌ PDF not generated on attempt {}.", attempt);
                    continue; // retry
                }

                byte[] pdfBytes = Files.readAllBytes(pdfPath);
                base64Pdf = Base64.getEncoder().encodeToString(pdfBytes);

                log.info("✅ Compilation successful on attempt {}.", attempt);

                // SUCCESS → break retry loop
                break;

            } catch (Exception e) {
                log.warn("⚠️ Attempt {} failed with exception: {}", attempt, e.getMessage());
            }
        }

        if (base64Pdf == null) {
            log.error("❌ All {} attempts failed. Giving up.", maxRetries);
            throw new RuntimeException("AI generation failed after 5 attempts.");
        }

        log.info("🎉 Resume generation pipeline completed successfully.");

        return new GenerateResumeResponse(latexContent, base64Pdf);
    }


    public Resume getResumeById(UUID resumeId) {
        return resumeRepository.findById(resumeId)
                .orElseThrow(() -> new RuntimeException("Resume not found"));
    }
}
