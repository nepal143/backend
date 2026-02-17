package com.placementgo.backend.jd_intel.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component("JdGeminiClient")
@Slf4j
public class GeminiClient {

    private final WebClient webClient;
    private final ObjectMapper mapper = new ObjectMapper();

    @Value("${gemini.model:gemini-2.5-flash}")
    private String model;

    public GeminiClient(@Value("${gemini.base-url:https://generativelanguage.googleapis.com/v1beta}") String baseUrl,
            @Value("${gemini.api-key}") String apiKey) {
        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("x-goog-api-key", apiKey)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    public String generateContent(String prompt) {
        try {

            String jsonPrompt = mapper.writeValueAsString(prompt);
            String requestBody = "{\"contents\":[{\"parts\":[{\"text\":" + jsonPrompt + "}]}]}";

            JsonNode response = webClient.post()
                    .uri("/models/{model}:generateContent", model)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();

            if (response != null) {
                JsonNode candidates = response.path("candidates");
                if (candidates.isArray() && !candidates.isEmpty()) {
                    JsonNode content = candidates.get(0).path("content");
                    JsonNode parts = content.path("parts");
                    if (parts.isArray() && !parts.isEmpty()) {
                        return parts.get(0).path("text").asText();
                    }
                }
            }
            return "";

        } catch (Exception e) {
            log.error("Gemini API call failed: {}", e.getMessage());
            return "";
        }
    }
}
