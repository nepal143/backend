package com.placementgo.backend.resume.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

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

            JsonNode response = webClient.post()
                    .uri("/models/{model}:generateContent", props.getModel())
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();

            return response
                    .path("candidates")
                    .get(0)
                    .path("content")
                    .path("parts")
                    .get(0)
                    .path("text")
                    .asText();

        } catch (Exception e) {
            throw new RuntimeException("Gemini API call failed", e);
        }
    }
}
