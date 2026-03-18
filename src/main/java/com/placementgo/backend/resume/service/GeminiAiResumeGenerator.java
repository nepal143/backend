package com.placementgo.backend.resume.service;

import com.placementgo.backend.resume.ai.GeminiClient;
import com.placementgo.backend.resume.ai.GeminiPromptBuilder;
import org.springframework.stereotype.Service;

@Service
public class GeminiAiResumeGenerator implements AiResumeGenerator {

    private final GeminiClient geminiClient;
    private final GeminiPromptBuilder promptBuilder;

    public GeminiAiResumeGenerator(
            GeminiClient geminiClient,
            GeminiPromptBuilder promptBuilder) {
        this.geminiClient = geminiClient;
        this.promptBuilder = promptBuilder;
    }

    @Override
    public String generateOptimizedJson(String parsedResumeJson, String jobDescription, String templateId) {

        // Build prompt
        String prompt = promptBuilder.buildLatexPrompt(
                parsedResumeJson,
                jobDescription,
                templateId);

        // Call Gemini
        String aiResponse = geminiClient.generateContent(prompt);

        if (aiResponse == null || aiResponse.isBlank()) {
            // Instead of throwing error, return fallback JSON
            return """
                    {
                      "optimized_resume": {},
                      "gap_analysis": {
                        "overall_alignment_assessment": "AI did not return content."
                      }
                    }
                    """;
        }

        String cleaned = aiResponse.trim();

        // Strip any markdown code fences (wherever they appear)
        if (cleaned.contains("```")) {
            cleaned = cleaned.replaceAll("(?s)```[a-zA-Z]*\\r?\\n?", "").replace("```", "").trim();
        }

        // Extract just the LaTeX document — drop any leading explanatory text the AI may have added
        int docStart = cleaned.indexOf("\\documentclass");
        if (docStart > 0) {
            cleaned = cleaned.substring(docStart);
        }
        int docEnd = cleaned.lastIndexOf("\\end{document}");
        if (docEnd >= 0) {
            cleaned = cleaned.substring(0, docEnd + "\\end{document}".length());
        }

        // Fix common AI mistake: label={]} (bracket inside brace) → label={}] (brace then bracket)
        cleaned = cleaned.replace("label={]}", "label={}]");

        // Fix: AI sometimes uses raw \begin{itemize}[leftmargin=0in,label={}] instead of the macro.
        // Safe to replace since \resumeSubHeadingListStart expands to exactly the same thing,
        // and the corresponding \end{itemize} still correctly closes the environment.
        cleaned = cleaned.replace("\\begin{itemize}[leftmargin=0in,label={}]", "\\resumeSubHeadingListStart");

        // Strip custom colors (e.g. accent-blue, light-gray) not defined in main.tex preamble.
        // Standard color names (red, blue, black, etc.) do not contain hyphens.
        cleaned = cleaned.replaceAll("\\\\color\\{[a-zA-Z]+-[a-zA-Z0-9-]+\\}", "");

        return cleaned;
    }
}
