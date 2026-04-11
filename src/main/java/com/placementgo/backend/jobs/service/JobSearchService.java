package com.placementgo.backend.jobs.service;

import com.placementgo.backend.jobs.dto.JobDto;
import com.placementgo.backend.jobs.dto.JobSearchResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class JobSearchService {

    @Value("${jd-intel.search-api-key}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper;

    public JobSearchResponse searchPremiumJobs(String query) {
        log.info("Searching premium jobs for: {}", query);

        if (apiKey != null && !apiKey.isEmpty()) {
            return searchViaSerpApi(query);
        }

        log.info("SEARCH_API_KEY not set — falling back to Remotive free API");
        return searchViaRemotive(query);
    }

    // ── SerpAPI (primary, requires SEARCH_API_KEY env var) ──────────────────

    private JobSearchResponse searchViaSerpApi(String query) {
        String url = UriComponentsBuilder.fromUriString("https://serpapi.com/search.json")
                .queryParam("engine", "google_jobs")
                .queryParam("q", query)
                .queryParam("hl", "en")
                .queryParam("api_key", apiKey)
                .toUriString();

        try {
            String jsonResponse = restTemplate.getForObject(url, String.class);
            JsonNode rootNode = objectMapper.readTree(jsonResponse);

            if (rootNode == null || !rootNode.has("jobs_results")) {
                log.warn("No 'jobs_results' found in SerpApi response — falling back to Remotive");
                return searchViaRemotive(query);
            }

            JsonNode jobsNode = rootNode.path("jobs_results");
            List<JobDto> parsedJobs = new ArrayList<>();
            if (jobsNode.isArray()) {
                for (JsonNode item : jobsNode) {
                    parsedJobs.add(mapToJobDto(item));
                }
            }

            if (parsedJobs.isEmpty()) {
                log.info("SerpAPI returned 0 jobs — falling back to Remotive");
                return searchViaRemotive(query);
            }

            return JobSearchResponse.builder()
                    .query(query)
                    .totalResults(parsedJobs.size())
                    .jobs(parsedJobs)
                    .build();

        } catch (Exception e) {
            log.error("SerpAPI error: {} — falling back to Remotive", e.getMessage());
            return searchViaRemotive(query);
        }
    }

    // ── Remotive fallback (free, no key required) ────────────────────────────

    private JobSearchResponse searchViaRemotive(String query) {
        // Map role categories to Remotive categories
        String category = mapToRemotiveCategory(query);

        String url = UriComponentsBuilder.fromUriString("https://remotive.com/api/remote-jobs")
                .queryParam("category", category)
                .queryParam("search", query)
                .queryParam("limit", 20)
                .toUriString();

        try {
            String jsonResponse = restTemplate.getForObject(url, String.class);
            JsonNode rootNode = objectMapper.readTree(jsonResponse);
            JsonNode jobsNode = rootNode.path("jobs");
            List<JobDto> parsedJobs = new ArrayList<>();

            if (jobsNode.isArray()) {
                for (JsonNode item : jobsNode) {
                    parsedJobs.add(mapFromRemotive(item));
                }
            }

            log.info("Remotive returned {} jobs for query '{}'", parsedJobs.size(), query);
            return JobSearchResponse.builder()
                    .query(query)
                    .totalResults(parsedJobs.size())
                    .jobs(parsedJobs)
                    .build();

        } catch (Exception e) {
            log.error("Remotive API error: {}", e.getMessage(), e);
            return JobSearchResponse.builder().query(query).totalResults(0).jobs(Collections.emptyList()).build();
        }
    }

    private String mapToRemotiveCategory(String query) {
        String q = query.toLowerCase();
        if (q.contains("design") || q.contains("ux") || q.contains("ui")) return "Design";
        if (q.contains("data") || q.contains("ml") || q.contains("machine learning") || q.contains("ai")) return "Data";
        if (q.contains("devops") || q.contains("sre") || q.contains("cloud") || q.contains("infrastructure")) return "DevOps / Sysadmin";
        if (q.contains("product manager") || q.contains("product management")) return "Product";
        if (q.contains("marketing")) return "Marketing";
        if (q.contains("sales")) return "Sales";
        if (q.contains("finance") || q.contains("accounting")) return "Finance / Legal";
        return "Software Development";
    }

    private JobDto mapFromRemotive(JsonNode item) {
        String id = String.valueOf(item.path("id").asInt());
        String title = item.path("title").asText(null);
        String company = item.path("company_name").asText(null);
        String location = item.path("candidate_required_location").asText("Remote");
        String applyUrl = item.path("url").asText(null);
        String postedAt = item.path("publication_date").asText(null);
        String rawDescription = item.path("description").asText("");

        // Strip HTML tags for snippet
        String snippet = rawDescription.replaceAll("<[^>]+>", "").trim();
        if (snippet.length() > 400) snippet = snippet.substring(0, 400) + "...";

        return JobDto.builder()
                .id(UUID.randomUUID())
                .title(title)
                .companyName(company)
                .location(location)
                .descriptionSnippet(snippet)
                .applyUrl(applyUrl)
                .jobPlatformSource("Remotive")
                .platformJobId("remotive-" + id)
                .isInternal(false)
                .postedAt(postedAt)
                .build();
    }

    private JobDto mapToJobDto(JsonNode item) {
        String title = item.path("title").asText(null);
        String company = item.path("company_name").asText(null);
        String location = item.path("location").asText(null);
        String snippet = item.path("description").asText(null);
        String jobId = item.path("job_id").asText(null);
        
        JsonNode applyOptions = item.path("apply_options");
        String applyUrl = "";
        String source = "Google Jobs";
        
        if (applyOptions.isArray() && applyOptions.size() > 0) {
            JsonNode firstOption = applyOptions.get(0);
            applyUrl = firstOption.path("link").asText("");
            source = firstOption.path("title").asText("Google Jobs").replace("Apply on ", "");
        } else {
            // fallback
            applyUrl = item.path("share_link").asText("");
        }
        
        String postedAt = item.path("detected_extensions").path("posted_at").asText(null);
        if (postedAt != null && postedAt.isEmpty()) {
             postedAt = null;
        }

        return JobDto.builder()
                .id(UUID.randomUUID())
                .title(title)
                .companyName(company)
                .location(location)
                .descriptionSnippet(snippet != null && snippet.length() > 500 ? snippet.substring(0, 500) + "..." : snippet)
                .applyUrl(applyUrl)
                .jobPlatformSource(source)
                .platformJobId(jobId)
                .isInternal(false)
                .postedAt(postedAt)
                .build();
    }
}
