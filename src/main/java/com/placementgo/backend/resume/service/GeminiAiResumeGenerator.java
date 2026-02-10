package com.placementgo.backend.resume.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.placementgo.backend.resume.ai.GeminiClient;
import com.placementgo.backend.resume.ai.GeminiPromptBuilder;
import org.springframework.stereotype.Service;

@Service
public class GeminiAiResumeGenerator implements AiResumeGenerator {

    private final GeminiClient geminiClient;
    private final GeminiPromptBuilder promptBuilder;
    private final ObjectMapper mapper = new ObjectMapper();

    public GeminiAiResumeGenerator(
            GeminiClient geminiClient,
            GeminiPromptBuilder promptBuilder
    ) {
        this.geminiClient = geminiClient;
        this.promptBuilder = promptBuilder;
    }

    @Override
    public String generate(String parsedResumeJson, String jobDescription) {

        try {
            String prompt = promptBuilder.build(parsedResumeJson, jobDescription);

            String aiResponse = geminiClient.generateContent(prompt);

            // Validate JSON (fail fast if Gemini misbehaves)
            mapper.readTree(aiResponse);

            return aiResponse;

        } catch (Exception e) {
            throw new RuntimeException("AI resume generation failed", e);
        }
    }
}
