package com.JobsNow.backend.service.imp;

import com.JobsNow.backend.entity.*;
import com.JobsNow.backend.entity.enums.EducationLevel;
import com.JobsNow.backend.exception.BadRequestException;
import com.JobsNow.backend.exception.NotFoundException;
import com.JobsNow.backend.repositories.JobRepository;
import com.JobsNow.backend.repositories.JobSeekerProfileRepository;
import com.JobsNow.backend.repositories.ResumeRepository;
import com.JobsNow.backend.repositories.JobMatchScoreRepository;
import com.JobsNow.backend.request.JobMatchRequest;
import com.JobsNow.backend.response.JobMatchResponse;
import com.JobsNow.backend.service.JobMatchService;
import com.JobsNow.backend.service.OpenAIService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.Period;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class JobMatchServiceImpl implements JobMatchService {
    private final JobRepository jobRepository;
    private final JobSeekerProfileRepository jobSeekerProfileRepository;
    private final ResumeRepository resumeRepository;
    private final JobMatchScoreRepository jobMatchScoreRepository;
    private final OpenAIService openAIService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Transactional(readOnly = true)
    @Override
    public JobMatchResponse calculateMatch(JobMatchRequest request) {
        if (request.getJobId() == null) {
            throw new BadRequestException("jobId is required");
        }
        if (request.getProfileId() == null && request.getResumeId() == null) {
            throw new BadRequestException("Either profileId or resumeId is required");
        }

        Job job = jobRepository.findById(request.getJobId())
                .orElseThrow(() -> new NotFoundException("Job not found"));

        Set<String> requiredSkills = new HashSet<>();
        Set<String> allJobSkills = new HashSet<>();

        if (job.getJobSkills() != null) {
            for (JobSkill js : job.getJobSkills()) {
                String skillName = js.getSkill().getSkillName().toLowerCase().trim();
                allJobSkills.add(skillName);
                if (Boolean.TRUE.equals(js.getIsRequired())) {
                    requiredSkills.add(skillName);
                }
            }
        }

        final Set<String> candidateSkills = new HashSet<>();
        String cvText = "";
        JobSeekerProfile candidateProfile = null;
        Resume candidateResume = null;

        if (request.getProfileId() != null) {
            candidateProfile = jobSeekerProfileRepository.findById(request.getProfileId())
                    .orElseThrow(() -> new NotFoundException("Profile not found"));
            if (candidateProfile.getJobSeekerSkills() != null) {
                candidateSkills.addAll(candidateProfile.getJobSeekerSkills().stream()
                        .map(js -> js.getSkill().getSkillName().toLowerCase().trim())
                        .collect(Collectors.toSet()));
            }
            cvText = buildProfileText(candidateProfile);
        }

        if (request.getResumeId() != null) {
            candidateResume = resumeRepository.findById(request.getResumeId())
                    .orElseThrow(() -> new NotFoundException("Resume not found"));
            if (candidateResume.getResumeSkills() != null) {
                candidateSkills.addAll(candidateResume.getResumeSkills().stream()
                        .filter(rs -> rs.getSkill() != null && rs.getSkill().getSkillName() != null)
                        .map(rs -> rs.getSkill().getSkillName().toLowerCase().trim())
                        .collect(Collectors.toSet()));
            }
            if (candidateResume.getExtractedText() != null && !candidateResume.getExtractedText().isBlank()) {
                cvText = candidateResume.getExtractedText();
                candidateSkills.addAll(extractSkillsFromText(candidateResume.getExtractedText(), allJobSkills));
            }
            if (candidateProfile == null) {
                candidateProfile = candidateResume.getJobSeekerProfile();
                if (candidateProfile != null && candidateProfile.getJobSeekerSkills() != null) {
                    candidateSkills.addAll(candidateProfile.getJobSeekerSkills().stream()
                            .map(js -> js.getSkill().getSkillName().toLowerCase().trim())
                            .collect(Collectors.toSet()));
                }
            }
        }

        List<String> matchedSkills = new ArrayList<>();
        List<String> missingSkills = new ArrayList<>();

        for (String jobSkill : allJobSkills) {
            if (candidateSkills.contains(jobSkill)) {
                matchedSkills.add(jobSkill);
            } else {
                missingSkills.add(jobSkill);
            }
        }

        int skillScore = allJobSkills.isEmpty() ? 50 :
                (int) ((double) matchedSkills.size() / allJobSkills.size() * 100);

        if (!requiredSkills.isEmpty()) {
            long missingRequired = requiredSkills.stream()
                    .filter(s -> !candidateSkills.contains(s)).count();
            double requiredPenalty = (double) missingRequired / requiredSkills.size() * 30;
            skillScore = Math.max(0, skillScore - (int) requiredPenalty);
        }

        int educationScore = calculateEducationScore(candidateProfile, candidateResume, job);
        int experienceScore = calculateExperienceScore(candidateProfile, candidateResume, job);
        int ruleBasedScore = (int) (skillScore * 0.6 + educationScore * 0.2 + experienceScore * 0.2);

        int aiScore = 50;
        String aiFeedback = "";
        List<String> recommendations = new ArrayList<>();

        if (!cvText.isBlank()) {
            try {
                var aiResult = getAIMatchScore(cvText, job);
                aiScore = aiResult.get("score") != null ? (int) aiResult.get("score") : 50;
                aiFeedback = (String) aiResult.getOrDefault("feedback", "");
                recommendations = (List<String>) aiResult.getOrDefault("recommendations", new ArrayList<>());
            } catch (Exception e) {
                log.error("AI scoring failed: {}", e.getMessage());
                aiFeedback = "AI analysis unavailable";
            }
        }

        int overallScore = (int) (ruleBasedScore * 0.4 + aiScore * 0.6);

        return JobMatchResponse.builder()
                .overallScore(overallScore)
                .skillMatchScore(skillScore)
                .educationMatchScore(educationScore)
                .experienceMatchScore(experienceScore)
                .ruleBasedScore(ruleBasedScore)
                .aiSemanticScore(aiScore)
                .aiFeedback(aiFeedback)
                .matchedSkills(matchedSkills)
                .missingSkills(missingSkills)
                .recommendations(recommendations)
                .jobTitle(job.getTitle())
                .companyName(job.getCompany().getCompanyName())
                .build();
    }

    @Override
    public int calculateQuickScore(JobSeekerProfile profile, Job job) {
        final Set<String> candidateSkills = new HashSet<>();

        // 1) Always take skills from profile.
        if (profile.getJobSeekerSkills() != null && !profile.getJobSeekerSkills().isEmpty()) {
            candidateSkills.addAll(profile.getJobSeekerSkills().stream()
                    .filter(js -> js.getSkill() != null && js.getSkill().getSkillName() != null)
                    .map(js -> js.getSkill().getSkillName().toLowerCase().trim())
                    .collect(Collectors.toSet()));
        }

        // 2) Merge skills from preferred resume (primary first, otherwise latest non-deleted).
        if (profile.getResumes() != null && !profile.getResumes().isEmpty()) {
            profile.getResumes().stream()
                    .filter(r -> !Boolean.TRUE.equals(r.getIsDeleted()))
                    .sorted(Comparator
                            .comparing((Resume r) -> Boolean.TRUE.equals(r.getIsPrimary())).reversed()
                            .thenComparing(Resume::getUploadedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                    .findFirst()
                    .ifPresent(preferredResume -> {
                        if (preferredResume.getResumeSkills() != null && !preferredResume.getResumeSkills().isEmpty()) {
                            candidateSkills.addAll(preferredResume.getResumeSkills().stream()
                                    .filter(rs -> rs.getSkill() != null && rs.getSkill().getSkillName() != null)
                                    .map(rs -> rs.getSkill().getSkillName().toLowerCase().trim())
                                    .collect(Collectors.toSet()));
                        }

                        // 3) Also enrich from extracted text if available.
                        if (preferredResume.getExtractedText() != null && !preferredResume.getExtractedText().isBlank()) {
                            String text = preferredResume.getExtractedText().toLowerCase();
                            if (job.getJobSkills() != null) {
                                for (JobSkill js : job.getJobSkills()) {
                                    String sName = js.getSkill().getSkillName().toLowerCase().trim();
                                    if (text.contains(sName)) {
                                        candidateSkills.add(sName);
                                    }
                                }
                            }
                        }
                    });
        }

        Set<String> allJobSkills = new HashSet<>();
        Set<String> requiredSkills = new HashSet<>();
        if (job.getJobSkills() != null) {
            for (JobSkill js : job.getJobSkills()) {
                String name = js.getSkill().getSkillName().toLowerCase().trim();
                allJobSkills.add(name);
                if (Boolean.TRUE.equals(js.getIsRequired())) requiredSkills.add(name);
            }
        }

        if (allJobSkills.isEmpty()) return 50;

        long matched = allJobSkills.stream().filter(candidateSkills::contains).count();
        int skillScore = (int) ((double) matched / allJobSkills.size() * 100);

        if (!requiredSkills.isEmpty()) {
            long missingReq = requiredSkills.stream().filter(s -> !candidateSkills.contains(s)).count();
            skillScore = Math.max(0, skillScore - (int) ((double) missingReq / requiredSkills.size() * 30));
        }

        return skillScore;
    }

    @Transactional
    @Override
    public void recalculateForProfile(Integer profileId) {
        JobSeekerProfile profile = jobSeekerProfileRepository.findById(profileId)
                .orElseThrow(() -> new NotFoundException("Profile not found"));

        jobMatchScoreRepository.deleteByProfileProfileId(profileId);
        jobMatchScoreRepository.flush();

        List<Job> activeJobs = jobRepository.findByIsActiveTrueAndIsDeletedFalse();

        List<JobMatchScore> scores = activeJobs.stream().map(job -> {
            int score = calculateQuickScore(profile, job);
            return JobMatchScore.builder()
                    .profile(profile)
                    .job(job)
                    .overallScore(score)
                    .skillMatchScore(score)
                    .calculatedAt(LocalDateTime.now())
                    .build();
        }).collect(Collectors.toList());

        jobMatchScoreRepository.saveAll(scores);
        log.info("Recalculated {} match scores for profile {}", scores.size(), profileId);
    }

    @Transactional
    @Override
    public void recalculateForJob(Integer jobId) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new NotFoundException("Job not found"));

        jobMatchScoreRepository.deleteByJobJobId(jobId);
        jobMatchScoreRepository.flush();

        List<JobSeekerProfile> profiles = jobSeekerProfileRepository.findAll();

        List<JobMatchScore> scores = profiles.stream().map(profile -> {
            int score = calculateQuickScore(profile, job);
            return JobMatchScore.builder()
                    .profile(profile)
                    .job(job)
                    .overallScore(score)
                    .skillMatchScore(score)
                    .calculatedAt(LocalDateTime.now())
                    .build();
        }).collect(Collectors.toList());

        jobMatchScoreRepository.saveAll(scores);
        log.info("Recalculated {} match scores for job {}", scores.size(), jobId);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getAIMatchScore(String cvText, Job job) {
        StringBuilder jdText = new StringBuilder();
        jdText.append("Job Title: ").append(job.getTitle()).append("\n");
        if (job.getDescription() != null) jdText.append("Description: ").append(job.getDescription()).append("\n");
        if (job.getRequirements() != null) jdText.append("Requirements: ").append(job.getRequirements()).append("\n");
        if (job.getYearsOfExperience() != null) jdText.append("Experience Required: ").append(job.getYearsOfExperience()).append("\n");
        if (job.getEducationLevel() != null) jdText.append("Education: ").append(job.getEducationLevel()).append("\n");
        if (job.getJobType() != null) jdText.append("Job Type: ").append(job.getJobType()).append("\n");
        if (job.getLocation() != null) jdText.append("Location: ").append(job.getLocation()).append("\n");

        if (job.getJobSkills() != null && !job.getJobSkills().isEmpty()) {
            jdText.append("Required Skills: ");
            jdText.append(job.getJobSkills().stream()
                    .map(js -> js.getSkill().getSkillName() +
                            (Boolean.TRUE.equals(js.getIsRequired()) ? " (required)" : " (preferred)"))
                    .collect(Collectors.joining(", ")));
            jdText.append("\n");
        }

        String systemPrompt = """
                You are a recruitment expert. Compare the CV with the Job Description.
                Analyze semantic similarity, not just exact keyword matching.
                Consider synonyms and related skills.
                Return ONLY valid JSON (no markdown, no code blocks).
                If the CV is in Vietnamese, respond in Vietnamese.
                """;

        String userPrompt = """
                Compare this CV with the Job Description:
                
                === CV ===
                %s
                
                === JOB DESCRIPTION ===
                %s
                
                Return ONLY this JSON:
                {
                  "score": <0-100>,
                  "feedback": "<2-3 sentences>",
                  "matchedAreas": ["<matched area>"],
                  "gaps": ["<gap>"],
                  "recommendations": ["<recommendation>"]
                }
                """.formatted(cvText, jdText.toString());

        String aiResponse = openAIService.chatCompletion(systemPrompt, userPrompt);

        try {
            String cleanJson = aiResponse
                    .replaceAll("```json\\s*", "")
                    .replaceAll("```\\s*", "")
                    .trim();

            JsonNode root = objectMapper.readTree(cleanJson);
            Map<String, Object> result = new HashMap<>();
            result.put("score", root.path("score").asInt(50));
            result.put("feedback", root.path("feedback").asText(""));

            List<String> recs = new ArrayList<>();
            if (root.has("recommendations")) {
                root.path("recommendations").forEach(n -> recs.add(n.asText()));
            }
            result.put("recommendations", recs);

            return result;
        } catch (Exception e) {
            log.error("Failed to parse AI match response: {}", aiResponse, e);
            return Map.of("score", 50, "feedback", "AI parse error", "recommendations", List.of());
        }
    }

    private String buildProfileText(JobSeekerProfile profile) {
        StringBuilder sb = new StringBuilder();
        sb.append("Name: ").append(profile.getUser().getFullName()).append("\n");
        if (profile.getTitle() != null) sb.append("Title: ").append(profile.getTitle()).append("\n");

        if (profile.getJobSeekerSkills() != null) {
            sb.append("Skills: ");
            sb.append(profile.getJobSeekerSkills().stream()
                    .map(js -> js.getSkill().getSkillName())
                    .collect(Collectors.joining(", ")));
            sb.append("\n");
        }

        if (profile.getWorkExperiences() != null) {
            sb.append("\nExperience:\n");
            for (WorkExperience we : profile.getWorkExperiences()) {
                sb.append("- ").append(we.getTitle());
                if (we.getDescription() != null) sb.append(": ").append(we.getDescription());
                sb.append("\n");
            }
        }

        if (profile.getEducations() != null) {
            sb.append("\nEducation:\n");
            for (Education edu : profile.getEducations()) {
                sb.append("- ").append(edu.getTitle());
                if (edu.getMajor() != null) sb.append(", ").append(edu.getMajor().getName());
                sb.append("\n");
            }
        }

        return sb.toString();
    }

    private Set<String> extractSkillsFromText(String text, Set<String> referenceSkills) {
        if (text == null || text.isBlank() || referenceSkills.isEmpty()) {
            return Set.of();
        }
        String normalizedText = text.toLowerCase();
        Set<String> extracted = new HashSet<>();
        for (String skill : referenceSkills) {
            if (normalizedText.contains(skill)) {
                extracted.add(skill);
            }
        }
        return extracted;
    }

    private int calculateEducationScore(JobSeekerProfile profile, Resume resume, Job job) {
        if (job.getEducationLevel() == null || job.getEducationLevel() == EducationLevel.ANY) {
            return 100;
        }

        JobSeekerProfile candidateProfile = profile;
        if (candidateProfile == null && resume != null) {
            candidateProfile = resume.getJobSeekerProfile();
        }
        if (candidateProfile == null || candidateProfile.getEducations() == null || candidateProfile.getEducations().isEmpty()) {
            return 20;
        }

        int requiredRank = educationRank(job.getEducationLevel());
        int candidateRank = candidateProfile.getEducations().stream()
                .map(Education::getEducationLevel)
                .filter(Objects::nonNull)
                .mapToInt(this::educationRank)
                .max()
                .orElse(0);

        if (candidateRank >= requiredRank) return 100;
        if (candidateRank == requiredRank - 1) return 70;
        if (candidateRank == requiredRank - 2) return 40;
        return 20;
    }

    private int educationRank(EducationLevel level) {
        return switch (level) {
            case HIGH_SCHOOL -> 1;
            case VOCATIONAL -> 2;
            case ASSOCIATE -> 3;
            case BACHELOR -> 4;
            case MASTER -> 5;
            case DOCTORATE -> 6;
            case OTHER -> 1;
            case ANY -> 0;
        };
    }

    private int calculateExperienceScore(JobSeekerProfile profile, Resume resume, Job job) {
        int requiredYears = parseMinimumRequiredYears(job.getYearsOfExperience());
        JobSeekerProfile candidateProfile = profile;
        if (candidateProfile == null && resume != null) {
            candidateProfile = resume.getJobSeekerProfile();
        }

        double candidateYears = estimateCandidateYears(candidateProfile);

        if (requiredYears <= 0) {
            if (candidateYears >= 3) return 100;
            if (candidateYears >= 1) return 80;
            if (candidateYears > 0) return 60;
            return 40;
        }

        if (candidateYears >= requiredYears) return 100;
        if (candidateYears <= 0) return 0;
        return Math.max(10, (int) Math.round((candidateYears / requiredYears) * 100));
    }

    private int parseMinimumRequiredYears(String yearsText) {
        if (yearsText == null || yearsText.isBlank()) {
            return 0;
        }
        Matcher matcher = Pattern.compile("\\d+").matcher(yearsText);
        if (matcher.find()) {
            return Integer.parseInt(matcher.group());
        }
        return 0;
    }

    private double estimateCandidateYears(JobSeekerProfile profile) {
        if (profile == null || profile.getWorkExperiences() == null || profile.getWorkExperiences().isEmpty()) {
            return 0;
        }

        long totalMonths = 0;
        LocalDate now = LocalDate.now();
        for (WorkExperience we : profile.getWorkExperiences()) {
            if (we.getStartDate() == null) {
                continue;
            }
            LocalDate end = we.getEndDate() != null ? we.getEndDate() : now;
            if (end.isBefore(we.getStartDate())) {
                continue;
            }
            Period period = Period.between(we.getStartDate(), end);
            totalMonths += (long) period.getYears() * 12 + period.getMonths();
        }
        return totalMonths / 12.0;
    }
}
