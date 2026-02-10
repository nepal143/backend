package com.placementgo.backend.resume.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class MockAiResumeGenerator implements AiResumeGenerator {

    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public String generate(String parsedResumeJson, String jobDescription) {

        try {
            Map<String, Object> parsedResume =
                    mapper.readValue(parsedResumeJson, Map.class);

            Map<String, Object> generated = new LinkedHashMap<>();

            generated.put("title", "Job-Tailored Resume");

            generated.put(
                    "summary",
                    "Resume optimized for the following job description:\n" +
                            jobDescription.substring(0, Math.min(300, jobDescription.length()))
            );

            generated.put("skills", parsedResume.getOrDefault("skills", List.of()));
            generated.put("rawResumeText", parsedResume.get("rawText"));

            generated.put("optimizationNotes", List.of(
                    "Keywords aligned with job description",
                    "ATS-friendly formatting",
                    "Experience reframed for role relevance"
            ));

            return mapper.writeValueAsString(generated);

        } catch (Exception e) {
            throw new RuntimeException("Failed to generate resume", e);
        }
    }
}
