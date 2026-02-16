package com.placementgo.backend.resume.service;

public interface AiResumeGenerator {
    String generateOptimizedJson(String parsedResumeJson, String jobDescription);
}
