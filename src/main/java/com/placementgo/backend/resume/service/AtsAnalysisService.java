package com.placementgo.backend.resume.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.placementgo.backend.resume.ai.GroqClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class AtsAnalysisService {

    private final GroqClient groqClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AtsAnalysisService(@Qualifier("ResumeGroqClient") GroqClient groqClient) {
        this.groqClient = groqClient;
    }

    public record AtsResult(int score, List<String> suggestions) {}

    public AtsResult analyze(String parsedResumeJson, String jobDescription) {
        String prompt = """
You are an ATS (Applicant Tracking System) expert. Analyze the following resume against the job description.

RESUME (JSON):
%s

JOB DESCRIPTION:
%s

Your task:
1. Compute an ATS compatibility score from 0 to 100 based on:
   - Keyword match between resume and job description
   - Presence of required skills
   - Relevant experience alignment
   - Education fit
   - Quantifiable achievements

2. Generate 5-8 specific, actionable improvement suggestions such as:
   - Missing technical skills to add (e.g. "Add Docker/Kubernetes to your skills section")
   - Project ideas that would strengthen the resume for this role
   - Certifications or courses that would help
   - Ways to reword experience bullet points
   - Sections that are weak or missing

Respond ONLY with valid JSON in this exact format (no extra text, no markdown):
{
  "ats_score": <integer 0-100>,
  "suggestions": [
    "<suggestion 1>",
    "<suggestion 2>",
    "<suggestion 3>",
    "<suggestion 4>",
    "<suggestion 5>"
  ]
}
""".formatted(parsedResumeJson, jobDescription);

        try {
            String response = groqClient.generateContent(prompt);

            if (response == null || response.isBlank()) {
                log.warn("ATS analysis returned empty response, using defaults");
                return new AtsResult(0, List.of("Could not analyze resume. Please try again."));
            }

            String cleaned = response.trim();
            // Strip markdown code fences if present
            if (cleaned.contains("```")) {
                cleaned = cleaned.replaceAll("(?s)```[a-zA-Z]*\\r?\\n?", "").replace("```", "").trim();
            }

            // Find JSON object boundaries
            int start = cleaned.indexOf('{');
            int end = cleaned.lastIndexOf('}');
            if (start >= 0 && end > start) {
                cleaned = cleaned.substring(start, end + 1);
            }

            JsonNode node = objectMapper.readTree(cleaned);
            int score = node.path("ats_score").asInt(0);
            score = Math.max(0, Math.min(100, score));

            List<String> suggestions = new ArrayList<>();
            JsonNode suggestionsNode = node.path("suggestions");
            if (suggestionsNode.isArray()) {
                for (JsonNode s : suggestionsNode) {
                    String text = s.asText("").trim();
                    if (!text.isEmpty()) suggestions.add(text);
                }
            }

            if (suggestions.isEmpty()) {
                suggestions.add("No specific suggestions generated. Try providing a more detailed job description.");
            }

            log.info("ATS analysis complete: score={}, suggestions={}", score, suggestions.size());
            return new AtsResult(score, suggestions);

        } catch (Exception e) {
            log.warn("ATS analysis failed: {}", e.getMessage());
            return new AtsResult(0, List.of("ATS analysis failed: " + e.getMessage()));
        }
    }
}
