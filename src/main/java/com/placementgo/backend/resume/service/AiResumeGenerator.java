package com.placementgo.backend.resume.service;

public interface AiResumeGenerator {
    String generate(String parsedResumeJson, String jobDescription);
}
