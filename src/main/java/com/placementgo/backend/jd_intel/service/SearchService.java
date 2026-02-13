package com.placementgo.backend.jd_intel.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class SearchService {

    @Value("${jd-intel.search-api-key}")
    private String apiKey;

    @Value("${jd-intel.search-engine-id}")
    private String searchEngineId;

    private final RestTemplate restTemplate = new RestTemplate();

    public List<String> search(String query) {
        log.info("Searching for: {}", query);

        if (apiKey == null || apiKey.isEmpty()) {
            log.warn("Search API Key is missing. Returning empty results.");
            return new ArrayList<>();
        }

        String url = UriComponentsBuilder.fromHttpUrl("https://customsearch.googleapis.com/customsearch/v1")
                .queryParam("key", apiKey)
                .queryParam("cx", searchEngineId)
                .queryParam("q", query)
                .toUriString();

        try {
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            List<Map<String, Object>> items = (List<Map<String, Object>>) response.get("items");

            if (items == null)
                return new ArrayList<>();

            return items.stream()
                    .map(item -> (String) item.get("link"))
                    .toList();
        } catch (Exception e) {
            log.error("Error during search: {}", e.getMessage());
            return new ArrayList<>();
        }
    }
}
