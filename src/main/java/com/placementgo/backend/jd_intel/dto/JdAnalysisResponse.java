package com.placementgo.backend.jd_intel.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JdAnalysisResponse {
    private String company;
    private String role;
    private String difficultyLevel;
    private List<RoundInfo> roundStructure;
    private List<String> focusAreas;
    private List<String> primaryLanguages;
    private List<ChecklistItem> preparationChecklist;
    private List<QuestionPattern> questionPatterns;
    private List<EvaluationCriterion> evaluationCriteria;
    private List<String> technicalQuestions;
    private List<String> behavioralQuestions;
    private List<String> codingFocus;
    private List<String> systemDesignFocus;
    private List<String> companyTips;
    private List<String> rejectionReasons;
    private Integer confidenceScore;
    private String sourceSummary;
}