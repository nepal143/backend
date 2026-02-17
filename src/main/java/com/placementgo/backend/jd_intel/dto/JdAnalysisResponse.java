package com.placementgo.backend.jd_intel.dto;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class JdAnalysisResponse {
    private String company;
    private String role;
    private List<String> technicalQuestions;
    private List<String> behavioralQuestions;
    private List<String> codingFocus;
    private List<String> systemDesignFocus;
    private List<String> predictedRounds;
    private String difficultyLevel;
    private List<String> rejectionReasons;
    private List<String> companyTips;
}
