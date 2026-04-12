package com.JobsNow.backend.service;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.util.List;
import java.util.Map;
@Slf4j
@Service
public class OpenAIService {
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();
    @Value("${openai.base-url}")
    private String baseUrl;
    @Value("${openai.model}")
    private String model;
    @Value("${openai.max-tokens}")
    private int maxTokens;
    public OpenAIService(@Qualifier("openAIRestTemplate") RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public String chatCompletion(String systemPrompt, String userPrompt) {
        String url = baseUrl + "/chat/completions";
        Map<String, Object> requestBody = Map.of(
                "model", model,
                "max_tokens", maxTokens,
                "temperature", 0.3,
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userPrompt)
                )
        );
        try {
            ResponseEntity<String> response = restTemplate.postForEntity(url, requestBody, String.class);
            JsonNode root = objectMapper.readTree(response.getBody());
            String content = root.path("choices").get(0).path("message").path("content").asText();
            log.info("OpenAI response received, tokens used: {}",
                    root.path("usage").path("total_tokens").asInt());
            return content;
        } catch (Exception e) {
            log.error("OpenAI API call failed: {}", e.getMessage(), e);
            throw new RuntimeException("AI service unavailable: " + e.getMessage());
        }
    }
}