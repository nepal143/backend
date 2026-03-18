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
import java.util.concurrent.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class JdIntelligenceService {

        private final SearchService searchService;
        private final ScrapingService scrapingService;
        private final LlmExtractionService llmExtractionService;
        private final InterviewInsightRepository repository;

        // 🔥 Controlled thread pool (better than default)
        private final ExecutorService executor = Executors.newFixedThreadPool(5);

        public JdAnalysisResponse analyze(JdAnalysisRequest request) {

                log.info("🚀 Starting JD Analysis for {} - {}", request.getCompany(), request.getRole());

                // 1️⃣ Generate Search Queries
                List<String> queries = generateQueries(request);

                // 2️⃣ Search URLs
                List<String> urls = queries.parallelStream()
                                .map(searchService::search)
                                .flatMap(List::stream)
                                .distinct()
                                .limit(10)
                                .collect(Collectors.toList());

                log.info("🔍 Found {} URLs to scrape", urls.size());

                // 3️⃣ Scrape Content (Parallel + Timeout + Filtering)
                String aggregatedContent = scrapeContent(urls);

                log.info("📄 Aggregated content length: {}", aggregatedContent.length());

                // 4️⃣ Extract Insights via LLM
                JdAnalysisResponse response = llmExtractionService.extractInsights(
                                aggregatedContent,
                                request.getCompany(),
                                request.getRole(),
                                request.getJobDescription());

                // 5️⃣ Save to DB
                saveInsight(request, response);

                return response;
        }

        // 🔥 Improved scraping pipeline
        private String scrapeContent(List<String> urls) {

                if (urls == null || urls.isEmpty()) {
                        log.warn("⚠️ No URLs found, skipping scraping.");
                        return "";
                }

                List<CompletableFuture<String>> futures = urls.stream()
                                .map(url -> CompletableFuture.supplyAsync(() -> {
                                        try {
                                                String content = scrapingService.scrape(url);

                                                if (content == null || content.isBlank()) {
                                                        return "";
                                                }

                                                // 🔥 Filter noisy content
                                                if (content.length() < 200) {
                                                        return "";
                                                }

                                                return content;

                                        } catch (Exception e) {
                                                log.warn("⚠️ Failed scraping URL: {} | {}", url, e.getMessage());
                                                return "";
                                        }
                                }, executor).orTimeout(10, TimeUnit.SECONDS)
                                                .exceptionally(ex -> {
                                                        log.warn("⏱️ Timeout or error while scraping: {}",
                                                                        ex.getMessage());
                                                        return "";
                                                }))
                                .toList();

                return futures.stream()
                                .map(CompletableFuture::join)
                                .filter(content -> !content.isBlank())
                                .collect(Collectors.joining("\n\n"));
        }

        // 💾 Save important insights
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
                                        .difficultyLevel(response.getDifficultyLevel())
                                        .rejectionReasons(response.getRejectionReasons())
                                        .companyTips(response.getCompanyTips())
                                        .build();

                        repository.save(insight);

                        log.info("💾 Saved InterviewInsight for {} - {}", request.getCompany(), request.getRole());

                } catch (Exception e) {
                        log.error("❌ Failed to save InterviewInsight: {}", e.getMessage());
                }
        }

        // 🔎 Better query generation
        private List<String> generateQueries(JdAnalysisRequest request) {

                String company = request.getCompany();
                String role = request.getRole();

                List<String> queries = new ArrayList<>();

                queries.add(company + " " + role + " interview experience");
                queries.add(company + " " + role + " interview questions");
                queries.add(company + " " + role + " leetcode discuss");
                queries.add(company + " " + role + " geeksforgeeks interview");
                queries.add(company + " " + role + " glassdoor interview");

                return queries;
        }
}