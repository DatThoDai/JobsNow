package com.JobsNow.backend.service.imp;

import com.JobsNow.backend.dto.AiSearchRequest;
import com.JobsNow.backend.dto.AiSearchResponse;
import com.JobsNow.backend.entity.*;
import com.JobsNow.backend.repositories.ApplicationRepository;
import com.JobsNow.backend.repositories.JobSeekerProfileRepository;
import com.JobsNow.backend.service.CandidateRAGService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class CandidateRAGServiceImpl implements CandidateRAGService {

    private final JobSeekerProfileRepository jobSeekerProfileRepository;
    private final ApplicationRepository applicationRepository;
    private final RestTemplate openAIRestTemplate;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    @Value("${openai.base-url}")
    private String baseUrl;

    @Value("${openai.model}")
    private String chatModel;

    @Value("${openai.max-tokens}")
    private int maxTokens;

    private static final String EMBEDDING_MODEL = "text-embedding-3-small";
    private static final String EMBEDDING_KEY_PREFIX = "rag:profile:embedding:";
    private static final String PROFILE_TEXT_KEY_PREFIX = "rag:profile:text:";
    private static final String PROFILE_META_KEY_PREFIX = "rag:profile:meta:";
    private static final String ALL_PROFILE_IDS_KEY = "rag:profile:ids";

    public CandidateRAGServiceImpl(
            JobSeekerProfileRepository jobSeekerProfileRepository,
            ApplicationRepository applicationRepository,
            @Qualifier("openAIRestTemplate") RestTemplate openAIRestTemplate,
            StringRedisTemplate stringRedisTemplate) {
        this.jobSeekerProfileRepository = jobSeekerProfileRepository;
        this.applicationRepository = applicationRepository;
        this.openAIRestTemplate = openAIRestTemplate;
        this.stringRedisTemplate = stringRedisTemplate;
        this.objectMapper = new ObjectMapper();
    }

    @Override
    @Transactional(readOnly = true)
    public AiSearchResponse searchCandidates(AiSearchRequest request, Integer companyId) {
        String query = request.getQuery();
        int topK = request.getTopK() != null ? request.getTopK() : 5;

        float[] queryEmbedding = getEmbedding(query);
        if (queryEmbedding == null) {
            return AiSearchResponse.builder()
                    .answer("Lỗi khi tạo embedding cho câu hỏi. Vui lòng thử lại.")
                    .candidates(Collections.emptyList())
                    .build();
        }

        Set<String> allProfileIds = stringRedisTemplate.opsForSet().members(ALL_PROFILE_IDS_KEY);
        if (allProfileIds == null || allProfileIds.isEmpty()) {
            return AiSearchResponse.builder()
                    .answer("Chưa có dữ liệu ứng viên nào được đánh chỉ mục. Vui lòng bấm nút \"Đánh chỉ mục\" trước.")
                    .candidates(Collections.emptyList())
                    .build();
        }

        Set<String> validProfileIds = new HashSet<>(allProfileIds);
        if (request.getJobId() != null) {
            List<Application> apps = applicationRepository.findByJob_JobId(request.getJobId());
            Set<String> applicantProfileIds = apps.stream()
                    .map(a -> String.valueOf(a.getJobSeekerProfile().getProfileId()))
                    .collect(Collectors.toSet());
            validProfileIds.retainAll(applicantProfileIds);
            
            if (validProfileIds.isEmpty()) {
                return AiSearchResponse.builder()
                        .answer("Chưa có ứng viên nào ứng tuyển vào công việc này.")
                        .candidates(Collections.emptyList())
                        .build();
            }
        }

        List<Map.Entry<Integer, Double>> scoredProfiles = new ArrayList<>();
        for (String idStr : validProfileIds) {
            Integer profileId = Integer.parseInt(idStr);
            String embeddingJson = stringRedisTemplate.opsForValue().get(EMBEDDING_KEY_PREFIX + profileId);
            if (embeddingJson == null) continue;

            try {
                float[] profileEmbedding = objectMapper.readValue(embeddingJson, float[].class);
                double similarity = cosineSimilarity(queryEmbedding, profileEmbedding);
                scoredProfiles.add(Map.entry(profileId, similarity));
            } catch (Exception e) {
                log.warn("Lỗi parse embedding profile {}: {}", profileId, e.getMessage());
            }
        }

        scoredProfiles.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));
        List<Map.Entry<Integer, Double>> topCandidates = scoredProfiles.stream()
                .limit(topK)
                .toList();

        if (topCandidates.isEmpty()) {
            return AiSearchResponse.builder()
                    .answer("Không tìm thấy ứng viên phù hợp với yêu cầu của bạn.")
                    .candidates(Collections.emptyList())
                    .build();
        }

        StringBuilder contextBuilder = new StringBuilder();
        List<AiSearchResponse.CandidateMatch> candidateMatches = new ArrayList<>();

        for (int i = 0; i < topCandidates.size(); i++) {
            Map.Entry<Integer, Double> entry = topCandidates.get(i);
            Integer profileId = entry.getKey();
            Double score = entry.getValue();

            String profileText = stringRedisTemplate.opsForValue().get(PROFILE_TEXT_KEY_PREFIX + profileId);
            String metaJson = stringRedisTemplate.opsForValue().get(PROFILE_META_KEY_PREFIX + profileId);

            if (profileText == null || metaJson == null) continue;

            contextBuilder.append("--- Ứng viên ").append(i + 1).append(" (ID: ").append(profileId)
                    .append(", Điểm tương đồng: ").append(String.format("%.2f", score)).append(") ---\n");
            contextBuilder.append(profileText).append("\n\n");

            try {
                Map<String, Object> meta = objectMapper.readValue(metaJson, new TypeReference<>() {});
                List<String> skills = meta.get("skills") != null
                        ? objectMapper.convertValue(meta.get("skills"), new TypeReference<>() {})
                        : Collections.emptyList();

                candidateMatches.add(AiSearchResponse.CandidateMatch.builder()
                        .profileId(profileId)
                        .fullName((String) meta.get("fullName"))
                        .email((String) meta.get("email"))
                        .title((String) meta.get("title"))
                        .avatarUrl((String) meta.get("avatarUrl"))
                        .skills(skills)
                        .experience((String) meta.get("experience"))
                        .relevanceScore(score)
                        .build());
            } catch (Exception e) {
                log.warn("Lỗi parse meta profile {}: {}", profileId, e.getMessage());
            }
        }

        String systemPrompt = """
                Bạn là trợ lý AI tuyển dụng chuyên nghiệp của nền tảng JobsNow.
                Nhiệm vụ: Dựa vào thông tin hồ sơ ứng viên được cung cấp bên dưới, hãy phân tích và trả lời câu hỏi của nhà tuyển dụng.
                
                QUY TẮC QUAN TRỌNG:
                1. Chỉ trả lời dựa trên dữ liệu được cung cấp. NẾU KHÔNG CÓ ỨNG VIÊN NÀO PHÙ HỢP HOẶC KHÔNG CÓ DỮ LIỆU, HÃY NÓI RÕ VÀ TRẢ VỀ matchReasons LÀ OBJECT RỖNG {}.
                2. Với mỗi ứng viên phù hợp, giải thích ngắn gọn LÝ DO vì sao họ khớp với yêu cầu. Yêu cầu có thể là Tên, Kỹ năng, Kinh nghiệm, Học vấn, Chứng chỉ, Địa chỉ, v.v.
                3. Xử lý tên và từ khóa Tiếng Việt một cách linh hoạt (Ví dụ: "Đạt" và "Dat", "Hồ Chí Minh" và "HCM" được xem là như nhau).
                4. Trả lời bằng tiếng Việt, ngắn gọn, chuyên nghiệp.
                5. CHỈ đưa ID của ứng viên vào matchReasons nếu họ THỰC SỰ đáp ứng được yêu cầu của nhà tuyển dụng.
                6. Trả về kết quả CHỈ LÀ JSON với format sau:
                {
                  "answer": "Câu trả lời tổng quát (vd: Tìm thấy 2 ứng viên phù hợp... hoặc Không có ứng viên nào phù hợp)",
                  "matchReasons": {
                    "<profileId>": "Lý do ứng viên này phù hợp..."
                  }
                }
                
                DỮ LIỆU ỨNG VIÊN:
                """ + contextBuilder.toString();

        try {
            String aiResponse = callChatCompletion(systemPrompt, query);

            String cleanResponse = aiResponse;
            if (cleanResponse.startsWith("```json")) {
                cleanResponse = cleanResponse.substring(7);
            }
            if (cleanResponse.startsWith("```")) {
                cleanResponse = cleanResponse.substring(3);
            }
            if (cleanResponse.endsWith("```")) {
                cleanResponse = cleanResponse.substring(0, cleanResponse.length() - 3);
            }
            cleanResponse = cleanResponse.trim();

            Map<String, Object> parsed = objectMapper.readValue(cleanResponse, new TypeReference<>() {});
            String answer = (String) parsed.get("answer");

            @SuppressWarnings("unchecked")
            Map<String, String> matchReasons = parsed.get("matchReasons") != null
                    ? (Map<String, String>) parsed.get("matchReasons")
                    : Collections.emptyMap();

            List<AiSearchResponse.CandidateMatch> finalMatches = new ArrayList<>();
            for (AiSearchResponse.CandidateMatch candidate : candidateMatches) {
                String reason = matchReasons.get(String.valueOf(candidate.getProfileId()));
                if (reason != null && !reason.trim().isEmpty()) {
                    candidate.setMatchReason(reason);
                    finalMatches.add(candidate);
                }
            }

            return AiSearchResponse.builder()
                    .answer(answer)
                    .candidates(finalMatches)
                    .build();

        } catch (Exception e) {
            log.error("Lỗi gọi AI: {}", e.getMessage(), e);
            return AiSearchResponse.builder()
                    .answer("Tìm thấy " + candidateMatches.size() + " ứng viên tiềm năng dựa trên yêu cầu của bạn.")
                    .candidates(candidateMatches)
                    .build();
        }
    }

    @Override
    @Transactional(readOnly = true)
    @EventListener(ApplicationReadyEvent.class)
    public void indexAllProfiles() {
        List<JobSeekerProfile> profiles = jobSeekerProfileRepository.findAll();
        log.info("Bắt đầu đánh chỉ mục {} hồ sơ ứng viên...", profiles.size());

        int indexed = 0;
        for (JobSeekerProfile profile : profiles) {
            try {
                indexSingleProfile(profile);
                indexed++;
            } catch (Exception e) {
                log.warn("Lỗi đánh chỉ mục profile {}: {}", profile.getProfileId(), e.getMessage());
            }
        }
        log.info("Hoàn tất đánh chỉ mục: {}/{} hồ sơ", indexed, profiles.size());
    }

    @Override
    @Transactional(readOnly = true)
    public void indexProfile(Integer profileId) {
        JobSeekerProfile profile = jobSeekerProfileRepository.findById(profileId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy hồ sơ với ID: " + profileId));
        indexSingleProfile(profile);
        log.info("Đã đánh chỉ mục hồ sơ ID: {}", profileId);
    }

    private void indexSingleProfile(JobSeekerProfile profile) {
        String profileText = buildProfileText(profile);
        if (profileText.isBlank()) return;

        float[] embedding = getEmbedding(profileText);
        if (embedding == null) {
            log.warn("Không thể tạo embedding cho profile {}", profile.getProfileId());
            return;
        }

        Integer profileId = profile.getProfileId();
        try {
            stringRedisTemplate.opsForValue().set(EMBEDDING_KEY_PREFIX + profileId, objectMapper.writeValueAsString(embedding));
            stringRedisTemplate.opsForValue().set(PROFILE_TEXT_KEY_PREFIX + profileId, profileText);

            Map<String, Object> meta = new HashMap<>();
            User user = profile.getUser();
            meta.put("fullName", user != null ? user.getFullName() : "");
            meta.put("email", user != null ? user.getEmail() : "");
            meta.put("title", profile.getTitle() != null ? profile.getTitle() : "");
            meta.put("avatarUrl", profile.getAvatarUrl() != null ? profile.getAvatarUrl() : "");

            List<String> skillNames = new ArrayList<>();
            if (profile.getJobSeekerSkills() != null) {
                for (JobSeekerSkill js : profile.getJobSeekerSkills()) {
                    if (js.getSkill() != null && js.getSkill().getSkillName() != null) {
                        skillNames.add(js.getSkill().getSkillName());
                    }
                }
            }
            meta.put("skills", skillNames);

            String expSummary = buildExperienceSummary(profile);
            meta.put("experience", expSummary);

            stringRedisTemplate.opsForValue().set(PROFILE_META_KEY_PREFIX + profileId, objectMapper.writeValueAsString(meta));
            stringRedisTemplate.opsForSet().add(ALL_PROFILE_IDS_KEY, String.valueOf(profileId));
        } catch (Exception e) {
            log.error("Lỗi lưu embedding vào Redis cho profile {}: {}", profileId, e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private float[] getEmbedding(String text) {
        try {
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", EMBEDDING_MODEL);
            requestBody.put("input", text);

            ResponseEntity<Map> response = openAIRestTemplate.postForEntity(
                    baseUrl + "/embeddings",
                    requestBody,
                    Map.class
            );

            Map<String, Object> body = response.getBody();
            if (body == null) return null;

            List<Map<String, Object>> data = (List<Map<String, Object>>) body.get("data");
            if (data == null || data.isEmpty()) return null;

            List<Number> embeddingList = (List<Number>) data.get(0).get("embedding");
            if (embeddingList == null) return null;

            float[] embedding = new float[embeddingList.size()];
            for (int i = 0; i < embeddingList.size(); i++) {
                embedding[i] = embeddingList.get(i).floatValue();
            }
            return embedding;
        } catch (Exception e) {
            log.error("Lỗi gọi OpenAI Embedding API: {}", e.getMessage());
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private String callChatCompletion(String systemPrompt, String userMessage) {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", chatModel);
        requestBody.put("max_tokens", maxTokens);
        requestBody.put("temperature", 0.3);

        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", systemPrompt));
        messages.add(Map.of("role", "user", "content", userMessage));
        requestBody.put("messages", messages);

        ResponseEntity<Map> response = openAIRestTemplate.postForEntity(
                baseUrl + "/chat/completions",
                requestBody,
                Map.class
        );

        Map<String, Object> body = response.getBody();
        if (body == null) throw new RuntimeException("OpenAI trả về response rỗng");

        List<Map<String, Object>> choices = (List<Map<String, Object>>) body.get("choices");
        if (choices == null || choices.isEmpty()) throw new RuntimeException("OpenAI không trả về choices");

        Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
        return (String) message.get("content");
    }

    private String buildProfileText(JobSeekerProfile profile) {
        StringBuilder sb = new StringBuilder();
        User user = profile.getUser();

        if (user != null && user.getFullName() != null) {
            sb.append("Họ tên: ").append(user.getFullName()).append("\n");
        }
        if (profile.getTitle() != null) {
            sb.append("Vị trí mong muốn: ").append(profile.getTitle()).append("\n");
        }
        if (profile.getBio() != null) {
            sb.append("Giới thiệu: ").append(profile.getBio()).append("\n");
        }
        if (profile.getAddress() != null) {
            sb.append("Địa chỉ: ").append(profile.getAddress()).append("\n");
        }

        if (profile.getJobSeekerSkills() != null && !profile.getJobSeekerSkills().isEmpty()) {
            sb.append("Kỹ năng: ");
            List<String> skills = profile.getJobSeekerSkills().stream()
                    .filter(js -> js.getSkill() != null && js.getSkill().getSkillName() != null)
                    .map(js -> {
                        String s = js.getSkill().getSkillName();
                        if (js.getLevel() != null) s += " (" + js.getLevel() + ")";
                        if (js.getYearsOfExperience() != null) s += " - " + js.getYearsOfExperience() + " năm";
                        return s;
                    })
                    .toList();
            sb.append(String.join(", ", skills)).append("\n");
        }

        if (profile.getWorkExperiences() != null && !profile.getWorkExperiences().isEmpty()) {
            sb.append("Kinh nghiệm làm việc:\n");
            for (WorkExperience exp : profile.getWorkExperiences()) {
                sb.append("  - ").append(exp.getTitle() != null ? exp.getTitle() : "N/A");
                if (exp.getLevel() != null) sb.append(" (").append(exp.getLevel()).append(")");
                if (exp.getStartDate() != null) {
                    sb.append(" | ").append(exp.getStartDate());
                    sb.append(" - ").append(exp.getEndDate() != null ? exp.getEndDate() : "Hiện tại");
                }
                if (exp.getDescription() != null) sb.append("\n    ").append(exp.getDescription());
                sb.append("\n");
            }
        }

        if (profile.getEducations() != null && !profile.getEducations().isEmpty()) {
            sb.append("Học vấn:\n");
            for (Education edu : profile.getEducations()) {
                sb.append("  - ").append(edu.getTitle() != null ? edu.getTitle() : "N/A");
                if (edu.getEducationLevel() != null) sb.append(" (").append(edu.getEducationLevel()).append(")");
                if (edu.getMajor() != null) sb.append(" | Ngành: ").append(edu.getMajor().getName());
                sb.append("\n");
            }
        }

        if (profile.getProjects() != null && !profile.getProjects().isEmpty()) {
            sb.append("Dự án:\n");
            for (Project prj : profile.getProjects()) {
                sb.append("  - ").append(prj.getTitle() != null ? prj.getTitle() : "N/A");
                if (prj.getDescription() != null) sb.append(": ").append(prj.getDescription());
                sb.append("\n");
            }
        }

        if (profile.getCertificates() != null && !profile.getCertificates().isEmpty()) {
            sb.append("Chứng chỉ:\n");
            for (Certificate cert : profile.getCertificates()) {
                sb.append("  - ").append(cert.getTitle() != null ? cert.getTitle() : "N/A");
                if (cert.getDescription() != null) sb.append(": ").append(cert.getDescription());
                sb.append("\n");
            }
        }

        if (profile.getResumes() != null && !profile.getResumes().isEmpty()) {
            for (Resume resume : profile.getResumes()) {
                if (resume.getExtractedText() != null && !resume.getExtractedText().isBlank()) {
                    sb.append("Nội dung CV tải lên:\n").append(resume.getExtractedText()).append("\n");
                }
            }
        }

        return sb.toString().trim();
    }

    private String buildExperienceSummary(JobSeekerProfile profile) {
        if (profile.getWorkExperiences() == null || profile.getWorkExperiences().isEmpty()) {
            return "Chưa có kinh nghiệm";
        }
        return profile.getWorkExperiences().stream()
                .filter(e -> e.getTitle() != null)
                .map(e -> {
                    String s = e.getTitle();
                    if (e.getLevel() != null) s += " (" + e.getLevel() + ")";
                    return s;
                })
                .collect(Collectors.joining(", "));
    }

    private double cosineSimilarity(float[] a, float[] b) {
        if (a.length != b.length) return 0.0;
        double dotProduct = 0.0, normA = 0.0, normB = 0.0;
        for (int i = 0; i < a.length; i++) {
            dotProduct += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }
        double denominator = Math.sqrt(normA) * Math.sqrt(normB);
        return denominator == 0 ? 0.0 : dotProduct / denominator;
    }
}
