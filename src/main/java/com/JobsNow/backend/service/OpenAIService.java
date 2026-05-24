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

    public String extractTextFromImage(String imageUrl) {
        String url = baseUrl + "/chat/completions";
        String promptText = "Hãy đọc hình ảnh chứng chỉ/bằng cấp này và trích xuất thông tin thật ngắn gọn (dưới 100 từ) gồm: Loại chứng chỉ/bằng cấp, Điểm số/Xếp loại, và các kỹ năng nổi bật liên quan để phục vụ đánh giá hồ sơ.";
        List<Map<String, Object>> contentList = List.of(
            Map.of("type", "text", "text", promptText),
            Map.of("type", "image_url", "image_url", Map.of("url", imageUrl))
        );
        List<Map<String, Object>> messages = List.of(
            Map.of("role", "user", "content", contentList)
        );
        String visionModel = model.contains("gpt-4o") ? model : "gpt-4o-mini";
        Map<String, Object> requestBody = Map.of(
            "model", visionModel,
            "max_tokens", 300,
            "temperature", 0.1,
            "messages", messages
        );
        try {
            ResponseEntity<String> response = restTemplate.postForEntity(url, requestBody, String.class);
            JsonNode root = objectMapper.readTree(response.getBody());
            String content = root.path("choices").get(0).path("message").path("content").asText();
            log.info("OpenAI Vision response received successfully.");
            return content;
        } catch (Exception e) {
            log.error("OpenAI Vision API call failed for image: {}. Error: {}", imageUrl, e.getMessage(), e);
            return "[Không thể phân tích văn bản từ hình ảnh chứng chỉ]";
        }
    }
}