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
            GeminiPromptBuilder promptBuilder
    ) {
        this.geminiClient = geminiClient;
        this.promptBuilder = promptBuilder;
    }

    @Override
    public String generateOptimizedJson(String parsedResumeJson, String jobDescription) {

        // Build prompt
        String prompt = promptBuilder.buildLatexPrompt(
                parsedResumeJson,
                jobDescription
        );

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

        // Remove markdown fences safely
        if (cleaned.startsWith("```")) {
            cleaned = cleaned.replaceAll("```[a-zA-Z]*", "");
            cleaned = cleaned.replace("```", "").trim();
        }

        // 🔥 NO VALIDATION HERE
        // We let frontend validate JSON

        return cleaned;
    }
}
