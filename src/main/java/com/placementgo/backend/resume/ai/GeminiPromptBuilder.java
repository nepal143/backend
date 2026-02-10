package com.placementgo.backend.resume.ai;

import org.springframework.stereotype.Component;

@Component
public class GeminiPromptBuilder {

    public String build(String parsedResumeJson, String jobDescription) {

        return """
        You are an ATS-optimized resume generator.

        RULES:
        1. Output ONLY valid JSON.
        2. Do NOT include explanations or markdown.
        3. Do NOT invent skills, experience, companies, or education.
        4. Rephrase existing content to better match the job description.
        5. Use concise, professional, ATS-friendly language.

        INPUT_RESUME_JSON:
        %s

        JOB_DESCRIPTION:
        %s

        OUTPUT JSON FORMAT:
        {
          "title": "string",
          "summary": "string",
          "skills": ["string"],
          "experience": [
            {
              "company": "string",
              "role": "string",
              "bullets": ["string"]
            }
          ]
        }
        """.formatted(parsedResumeJson, jobDescription);
    }
}
