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

        if (apiKey == null || apiKey.isEmpty()) {
            log.warn("Search API Key is missing. Returning empty premium job results.");
            return JobSearchResponse.builder().query(query).totalResults(0).jobs(Collections.emptyList()).build();
        }

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
                log.warn("No 'jobs_results' found in SerpApi response.");
                return JobSearchResponse.builder().query(query).totalResults(0).jobs(Collections.emptyList()).build();
            }

            JsonNode jobsNode = rootNode.path("jobs_results");
            List<JobDto> parsedJobs = new ArrayList<>();
            
            if (jobsNode.isArray()) {
                for (JsonNode item : jobsNode) {
                    parsedJobs.add(mapToJobDto(item));
                }
            }

            return JobSearchResponse.builder()
                    .query(query)
                    .totalResults(parsedJobs.size())
                    .jobs(parsedJobs)
                    .build();

        } catch (Exception e) {
            log.error("Error during premium job search: {}", e.getMessage(), e);
            return JobSearchResponse.builder().query(query).totalResults(0).jobs(Collections.emptyList()).build();
        }
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
