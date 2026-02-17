package com.placementgo.backend.jd_intel.service;

import com.placementgo.backend.jd_intel.dto.JdAnalysisRequest;
import com.placementgo.backend.jd_intel.dto.JdAnalysisResponse;
import com.placementgo.backend.jd_intel.entity.InterviewInsight;
import com.placementgo.backend.jd_intel.repository.InterviewInsightRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class JdIntelligenceService {

        private final SearchService searchService;
        private final ScrapingService scrapingService;
        private final LlmExtractionService llmExtractionService;
        private final InterviewInsightRepository repository;

        public JdAnalysisResponse analyze(JdAnalysisRequest request) {
                log.info("Starting JD Analysis for {} - {}", request.getCompany(), request.getRole());

                // 1. Generate Search Queries
                List<String> queries = generateQueries(request);

                // 2. Search for URLs (Parallel)
                List<String> urls = queries.parallelStream()
                                .map(searchService::search)
                                .flatMap(List::stream)
                                .distinct()
                                .limit(10) // Limit to top 10 unique URLs to avoid overloading
                                .collect(Collectors.toList());

                log.info("Found {} URLs to scrape", urls.size());

                // 3. Scrape Content (Parallel)
                StringBuilder aggregatedContent = new StringBuilder();

                List<CompletableFuture<String>> futures = urls.stream()
                                .map(url -> CompletableFuture.supplyAsync(() -> scrapingService.scrape(url)))
                                .toList();

                futures.stream()
                                .map(CompletableFuture::join)
                                .forEach(content -> aggregatedContent.append(content).append("\n\n"));

                // 4. Extract Insights using LLM
                JdAnalysisResponse response = llmExtractionService.extractInsights(
                                aggregatedContent.toString(),
                                request.getCompany(),
                                request.getRole(),
                                request.getJobDescription());

                // 5. Save to DB
                saveInsight(request, response);

                return response;
        }

        private void saveInsight(JdAnalysisRequest request, JdAnalysisResponse response) {
                try {
                        InterviewInsight insight = InterviewInsight.builder()
                                        .company(request.getCompany())
                                        .role(request.getRole())
                                        .jobDescription(request.getJobDescription())
                                        .technicalQuestions(response.getTechnicalQuestions())
                                        .behavioralQuestions(response.getBehavioralQuestions())
                                        .codingFocus(response.getCodingFocus())
                                        .systemDesignFocus(response.getSystemDesignFocus())
                                        .predictedRounds(response.getPredictedRounds())
                                        .difficultyLevel(response.getDifficultyLevel())
                                        .rejectionReasons(response.getRejectionReasons())
                                        .companyTips(response.getCompanyTips())
                                        .build();

                        repository.save(insight);
                        log.info("Saved InterviewInsight for {} - {}", request.getCompany(), request.getRole());
                } catch (Exception e) {
                        log.error("Failed to save InterviewInsight: {}", e.getMessage());
                }
        }

        private List<String> generateQueries(JdAnalysisRequest request) {
                List<String> queries = new ArrayList<>();
                String base = request.getCompany() + " " + request.getRole() + " interview experience";
                queries.add(base);
                queries.add(request.getCompany() + " " + request.getRole() + " interview questions");
                queries.add(request.getCompany() + " " + request.getRole() + " leetcode discuss");
                queries.add(request.getCompany() + " " + request.getRole() + " geeksforgeeks");
                return queries;
        }
}
