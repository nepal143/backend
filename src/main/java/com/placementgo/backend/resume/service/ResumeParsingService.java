package com.placementgo.backend.resume.service;

import com.placementgo.backend.resume.model.Resume;
import com.placementgo.backend.resume.model.ResumeStatus;
import com.placementgo.backend.resume.repository.ResumeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ResumeParsingService {

    private final ResumeRepository resumeRepository;
    private final ResumeTextExtractor textExtractor;
    private final ResumeStructurer structurer;

    /**
     * ✅ SYNCHRONOUS parsing (used in uploadAndGenerate flow)
     * Now fully in-memory (no file saving)
     */
    public String parse(InputStream inputStream) throws Exception {

        // 1️⃣ Extract raw text directly from InputStream
        String rawText = textExtractor.extractText(inputStream);

        // 2️⃣ Convert to structured JSON
        return structurer.structure(rawText);
    }


    /**
     * ⚠️ OPTIONAL ASYNC parsing
     * If you want async later, you should store file in S3
     * and fetch stream from there instead of local path.
     */
    @Async
    public void parseAsync(UUID resumeId, InputStream inputStream) {

        Resume resume = resumeRepository.findById(resumeId)
                .orElseThrow();

        try {
            resume.setStatus(ResumeStatus.PARSING);
            resumeRepository.save(resume);

            String rawText = textExtractor.extractText(inputStream);
            String parsedJson = structurer.structure(rawText);

            resume.setParsedJson(parsedJson);
            resume.setStatus(ResumeStatus.PARSED);
            resumeRepository.save(resume);

        } catch (Exception e) {
            resume.setStatus(ResumeStatus.FAILED);
            resumeRepository.save(resume);
        }
    }
}