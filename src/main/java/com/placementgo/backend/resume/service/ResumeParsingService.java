package com.placementgo.backend.resume.service;


import com.placementgo.backend.resume.model.Resume;
import com.placementgo.backend.resume.model.ResumeStatus;
import com.placementgo.backend.resume.repository.ResumeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ResumeParsingService {

    private final ResumeRepository resumeRepository;
    private final ResumeTextExtractor textExtractor;
    private final ResumeStructurer structurer;

    @Async
    public void parseAsync(UUID resumeId) {

        Resume resume = resumeRepository.findById(resumeId)
                .orElseThrow();

        try {
            resume.setStatus(ResumeStatus.PARSING);
            resumeRepository.save(resume);

            File file = new File(resume.getStoredFilePath());

            // 1️⃣ Read actual resume content
            String rawText = textExtractor.extractText(file);

            // 2️⃣ Convert to structured JSON
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
