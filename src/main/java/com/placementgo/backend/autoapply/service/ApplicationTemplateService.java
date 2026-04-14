package com.placementgo.backend.autoapply.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.placementgo.backend.resume.ai.GroqClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Generates a ready-to-use application package for a job lead:
 *  - A personalised cover letter
 *  - An email subject line
 *  - Pre-filled field hints (name, phone, years-of-exp, etc.)
 *
 * The result is serialised to JSON and stored in {@code AutoApplyJobLead.applicationTemplate}.
 */
@Service
@Slf4j
public class ApplicationTemplateService {

    private final GroqClient groqClient;
    private final ObjectMapper objectMapper;

    public ApplicationTemplateService(@Qualifier("ResumeGroqClient") GroqClient groqClient,
                                      ObjectMapper objectMapper) {
        this.groqClient = groqClient;
        this.objectMapper = objectMapper;
    }

    /**
     * @param parsedResumeJson  JSON string from ResumeParsingService
     * @param jobTitle          e.g. "Backend Engineer"
     * @param company           e.g. "Stripe"
     * @param jobDescription    Full JD text (trimmed to 3000 chars internally)
     * @return JSON string: { coverLetter, subject, fields }
     */
    public String generate(String parsedResumeJson, String jobTitle, String company, String jobDescription) {
        String truncatedJd = jobDescription != null && jobDescription.length() > 2500
                ? jobDescription.substring(0, 2500) + "..."
                : jobDescription;

        String prompt = """
You are a professional job application assistant. Create a complete application package.

CANDIDATE RESUME (JSON):
%s

JOB TITLE: %s
COMPANY: %s
JOB DESCRIPTION:
%s

Generate a tailored application package. Respond ONLY with valid JSON, no markdown:
{
  "subject": "<email subject line, e.g. Application for Backend Engineer – [CandidateName]>",
  "coverLetter": "<3-4 paragraph professional cover letter tailored to this role>",
  "fields": {
    "fullName": "<from resume>",
    "email": "<from resume>",
    "phone": "<from resume or empty string>",
    "yearsOfExperience": "<number>",
    "currentTitle": "<from resume>",
    "linkedinUrl": "<from resume or empty string>",
    "githubUrl": "<from resume or empty string>",
    "portfolioUrl": "<from resume or empty string>",
    "summary": "<2-sentence professional summary tailored to this role>"
  }
}
""".formatted(parsedResumeJson, jobTitle, company, truncatedJd);

        try {
            String raw = groqClient.generateContent(prompt);
            if (raw == null || raw.isBlank()) return fallbackTemplate(jobTitle, company);

            String cleaned = raw.trim();
            if (cleaned.contains("```")) {
                cleaned = cleaned.replaceAll("(?s)```[a-zA-Z]*\r?\n?", "").replace("```", "").trim();
            }
            int start = cleaned.indexOf('{');
            int end = cleaned.lastIndexOf('}');
            if (start < 0 || end < 0) return fallbackTemplate(jobTitle, company);

            // Validate it parses, then return the raw JSON string
            String extracted = cleaned.substring(start, end + 1);
            objectMapper.readTree(extracted); // throws if invalid
            return extracted;
        } catch (Exception e) {
            log.warn("Template generation error for '{}' at '{}': {}", jobTitle, company, e.getMessage());
            return fallbackTemplate(jobTitle, company);
        }
    }

    private String fallbackTemplate(String jobTitle, String company) {
        try {
            return objectMapper.writeValueAsString(Map.of(
                    "subject", "Application for " + jobTitle + " at " + company,
                    "coverLetter", "Dear Hiring Team,\n\nI am writing to express my interest in the "
                            + jobTitle + " position at " + company + ". Please find my resume attached.\n\nBest regards",
                    "fields", Map.of(
                            "fullName", "",
                            "email", "",
                            "phone", "",
                            "yearsOfExperience", "",
                            "currentTitle", "",
                            "linkedinUrl", "",
                            "githubUrl", "",
                            "portfolioUrl", "",
                            "summary", ""
                    )
            ));
        } catch (Exception ex) {
            return "{}";
        }
    }
}
