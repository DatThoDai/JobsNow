package com.JobsNow.backend.service.imp;

import com.JobsNow.backend.request.SuggestJobDraftRequest;
import com.JobsNow.backend.response.JobDraftSuggestionResponse;
import com.JobsNow.backend.response.SuggestedSkillItem;
import com.JobsNow.backend.service.AIJobDraftService;
import com.JobsNow.backend.service.OpenAIService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AIJobDraftServiceImpl implements AIJobDraftService {

    private static final int MAX_CATALOG_ITEMS = 80;

    private static final Set<String> JOB_TYPES = Set.of(
            "full_time", "part_time", "contract", "internship", "freelance");
    private static final Set<String> EDU = Set.of(
            "ANY", "HIGH_SCHOOL", "VOCATIONAL", "ASSOCIATE", "BACHELOR",
            "MASTER", "DOCTORATE", "OTHER");
    private static final Set<String> SALARY_TYPES = Set.of("RANGE", "NEGOTIABLE", "COMPETITIVE");
    private static final Set<String> CURRENCIES = Set.of("VND", "USD", "EUR", "JPY", "SGD", "KRW", "OTHER");
    private static final Set<String> APP_LANGS = Set.of(
            "VIETNAMESE", "ENGLISH", "JAPANESE", "KOREAN", "CHINESE", "ANY");
    private static final Set<String> GENDERS = Set.of("MALE", "FEMALE", "ANY");
    private static final Set<String> YOE = Set.of("0", "1", "2", "3", "1-3", "3-5", "5+");

    private final OpenAIService openAIService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public JobDraftSuggestionResponse suggest(SuggestJobDraftRequest request) {
        String locale = Optional.ofNullable(request.getLocale()).filter(s -> !s.isBlank()).orElse("vi");
        String languageRule = "vi".equalsIgnoreCase(locale)
                ? "Write all text fields in Vietnamese (professional HR tone, similar to TopCV/VietnamWorks job posts)."
                : "Write all text fields in English.";

        String categoryCatalog = formatCatalog("ALLOWED_CATEGORIES", request.getCategoryNames());
        String skillCatalog = formatCatalog("PREFERRED_SKILLS", request.getSkillNames());
        String majorCatalog = formatCatalog("PREFERRED_MAJORS", request.getMajorNames());

        String system = """
            You are an expert Vietnam HR job posting writer for recruiters.
            Given a job title and optional catalogs, output ONE JSON object only (no markdown fences).
            %s

            ENUMS (use exact values):
            jobType: full_time|part_time|contract|internship|freelance
            educationLevel: ANY|HIGH_SCHOOL|VOCATIONAL|ASSOCIATE|BACHELOR|MASTER|DOCTORATE|OTHER
            salaryType: RANGE|NEGOTIABLE|COMPETITIVE
            salaryCurrency: VND|USD|EUR|JPY|SGD|KRW|OTHER
            yearsOfExperience: 0|1|2|3|1-3|3-5|5+
            applicationLanguage: VIETNAMESE|ENGLISH|JAPANESE|KOREAN|CHINESE|ANY
            genderRequirement: MALE|FEMALE|ANY

            CONTENT RULES:
            - description: 150-220 words. Role summary, main responsibilities, team/context. Plain text paragraphs separated by blank lines.
            - requirements: 12-18 bullet lines, each starting with "- ". Structure like real job boards:
              * Education & major expectations
              * Years of experience & domain
              * Hard/technical skills (specific tools, frameworks)
              * Soft skills & attitude
              * Nice-to-have / preferred qualifications
              * Key duties the hire will perform
              Do NOT repeat the description verbatim.
            - benefits: 10-15 bullet lines starting with "- ". Cover: salary/bonus, insurance, leave, training, career growth, environment, equipment, hours, location perks, team activities.
            - For VND RANGE: realistic monthly salaryMin/salaryMax for Vietnam market.
            - suggestedCategoryName: MUST be exactly one string from ALLOWED_CATEGORIES when that list is provided; otherwise best-fit category name in Vietnamese.
            - suggestedSkills: array of 4-8 objects { "name": "<skill>", "level": "<e.g. Junior|Intermediate|Senior|3+ years|Fluent>", "isRequired": true|false }.
              Prefer skill names from PREFERRED_SKILLS when provided. level must be short (max 30 chars).
            - suggestedMajorNames: 0-3 major names; prefer PREFERRED_MAJORS when provided.

            %s
            %s
            %s

            Do not invent company names.
            JSON keys: description, requirements, benefits, location, jobType, yearsOfExperience,
            educationLevel, salaryType, salaryCurrency, salaryMin, salaryMax, applicationLanguage,
            genderRequirement, suggestedCategoryName, suggestedSkills, suggestedMajorNames.
            """.formatted(languageRule, categoryCatalog, skillCatalog, majorCatalog);

        String user = "Job title: \"" + request.getTitle().trim() + "\"";

        String raw = openAIService.chatCompletion(system, user);
        JsonNode root = parseJson(raw);

        String categoryName = text(root, "suggestedCategoryName");
        categoryName = pickFromCatalog(categoryName, request.getCategoryNames());

        return JobDraftSuggestionResponse.builder()
                .description(text(root, "description"))
                .requirements(text(root, "requirements"))
                .benefits(text(root, "benefits"))
                .location(text(root, "location"))
                .jobType(jobTypeOrNull(root))
                .yearsOfExperience(enumOrNull(root, "yearsOfExperience", YOE))
                .educationLevel(enumOrNull(root, "educationLevel", EDU))
                .salaryType(enumOrNull(root, "salaryType", SALARY_TYPES))
                .salaryCurrency(enumOrNull(root, "salaryCurrency", CURRENCIES))
                .salaryMin(number(root, "salaryMin"))
                .salaryMax(number(root, "salaryMax"))
                .applicationLanguage(enumOrNull(root, "applicationLanguage", APP_LANGS))
                .genderRequirement(enumOrNull(root, "genderRequirement", GENDERS))
                .suggestedCategoryName(categoryName)
                .suggestedSkills(parseSuggestedSkills(root))
                .suggestedMajorNames(stringList(root, "suggestedMajorNames"))
                .build();
    }

    private static String formatCatalog(String label, List<String> items) {
        if (items == null || items.isEmpty()) {
            return label + ": (not provided — use your best judgment)";
        }
        List<String> trimmed = items.stream()
                .filter(s -> s != null && !s.isBlank())
                .map(String::trim)
                .limit(MAX_CATALOG_ITEMS)
                .collect(Collectors.toList());
        if (trimmed.isEmpty()) {
            return label + ": (not provided — use your best judgment)";
        }
        return label + ":\n" + String.join("\n", trimmed.stream().map(s -> "- " + s).toList());
    }

    private static String pickFromCatalog(String aiPick, List<String> catalog) {
        if (aiPick == null || aiPick.isBlank() || catalog == null || catalog.isEmpty()) {
            return aiPick;
        }
        String normPick = normalize(aiPick);
        for (String c : catalog) {
            if (c == null || c.isBlank()) continue;
            String normC = normalize(c);
            if (normC.equals(normPick) || normC.contains(normPick) || normPick.contains(normC)) {
                return c.trim();
            }
        }
        return aiPick.trim();
    }

    private static String normalize(String s) {
        return s.trim().toLowerCase()
                .replaceAll("\\s+", " ");
    }

    private List<SuggestedSkillItem> parseSuggestedSkills(JsonNode root) {
        List<SuggestedSkillItem> out = new ArrayList<>();
        if (root.has("suggestedSkills") && root.get("suggestedSkills").isArray()) {
            root.get("suggestedSkills").forEach(node -> {
                String name = text(node, "name");
                if (name == null) return;
                String level = text(node, "level");
                boolean required = !node.has("isRequired") || node.get("isRequired").asBoolean(true);
                out.add(SuggestedSkillItem.builder()
                        .name(name)
                        .level(level != null ? level : "")
                        .isRequired(required)
                        .build());
            });
        }
        if (out.isEmpty() && root.has("suggestedSkillNames") && root.get("suggestedSkillNames").isArray()) {
            root.get("suggestedSkillNames").forEach(node -> {
                String name = node.asText("").trim();
                if (!name.isEmpty()) {
                    out.add(SuggestedSkillItem.builder()
                            .name(name)
                            .level("")
                            .isRequired(true)
                            .build());
                }
            });
        }
        return out;
    }

    private JsonNode parseJson(String raw) {
        try {
            String clean = raw.replaceAll("```json\\s*", "").replaceAll("```\\s*", "").trim();
            return objectMapper.readTree(clean);
        } catch (Exception e) {
            log.error("Parse job draft AI failed: {}", raw);
            throw new RuntimeException("AI response invalid");
        }
    }

    private static String text(JsonNode n, String key) {
        if (!n.has(key) || n.get(key).isNull()) {
            return null;
        }
        String v = n.get(key).asText().trim();
        return v.isEmpty() ? null : v;
    }

    private static String jobTypeOrNull(JsonNode n) {
        String v = text(n, "jobType");
        if (v == null) {
            return null;
        }
        String norm = v.trim().toLowerCase().replace(' ', '_').replace('-', '_');
        return JOB_TYPES.contains(norm) ? norm : null;
    }

    private static String enumOrNull(JsonNode n, String key, Set<String> allowed) {
        String v = text(n, key);
        if (v == null) {
            return null;
        }
        String upper = v.trim().toUpperCase();
        return allowed.contains(upper) ? upper : null;
    }

    private static Double number(JsonNode n, String key) {
        if (!n.has(key) || n.get(key).isNull()) {
            return null;
        }
        double d = n.get(key).asDouble();
        return d >= 0 ? d : null;
    }

    private static List<String> stringList(JsonNode n, String key) {
        if (!n.has(key) || !n.get(key).isArray()) {
            return List.of();
        }
        List<String> out = new ArrayList<>();
        n.get(key).forEach(item -> {
            String s = item.asText("").trim();
            if (!s.isEmpty()) {
                out.add(s);
            }
        });
        return out;
    }
}
