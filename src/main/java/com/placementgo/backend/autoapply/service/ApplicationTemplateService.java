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
        // No resume → nothing to personalise; return empty fallback immediately
        if (parsedResumeJson == null || parsedResumeJson.isBlank()) {
            log.info("No resume data available for template generation ({}/{})", jobTitle, company);
            return fallbackTemplate(jobTitle, company);
        }

        String truncatedJd = jobDescription != null && jobDescription.length() > 2500
                ? jobDescription.substring(0, 2500) + "..."
                : jobDescription;

        String prompt = """
You are a professional job application assistant. Create a complete application package.

IMPORTANT: Extract ALL personal details (name, email, phone, LinkedIn, GitHub, etc.) \
ONLY from the CANDIDATE RESUME JSON below. \
NEVER invent, guess, or use placeholder values for personal information. \
If a field is not present in the resume, output an EMPTY STRING "" for that field.

CANDIDATE RESUME (JSON):
%s

JOB TITLE: %s
COMPANY: %s
JOB DESCRIPTION:
%s

Generate a tailored application package. Respond ONLY with valid JSON, no markdown:
{
  "subject": "<email subject line using the candidate's real name from the resume>",
  "coverLetter": "<3-4 paragraph professional cover letter tailored to this role, using real details from the resume>",
  "fields": {
    "fullName": "<exact full name from resume, or empty string>",
    "email": "<exact email from resume, or empty string>",
    "phone": "<exact phone from resume, or empty string>",
    "yearsOfExperience": "<number derived from resume experience, or empty string>",
    "currentTitle": "<most recent title from resume, or empty string>",
    "linkedinUrl": "<exact LinkedIn URL from resume, or empty string>",
    "githubUrl": "<exact GitHub URL from resume, or empty string>",
    "portfolioUrl": "<exact portfolio URL from resume, or empty string>",
    "summary": "<2-sentence professional summary tailored to this role, based on real resume content>"
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
