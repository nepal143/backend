package com.placementgo.backend.jd_intel.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.placementgo.backend.resume.ai.GeminiClient;
import com.placementgo.backend.jd_intel.dto.JdAnalysisResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class LlmExtractionService {

    private final GeminiClient geminiClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public JdAnalysisResponse extractInsights(
            String aggregatedText,
            String company,
            String role,
            String jobDescription) {

        log.info("Sending text to Gemini for extraction...");

        // ✅ Handle empty input early
        // if ((aggregatedText == null || aggregatedText.isBlank())
        // && (jobDescription == null || jobDescription.isBlank())) {

        // log.warn("No input data available for extraction.");

        // return JdAnalysisResponse.builder()
        // .company(company)
        // .role(role)
        // .difficultyLevel("Unknown")
        // .confidenceScore(0)
        // .sourceSummary("No data available")
        // .build();
        // }

        String prompt = createPrompt(aggregatedText, company, role, jobDescription);

        try {
            String content = geminiClient.generateContent(prompt);

            if (content == null || content.isBlank()) {
                log.warn("Received empty response from Gemini.");
                return fallbackResponse(company, role);
            }

            log.info("Gemini RAW response:\n{}", content);

            // ✅ Clean markdown if Gemini returns it
            content = cleanJson(content);

            // ✅ Parse safely
            JsonNode node = objectMapper.readTree(content);
            JdAnalysisResponse response = objectMapper.treeToValue(node, JdAnalysisResponse.class);

            // ✅ Basic sanity fallback
            if (response.getCompany() == null) {
                response.setCompany(company);
            }
            if (response.getRole() == null) {
                response.setRole(role);
            }

            return response;

        } catch (Exception e) {
            log.error("Error during LLM extraction: {}", e.getMessage());
            return fallbackResponse(company, role);
        }
    }

    // 🔥 Strong Prompt (UI-aligned)
    private String createPrompt(String text, String company, String role, String jobDescription) {

        int maxChars = 30000;
        String safeText = (text == null) ? "" : (text.length() > maxChars ? text.substring(0, maxChars) : text);

        String safeJd = (jobDescription == null) ? "" : jobDescription;

        return String.format(
                """
                        You are an expert technical recruiter and interview intelligence analyst.

                        Your task is to analyze job description and interview experiences and generate structured insights for a frontend dashboard.

                        STRICT RULES:
                        - Return ONLY valid JSON
                        - No markdown, no explanation
                        - All fields must exist
                        - Do NOT hallucinate unknown data
                        - Keep arrays empty if unsure
                        - Percentages must sum to 100
                        - difficultyLevel ∈ ["Easy","Medium","Hard","Mixed","Unknown"]
                        - confidenceScore: 0-100

                        Context:
                        Company: %s
                        Role: %s

                        Job Description:
                        %s

                        Interview Experiences:
                        %s

                        Return JSON:

                        {
                          "company": "%s",
                          "role": "%s",
                          "difficultyLevel": "Medium",

                          "roundStructure": [
                            {
                              "step": 1,
                              "title": "Online Assessment",
                              "duration": "90 mins",
                              "description": "DSA problems"
                            }
                          ],

                          "focusAreas": [
                            "Dynamic Programming",
                            "Graphs",
                            "OOP"
                          ],

                          "primaryLanguages": [
                            "Java",
                            "C++",
                            "Python"
                          ],

                          "preparationChecklist": [
                            {
                              "title": "Practice DSA",
                              "description": "Focus on graphs and DP",
                              "completed": false,
                              "priority": 1
                            }
                          ],

                          "questionPatterns": [
                            {
                              "category": "Algorithmic",
                              "pattern": "Optimization and constraints"
                            }
                          ],

                          "evaluationCriteria": [
                            {"name": "Cognitive Ability", "percentage": 40},
                            {"name": "Role Knowledge", "percentage": 30},
                            {"name": "Leadership", "percentage": 20},
                            {"name": "Communication", "percentage": 10}
                          ],

                          "technicalQuestions": [],
                          "behavioralQuestions": [],
                          "codingFocus": [],
                          "systemDesignFocus": [],
                          "companyTips": [],
                          "rejectionReasons": [],

                          "confidenceScore": 75,
                          "sourceSummary": "Derived from JD and interview patterns"
                        }

                        IMPORTANT:
                        - Keep data concise and UI-friendly
                        - Focus on actionable insights
                        - Deduplicate similar entries

                        """,
                company, role, safeJd, safeText, company, role);
    }

    // 🧹 Clean markdown wrapper
    private String cleanJson(String content) {
        content = content.trim();

        if (content.startsWith("```json")) {
            content = content.substring(7);
        } else if (content.startsWith("```")) {
            content = content.substring(3);
        }

        if (content.endsWith("```")) {
            content = content.substring(0, content.length() - 3);
        }

        return content.trim();
    }

    // 🛟 Fallback response
    private JdAnalysisResponse fallbackResponse(String company, String role) {
        return JdAnalysisResponse.builder()
                .company(company)
                .role(role)
                .difficultyLevel("Unknown")
                .confidenceScore(0)
                .sourceSummary("Fallback response due to parsing failure")
                .build();
    }
}