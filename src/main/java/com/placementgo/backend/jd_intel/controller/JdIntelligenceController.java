package com.placementgo.backend.jd_intel.controller;

import com.placementgo.backend.jd_intel.dto.JdAnalysisRequest;
import com.placementgo.backend.jd_intel.dto.JdAnalysisResponse;
import com.placementgo.backend.jd_intel.service.JdIntelligenceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/jd-intel")
@Slf4j
@RequiredArgsConstructor
public class JdIntelligenceController {

    private final JdIntelligenceService service;

    @PostMapping("/analyze")
    public ResponseEntity<JdAnalysisResponse> analyze(@RequestBody JdAnalysisRequest request) {
        log.info("Received JD Analysis request for company: {}", request.getCompany());
        JdAnalysisResponse response = service.analyze(request);
        return ResponseEntity.ok(response);
    }
}
