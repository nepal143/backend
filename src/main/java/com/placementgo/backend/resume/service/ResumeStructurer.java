package com.placementgo.backend.resume.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class ResumeStructurer {

    private final ObjectMapper mapper = new ObjectMapper();

    public String structure(String rawText) {
        Map<String, Object> json = new LinkedHashMap<>();

        // VERY basic heuristics (can improve later)
        json.put("rawText", rawText);

        List<String> skills = new ArrayList<>();
        if (rawText.toLowerCase().contains("java")) skills.add("Java");
        if (rawText.toLowerCase().contains("spring")) skills.add("Spring Boot");
        if (rawText.toLowerCase().contains("sql")) skills.add("SQL");

        json.put("skills", skills);

        try {
            return mapper.writeValueAsString(json);
        } catch (Exception e) {
            throw new RuntimeException("Failed to structure resume", e);
        }
    }
}
