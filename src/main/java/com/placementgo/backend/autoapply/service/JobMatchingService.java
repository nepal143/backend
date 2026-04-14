package com.placementgo.backend.autoapply.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.placementgo.backend.resume.ai.GroqClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Uses the existing Groq (LLaMA-3.3-70b) client to score how well a job
 * description matches a user's parsed resume JSON.
 */
@Service
@Slf4j
public class JobMatchingService {

    private final GroqClient groqClient;
    private final ObjectMapper objectMapper;

    public JobMatchingService(@Qualifier("ResumeGroqClient") GroqClient groqClient,
                              ObjectMapper objectMapper) {
        this.groqClient = groqClient;
        this.objectMapper = objectMapper;
    }

    public record MatchResult(int score, List<String> reasons) {}

    /**
     * @param parsedResumeJson  JSON string previously extracted by ResumeParsingService
     * @param jobTitle          Title of the job posting
     * @param jobDescription    Full JD text
     */
    public MatchResult score(String parsedResumeJson, String jobTitle, String jobDescription) {
        // Truncate JD to avoid token limits
        String truncatedJd = jobDescription != null && jobDescription.length() > 3000
                ? jobDescription.substring(0, 3000) + "..."
                : jobDescription;

        String prompt = """
You are an expert technical recruiter. Score how well the candidate's resume matches the job.

RESUME (JSON):
%s

JOB TITLE: %s
JOB DESCRIPTION:
%s

Respond ONLY with valid JSON, no markdown, no extra text:
{
  "match_score": <integer 0-100>,
  "reasons": [
    "<reason 1>",
    "<reason 2>",
    "<reason 3>"
  ]
}

Scoring guide:
- 80-100: Excellent match – most skills/experience align
- 60-79:  Good match – candidate meets core requirements
- 40-59:  Partial match – some gaps but worth applying
- 0-39:   Poor match – major skill/experience mismatch
""".formatted(parsedResumeJson, jobTitle, truncatedJd);

        try {
            String raw = groqClient.generateContent(prompt);
            if (raw == null || raw.isBlank()) return fallback();

            String cleaned = raw.trim();
            if (cleaned.contains("```")) {
                cleaned = cleaned.replaceAll("(?s)```[a-zA-Z]*\r?\n?", "").replace("```", "").trim();
            }
            int start = cleaned.indexOf('{');
            int end = cleaned.lastIndexOf('}');
            if (start < 0 || end < 0) return fallback();

            JsonNode node = objectMapper.readTree(cleaned.substring(start, end + 1));
            int score = node.path("match_score").asInt(50);
            List<String> reasons = objectMapper.readerForListOf(String.class)
                    .readValue(node.path("reasons"));
            return new MatchResult(Math.max(0, Math.min(100, score)), reasons);
        } catch (Exception e) {
            log.warn("Job matching AI error for '{}': {}", jobTitle, e.getMessage());
            return fallback();
        }
    }

    private MatchResult fallback() {
        return new MatchResult(50, List.of("AI scoring unavailable – manual review recommended"));
    }
}
