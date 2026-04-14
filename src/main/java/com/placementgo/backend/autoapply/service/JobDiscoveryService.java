package com.placementgo.backend.autoapply.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.placementgo.backend.autoapply.enums.ApplyMethod;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.*;

/**
 * Fetches job postings from multiple APIs in priority order:
 *  1. JSearch (RapidAPI) – broadest coverage (LinkedIn/Indeed/etc.)
 *  2. Adzuna – free tier, good UK/US coverage
 *  3. Remotive – free, remote-only fallback
 */
@Service
@Slf4j
public class JobDiscoveryService {

    @Value("${autoapply.jsearch.api-key:}")
    private String jsearchApiKey;

    @Value("${autoapply.adzuna.app-id:}")
    private String adzunaAppId;

    @Value("${autoapply.adzuna.app-key:}")
    private String adzunaAppKey;

    @Value("${autoapply.adzuna.country:us}")
    private String adzunaCountry;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper;

    public JobDiscoveryService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    // ── Public API ────────────────────────────────────────────────────────────

    public record RawJobLead(
            String externalId,
            String source,
            String title,
            String company,
            String location,
            String description,
            String applyUrl,
            String applyEmail,
            ApplyMethod applyMethod
    ) {}

    public List<RawJobLead> discover(String query, String location) {
        log.info("Discovering jobs – query='{}', location='{}'", query, location);

        if (jsearchApiKey != null && !jsearchApiKey.isBlank()) {
            List<RawJobLead> results = searchViaJSearch(query, location);
            if (!results.isEmpty()) return results;
        }

        if (adzunaAppId != null && !adzunaAppId.isBlank()) {
            List<RawJobLead> results = searchViaAdzuna(query, location);
            if (!results.isEmpty()) return results;
        }

        log.info("Falling back to Remotive for query='{}'", query);
        return searchViaRemotive(query);
    }

    // ── JSearch (RapidAPI) ────────────────────────────────────────────────────

    private List<RawJobLead> searchViaJSearch(String query, String location) {
        String fullQuery = location != null && !location.isBlank()
                ? query + " in " + location
                : query;
        // Use build().encode().toUri() to avoid double-encoding when RestTemplate
        // re-processes a String URL (spaces become %2520 instead of %20).
        URI uri = UriComponentsBuilder.fromUriString("https://jsearch.p.rapidapi.com/search")
                .queryParam("query", fullQuery)
                .queryParam("num_pages", "2")
                .queryParam("date_posted", "month")
                .build()
                .encode()
                .toUri();

        try {
            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.set("X-RapidAPI-Key", jsearchApiKey);
            headers.set("X-RapidAPI-Host", "jsearch.p.rapidapi.com");
            var entity = new org.springframework.http.HttpEntity<>(headers);
            var response = restTemplate.exchange(uri, org.springframework.http.HttpMethod.GET, entity, String.class);
            String body = response.getBody();
            log.info("JSearch raw response length: {}", body == null ? 0 : body.length());
            JsonNode root = objectMapper.readTree(body);
            JsonNode data = root.path("data");
            log.info("JSearch data isArray={}, status={}", data.isArray(), root.path("status").asText());

            List<RawJobLead> leads = new ArrayList<>();
            if (data.isArray()) {
                for (JsonNode item : data) {
                    leads.add(mapJSearchItem(item));
                }
            }
            log.info("JSearch returned {} leads for query='{}'", leads.size(), fullQuery);
            return leads;
        } catch (Exception e) {
            log.error("JSearch error: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    private RawJobLead mapJSearchItem(JsonNode item) {
        String applyLink = item.path("job_apply_link").asText(null);
        String applyEmail = item.path("job_apply_email").asText(null);
        ApplyMethod method = resolveMethod(applyLink, applyEmail, item.path("job_apply_is_direct").asBoolean(false));

        return new RawJobLead(
                item.path("job_id").asText(UUID.randomUUID().toString()),
                "JSearch",
                item.path("job_title").asText(""),
                item.path("employer_name").asText(""),
                item.path("job_city").asText("") + (item.has("job_country") ? ", " + item.path("job_country").asText("") : ""),
                item.path("job_description").asText(""),
                applyLink,
                applyEmail,
                method
        );
    }

    // ── Adzuna ────────────────────────────────────────────────────────────────

    private List<RawJobLead> searchViaAdzuna(String query, String location) {
        // Pick country based on location keyword; fall back to configured default
        String country = resolveAdzunaCountry(location);
        URI uri = UriComponentsBuilder
                .fromUriString("https://api.adzuna.com/v1/api/jobs/" + country + "/search/1")
                .queryParam("app_id", adzunaAppId)
                .queryParam("app_key", adzunaAppKey)
                .queryParam("what", query)
                .queryParam("where", location != null ? location : "")
                .queryParam("results_per_page", 20)
                .queryParam("content-type", "application/json")
                .build()
                .encode()
                .toUri();

        try {
            String body = restTemplate.getForObject(uri, String.class);
            JsonNode root = objectMapper.readTree(body);
            JsonNode results = root.path("results");

            List<RawJobLead> leads = new ArrayList<>();
            if (results.isArray()) {
                for (JsonNode item : results) {
                    leads.add(mapAdzunaItem(item));
                }
            }
            log.info("Adzuna [{}] returned {} leads", country, leads.size());
            return leads;
        } catch (Exception e) {
            log.error("Adzuna error: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    private String resolveAdzunaCountry(String location) {
        if (location == null) return adzunaCountry;
        String loc = location.toLowerCase();
        if (loc.contains("india") || loc.contains("bangalore") || loc.contains("banglore")
                || loc.contains("mumbai") || loc.contains("delhi") || loc.contains("hyderabad")
                || loc.contains("chennai") || loc.contains("pune") || loc.contains("india")) return "in";
        if (loc.contains("uk") || loc.contains("london") || loc.contains("england")) return "gb";
        if (loc.contains("australia") || loc.contains("sydney") || loc.contains("melbourne")) return "au";
        if (loc.contains("canada") || loc.contains("toronto")) return "ca";
        return adzunaCountry;
    }

    private RawJobLead mapAdzunaItem(JsonNode item) {
        String applyUrl = item.path("redirect_url").asText(null);
        return new RawJobLead(
                item.path("id").asText(UUID.randomUUID().toString()),
                "Adzuna",
                item.path("title").asText(""),
                item.path("company").path("display_name").asText(""),
                item.path("location").path("display_name").asText(""),
                item.path("description").asText(""),
                applyUrl,
                null,
                ApplyMethod.EXTERNAL_FORM
        );
    }

    // ── Remotive (free no-key fallback) ───────────────────────────────────────

    private List<RawJobLead> searchViaRemotive(String query) {
        URI uri = UriComponentsBuilder.fromUriString("https://remotive.com/api/remote-jobs")
                .queryParam("search", query)
                .queryParam("limit", 20)
                .build()
                .encode()
                .toUri();

        try {
            String body = restTemplate.getForObject(uri, String.class);
            JsonNode root = objectMapper.readTree(body);
            JsonNode jobs = root.path("jobs");

            List<RawJobLead> leads = new ArrayList<>();
            if (jobs.isArray()) {
                for (JsonNode item : jobs) {
                    leads.add(mapRemotiveItem(item));
                }
            }
            log.info("Remotive returned {} leads", leads.size());
            return leads;
        } catch (Exception e) {
            log.error("Remotive error: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    private RawJobLead mapRemotiveItem(JsonNode item) {
        String url = item.path("url").asText(null);
        return new RawJobLead(
                String.valueOf(item.path("id").asLong()),
                "Remotive",
                item.path("title").asText(""),
                item.path("company_name").asText(""),
                "Remote",
                stripHtml(item.path("description").asText("")),
                url,
                null,
                ApplyMethod.EXTERNAL_FORM
        );
    }

    // ── Utilities ─────────────────────────────────────────────────────────────

    private ApplyMethod resolveMethod(String applyUrl, String applyEmail, boolean isDirect) {
        if (applyEmail != null && !applyEmail.isBlank()) return ApplyMethod.EMAIL;
        if (isDirect && applyUrl != null) return ApplyMethod.EASY_APPLY_FORM;
        if (applyUrl != null && !applyUrl.isBlank()) return ApplyMethod.EXTERNAL_FORM;
        return ApplyMethod.UNKNOWN;
    }

    /** Very light HTML tag stripper used for Remotive descriptions */
    private String stripHtml(String html) {
        if (html == null) return "";
        return html.replaceAll("<[^>]+>", " ").replaceAll("\\s{2,}", " ").trim();
    }
}
