package com.placementgo.backend.resume.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.reactive.function.client.WebClient;


@Slf4j
@Component
public class GeminiClient {

    private final WebClient webClient;
    private final ObjectMapper mapper = new ObjectMapper();
    private final GeminiProperties props;

    public GeminiClient(GeminiProperties props) {
        this.props = props;
        this.webClient = WebClient.builder()
                .baseUrl(props.getBaseUrl())
                .defaultHeader("X-goog-api-key", props.getApiKey())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    public String generateContent(String prompt) {

        log.info("📡 Sending request to Gemini model: {}", props.getModel());
        log.info("🌍 Base URL: {}", props.getBaseUrl());

        try {

            String requestBody = """
        {
          "contents": [
            {
              "parts": [
                { "text": %s }
              ]
            }
          ]
        }
        """.formatted(mapper.writeValueAsString(prompt));

            log.info("📤 Request body size: {} chars", requestBody.length());

            JsonNode response = webClient.post()
                    .uri("/models/{model}:generateContent", props.getModel())
                    .bodyValue(requestBody)
                    .retrieve()
                    .onStatus(
                            status -> status.isError(),
                            clientResponse -> {
                                log.error("❌ Gemini returned HTTP error: {}", clientResponse.statusCode());
                                return clientResponse.bodyToMono(String.class)
                                        .map(body -> new RuntimeException("Gemini error body: " + body));
                            }
                    )
                    .bodyToMono(JsonNode.class)
                    .block();

            if (response == null) {
                throw new RuntimeException("Gemini returned null response");
            }

            log.info("📥 Gemini response received");

            return response
                    .path("candidates")
                    .get(0)
                    .path("content")
                    .path("parts")
                    .get(0)
                    .path("text")
                    .asText();

        } catch (Exception e) {

            log.error("❌ Gemini API exception:", e);

            throw new RuntimeException("Gemini API call failed", e);
        }
    }

}
