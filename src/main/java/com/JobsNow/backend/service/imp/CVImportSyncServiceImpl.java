package com.JobsNow.backend.service.imp;

import com.JobsNow.backend.entity.Skill;
import com.JobsNow.backend.repositories.SkillRepository;
import com.JobsNow.backend.request.CertificateRequest;
import com.JobsNow.backend.request.EducationRequest;
import com.JobsNow.backend.request.ProjectRequest;
import com.JobsNow.backend.request.ResumeSkillRequest;
import com.JobsNow.backend.request.WorkExperienceRequest;
import com.JobsNow.backend.response.ParsedCVData;
import com.JobsNow.backend.service.CertificateService;
import com.JobsNow.backend.service.CVImportSyncService;
import com.JobsNow.backend.service.EducationService;
import com.JobsNow.backend.service.ProjectService;
import com.JobsNow.backend.service.ResumeSkillService;
import com.JobsNow.backend.service.WorkExperienceService;
import com.JobsNow.backend.util.CvImportDateParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CVImportSyncServiceImpl implements CVImportSyncService {

    private final WorkExperienceService workExperienceService;
    private final EducationService educationService;
    private final ProjectService projectService;
    private final CertificateService certificateService;
    private final ResumeSkillService resumeSkillService;
    private final SkillRepository skillRepository;

    @Override
    @Transactional
    public int syncParsedCvToResumeSections(Integer resumeId, ParsedCVData parsed, boolean replaceExisting) {
        if (parsed == null || resumeId == null) {
            return 0;
        }
        if (replaceExisting) {
            clearResumeSections(resumeId);
        }
        int created = 0;
        int sort = 0;

        if (parsed.getWorkExperiences() != null) {
            for (ParsedCVData.ParsedWorkExperience we : parsed.getWorkExperiences()) {
                if (we == null || isBlank(we.getPosition()) && isBlank(we.getCompany())) {
                    continue;
                }
                try {
                    WorkExperienceRequest req = new WorkExperienceRequest();
                    req.setTitle(buildWorkTitle(we));
                    req.setLevel("OTHER");
                    req.setStartDate(CvImportDateParser.parseStartDate(we.getStartDate(), we.getDuration()));
                    req.setEndDate(CvImportDateParser.parseEndDate(we.getEndDate(), we.getDuration()));
                    req.setDescription(buildWorkDescription(we));
                    req.setSortOrder(sort++);
                    workExperienceService.create(resumeId, req);
                    created++;
                } catch (Exception e) {
                    log.warn("Skip work experience sync for resume {}: {}", resumeId, e.getMessage());
                }
            }
        }

        sort = 0;
        if (parsed.getEducations() != null) {
            for (ParsedCVData.ParsedEducation edu : parsed.getEducations()) {
                if (edu == null || isBlank(edu.getSchool())) {
                    continue;
                }
                try {
                    EducationRequest req = new EducationRequest();
                    req.setTitle(edu.getSchool().trim());
                    req.setEducationLevel(mapEducationLevel(edu.getDegree()));
                    req.setStartDate(CvImportDateParser.parseStartDate(edu.getStartDate(), edu.getDuration()));
                    req.setEndDate(CvImportDateParser.parseEndDate(edu.getEndDate(), edu.getDuration()));
                    req.setDescription(buildEducationDescription(edu));
                    req.setSortOrder(sort++);
                    educationService.create(resumeId, req);
                    created++;
                } catch (Exception e) {
                    log.warn("Skip education sync for resume {}: {}", resumeId, e.getMessage());
                }
            }
        }

        sort = 0;
        if (parsed.getProjects() != null) {
            for (ParsedCVData.ParsedProject prj : parsed.getProjects()) {
                if (prj == null || isBlank(prj.getName())) {
                    continue;
                }
                try {
                    ProjectRequest req = new ProjectRequest();
                    req.setTitle(prj.getName().trim());
                    req.setStartDate(CvImportDateParser.parseStartDate(null, prj.getDuration()));
                    req.setEndDate(CvImportDateParser.parseEndDate(null, prj.getDuration()));
                    req.setDescription(buildProjectDescription(prj));
                    req.setSortOrder(sort++);
                    projectService.create(resumeId, req);
                    created++;
                } catch (Exception e) {
                    log.warn("Skip project sync for resume {}: {}", resumeId, e.getMessage());
                }
            }
        }

        sort = 0;
        if (parsed.getCertificates() != null) {
            for (ParsedCVData.ParsedCertificate cert : parsed.getCertificates()) {
                if (cert == null || isBlank(cert.getName())) {
                    continue;
                }
                try {
                    CertificateRequest req = new CertificateRequest();
                    req.setTitle(cert.getName().trim());
                    req.setIssueDate(CvImportDateParser.parseIssueDate(cert.getIssueDate()));
                    req.setDescription(buildCertificateDescription(cert));
                    req.setSortOrder(sort++);
                    certificateService.create(resumeId, req);
                    created++;
                } catch (Exception e) {
                    log.warn("Skip certificate sync for resume {}: {}", resumeId, e.getMessage());
                }
            }
        }

        if (parsed.getSkills() != null) {
            for (ParsedCVData.ParsedSkill skill : parsed.getSkills()) {
                if (skill == null || isBlank(skill.getName())) {
                    continue;
                }
                try {
                    Skill entity = findOrCreateSkill(skill.getName().trim());
                    ResumeSkillRequest req = new ResumeSkillRequest();
                    req.setSkillId(entity.getSkillId());
                    req.setLevel(mapSkillLevel(skill.getLevel()));
                    resumeSkillService.add(resumeId, req);
                    created++;
                } catch (Exception e) {
                    log.warn("Skip skill sync for resume {}: {}", resumeId, e.getMessage());
                }
            }
        }

        return created;
    }

    private void clearResumeSections(Integer resumeId) {
        workExperienceService.getByResumeId(resumeId).forEach(dto ->
                workExperienceService.delete(resumeId, dto.getId()));
        educationService.getByResumeId(resumeId).forEach(dto ->
                educationService.delete(resumeId, dto.getId()));
        projectService.getByResumeId(resumeId).forEach(dto ->
                projectService.delete(resumeId, dto.getId()));
        certificateService.getByResumeId(resumeId).forEach(dto ->
                certificateService.delete(resumeId, dto.getId()));
        resumeSkillService.getByResumeId(resumeId).forEach(dto ->
                resumeSkillService.remove(resumeId, dto.getSkillId()));
    }

    private Skill findOrCreateSkill(String name) {
        return skillRepository.findFirstBySkillNameIgnoreCase(name)
                .orElseGet(() -> skillRepository.save(Skill.builder().skillName(name).build()));
    }

    /** Title = primary role line only; everything else goes to description. */
    private String buildWorkTitle(ParsedCVData.ParsedWorkExperience we) {
        String position = trimToNull(we.getPosition());
        if (position != null) {
            return position;
        }
        return trimToNull(we.getCompany()) != null ? we.getCompany().trim() : "Kinh nghiệm";
    }

    private String buildWorkDescription(ParsedCVData.ParsedWorkExperience we) {
        List<String> lines = new ArrayList<>();
        appendLabeled(lines, "Công ty", we.getCompany());
        appendLabeled(lines, "Vị trí", we.getPosition());
        appendLabeled(lines, "Thời gian", formatDateRange(we.getDuration(), we.getStartDate(), we.getEndDate(), we.getIsCurrent()));
        appendBody(lines, we.getDescription());
        return joinDescription(lines);
    }

    private String buildEducationDescription(ParsedCVData.ParsedEducation edu) {
        List<String> lines = new ArrayList<>();
        appendLabeled(lines, "Bằng cấp", edu.getDegree());
        appendLabeled(lines, "Chuyên ngành", edu.getMajor());
        appendLabeled(lines, "Thời gian", formatDateRange(edu.getDuration(), edu.getStartDate(), edu.getEndDate(), null));
        appendBody(lines, edu.getDescription());
        return joinDescription(lines);
    }

    private String buildProjectDescription(ParsedCVData.ParsedProject prj) {
        List<String> lines = new ArrayList<>();
        appendLabeled(lines, "Thời gian", formatDateRange(prj.getDuration(), null, null, null));
        if (prj.getTechnologies() != null && !prj.getTechnologies().isEmpty()) {
            String tech = prj.getTechnologies().stream()
                    .filter(t -> t != null && !t.isBlank())
                    .collect(Collectors.joining(", "));
            if (!tech.isBlank()) {
                appendLabeled(lines, "Công nghệ", tech);
            }
        }
        appendBody(lines, prj.getDescription());
        return joinDescription(lines);
    }

    private String buildCertificateDescription(ParsedCVData.ParsedCertificate cert) {
        List<String> lines = new ArrayList<>();
        appendLabeled(lines, "Đơn vị cấp", cert.getIssuer());
        appendLabeled(lines, "Ngày cấp", cert.getIssueDate());
        return joinDescription(lines);
    }

    private void appendLabeled(List<String> lines, String label, String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        String v = value.trim();
        if (lines.stream().anyMatch(line -> line.contains(v))) {
            return;
        }
        lines.add(label + ": " + v);
    }

    private void appendBody(List<String> lines, String body) {
        if (body == null || body.isBlank()) {
            return;
        }
        String normalized = body.trim();
        if (!lines.isEmpty()) {
            lines.add("");
        }
        lines.add(normalized);
    }

    private String joinDescription(List<String> lines) {
        if (lines.isEmpty()) {
            return null;
        }
        return String.join("\n", lines);
    }

    private String formatDateRange(String duration, String startDate, String endDate, Boolean isCurrent) {
        if (duration != null && !duration.isBlank()) {
            return duration.trim();
        }
        String start = startDate != null ? startDate.trim() : "";
        String end = endDate != null ? endDate.trim() : "";
        if (Boolean.TRUE.equals(isCurrent)) {
            end = "Hiện tại";
        }
        if (!start.isBlank() && !end.isBlank()) {
            return start + " - " + end;
        }
        if (!start.isBlank()) {
            return start;
        }
        return !end.isBlank() ? end : null;
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private String mapEducationLevel(String degree) {
        if (degree == null || degree.isBlank()) {
            return "OTHER";
        }
        String d = degree.toLowerCase(Locale.ROOT);
        if (d.contains("tiến sĩ") || d.contains("doctor") || d.contains("phd")) {
            return "DOCTORATE";
        }
        if (d.contains("thạc sĩ") || d.contains("master")) {
            return "MASTER";
        }
        if (d.contains("cử nhân") || d.contains("đại học") || d.contains("bachelor")) {
            return "BACHELOR";
        }
        if (d.contains("cao đẳng") || d.contains("associate")) {
            return "ASSOCIATE";
        }
        if (d.contains("trung cấp") || d.contains("vocational")) {
            return "VOCATIONAL";
        }
        if (d.contains("phổ thông") || d.contains("high school")) {
            return "HIGH_SCHOOL";
        }
        return "OTHER";
    }

    private String mapSkillLevel(String level) {
        if (level == null || level.isBlank()) {
            return "INTERMEDIATE";
        }
        String l = level.toUpperCase(Locale.ROOT);
        if (l.contains("BEGIN") || l.contains("CƠ BẢN") || l.contains("BASIC")) {
            return "BEGINNER";
        }
        if (l.contains("ADVANCED") || l.contains("NÂNG CAO") || l.contains("EXPERT")) {
            return "ADVANCED";
        }
        return "INTERMEDIATE";
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
