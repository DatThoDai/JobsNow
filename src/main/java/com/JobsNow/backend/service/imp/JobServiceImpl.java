package com.JobsNow.backend.service.imp;

import com.JobsNow.backend.dto.JobDTO;
import com.JobsNow.backend.entity.*;
import com.JobsNow.backend.entity.enums.ApplicationLanguage;
import com.JobsNow.backend.entity.enums.EducationLevel;
import com.JobsNow.backend.entity.enums.GenderRequirement;
import com.JobsNow.backend.entity.enums.JobType;
import com.JobsNow.backend.exception.BadRequestException;
import com.JobsNow.backend.exception.NotFoundException;
import com.JobsNow.backend.mapper.JobMapper;
import com.JobsNow.backend.repositories.*;
import com.JobsNow.backend.request.CreateJobRequest;
import com.JobsNow.backend.request.RejectJobRequest;
import com.JobsNow.backend.request.UpdateJobRequest;
import com.JobsNow.backend.service.EmailService;
import com.JobsNow.backend.service.JobService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.chrono.ChronoLocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class JobServiceImpl implements JobService {
    private final JobRepository jobRepository;
    private final CompanyRepository companyRepository;
    private final JobCategoryRepository jobCategoryRepository;
    private final SkillRepository skillRepository;
    private final JobSkillRepository jobSkillRepository;
    private final MajorRepository majorRepository;
    private final EmailService emailService;
    @Transactional
    @Override
    public void createJob(CreateJobRequest request) {
        Company company = companyRepository.findById(request.getCompanyId())
                .orElseThrow(() -> new NotFoundException("Company not found"));
        Job job = new Job();
        job.setCompany(company);
        job.setTitle(request.getTitle());
        job.setDescription(request.getDescription());
        job.setRequirements(request.getRequirements());
        job.setBenefits(request.getBenefits());
        job.setSalaryMin(request.getSalaryMin());
        job.setSalaryMax(request.getSalaryMax());
        job.setYearsOfExperience(request.getYearsOfExperience());
        if (request.getEducationLevel() != null) {
            try {
                job.setEducationLevel(EducationLevel.valueOf(request.getEducationLevel().toUpperCase()));
            } catch (IllegalArgumentException e) {
                throw new BadRequestException("Invalid education level. Allowed: HIGH_SCHOOL, ASSOCIATE, BACHELOR, MASTER, DOCTORATE, OTHER");
            }
        }
        job.setLocation(request.getLocation());
        job.setDeadline(request.getDeadline());
        job.setPostedAt(LocalDateTime.now());
        job.setIsDeleted(false);
        job.setIsExpired(false);
        if (request.getThumbnailUrl() != null && !request.getThumbnailUrl().isBlank()) {
            job.setThumbnailUrl(request.getThumbnailUrl());
        }
        if (request.getIsActive() != null && request.getIsActive()) {
            job.setIsActive(true);
            job.setIsPending(false);
            job.setIsApproved(true);
        } else {
            job.setIsActive(false);
            job.setIsPending(true);
            job.setIsApproved(false);
        }
        if (request.getJobType() != null) {
            job.setJobType(JobType.valueOf(request.getJobType().toUpperCase()));
        }
        if (request.getApplicationLanguage() != null && !request.getApplicationLanguage().isBlank()) {
            try {
                job.setApplicationLanguage(ApplicationLanguage.valueOf(request.getApplicationLanguage().toUpperCase()));
            } catch (IllegalArgumentException e) {
                throw new BadRequestException("Invalid application language");
            }
        }
        if (request.getGenderRequirement() != null && !request.getGenderRequirement().isBlank()) {
            try {
                job.setGenderRequirement(GenderRequirement.valueOf(request.getGenderRequirement().toUpperCase()));
            } catch (IllegalArgumentException e) {
                throw new BadRequestException("Invalid gender requirement");
            }
        }
        job.setMinAge(request.getMinAge());
        job.setMaxAge(request.getMaxAge());
        validateAgeRange(job.getMinAge(), job.getMaxAge());
        if (request.getCategoryId() != null) {
            JobCategory category = jobCategoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new NotFoundException("Category not found"));
            job.setCategory(category);
        }
        Job savedJob = jobRepository.save(job);

        if(request.getJobSkills()!=null && !request.getJobSkills().isEmpty()){
            List<JobSkill> jobSkills = new ArrayList<>();
            for(CreateJobRequest.JobSkillItem skillItem : request.getJobSkills()){
                if(skillItem.getSkillId() == null){
                    throw new BadRequestException("Skill ID cannot be null");
                }
                Skill skill = skillRepository.findById(skillItem.getSkillId())
                        .orElseThrow(() -> new NotFoundException("Skill not found"));
                JobSkillId jobSkillId = new JobSkillId(savedJob.getJobId(), skill.getSkillId());
                JobSkill jobSkill = new JobSkill();
                jobSkill.setId(jobSkillId);
                jobSkill.setJob(savedJob);
                jobSkill.setSkill(skill);
                jobSkill.setIsRequired(skillItem.getIsRequired());
                jobSkill.setLevel(skillItem.getLevel());
                jobSkills.add(jobSkill);
            }
            jobSkillRepository.saveAll(jobSkills);
            savedJob.setJobSkills(jobSkills);
        }

        if (request.getMajorIds() != null && !request.getMajorIds().isEmpty()) {
            List<Major> majors = new ArrayList<>();
            for(Integer majorId : request.getMajorIds()){
                Major major = majorRepository.findById(majorId)
                        .orElseThrow(() -> new NotFoundException("Major not found"));
                majors.add(major);
            }
            savedJob.setMajors(majors);
            jobRepository.save(savedJob);
        }
        int currentCount = company.getJobPostCount() != null ? company.getJobPostCount(): 0;
        company.setJobPostCount(currentCount + 1);
        companyRepository.save(company);
    }

    private boolean isJobAvailable(Job job) {
        return Boolean.TRUE.equals(job.getIsActive())
                && Boolean.TRUE.equals(job.getIsApproved())
                && !Boolean.TRUE.equals(job.getIsDeleted())
                && !Boolean.TRUE.equals(job.getIsExpired());
    }

    @Override
    @Transactional(readOnly = true)
    public JobDTO getJobById(Integer jobId) {
        Job job = jobRepository.findByIdWithCompanyAndSocials(jobId)
                .orElseThrow(() -> new NotFoundException("Job not found"));
        return JobMapper.toJobDTO(job);
    }

    private static void validateAgeRange(Integer minAge, Integer maxAge) {
        if (minAge != null && maxAge != null && minAge > maxAge) {
            throw new BadRequestException("minAge must be <= maxAge");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<JobDTO> getRelatedJobs(Integer jobId, int limit) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new NotFoundException("Job not found"));
        if (job.getCategory() == null) {
            return new ArrayList<>();
        }
        int cap = Math.min(Math.max(limit, 1), 24);
        List<Job> related = jobRepository.findRelatedByCategory(
                job.getCategory().getId(),
                jobId,
                PageRequest.of(0, cap));
        return related.stream().map(JobMapper::toJobDTO).toList();
    }

    @Override
    public List<JobDTO> getAllJobs() {
        List<Job> jobs = jobRepository.findAll().stream()
                .filter(this::isJobAvailable)
                .toList();
        return jobs.stream()
                .map(JobMapper::toJobDTO)
                .toList();
    }

    @Override
    public List<JobDTO> getJobsByCompanyId(Integer companyId) {
        List<Job> jobs = jobRepository.findByCompany_CompanyId(companyId);
        return jobs.stream()
                .map(JobMapper::toJobDTO)
                .toList();
    }

    @Override
    @Transactional
    public void updateJob(UpdateJobRequest request) {
        Job job = jobRepository.findById(request.getJobId())
                .orElseThrow(() -> new NotFoundException("Job not found"));

        if (request.getTitle() != null) {
            job.setTitle(request.getTitle());
        }
        if (request.getDescription() != null) {
            job.setDescription(request.getDescription());
        }
        if (request.getRequirements() != null) {
            job.setRequirements(request.getRequirements());
        }
        if (request.getBenefits() != null) {
            job.setBenefits(request.getBenefits());
        }
        if (request.getSalaryMin() != null) {
            job.setSalaryMin(request.getSalaryMin());
        }
        if (request.getSalaryMax() != null) {
            job.setSalaryMax(request.getSalaryMax());
        }
        if (request.getYearsOfExperience() != null) {
            job.setYearsOfExperience(request.getYearsOfExperience());
        }
        if (request.getLocation() != null) {
            job.setLocation(request.getLocation());
        }
        if (request.getDeadline() != null) {
            job.setDeadline(request.getDeadline());
        }
        if (request.getEducationLevel() != null) {
            try {
                job.setEducationLevel(EducationLevel.valueOf(request.getEducationLevel().toUpperCase()));
            } catch (IllegalArgumentException e) {
                throw new BadRequestException("Invalid education level. Allowed: HIGH_SCHOOL, ASSOCIATE, BACHELOR, MASTER, DOCTORATE, OTHER");
            }
        }
        if (request.getJobType() != null) {
            job.setJobType(JobType.valueOf(request.getJobType().toUpperCase()));
        }
        if (request.getCategoryId() != null) {
            JobCategory category = jobCategoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new NotFoundException("Category not found"));
            job.setCategory(category);
        }
        if (request.getJobSkills() != null) {
            jobSkillRepository.deleteByJobId(job.getJobId());
            if (!request.getJobSkills().isEmpty()) {
                List<JobSkill> newJobSkills = new ArrayList<>();
                for (UpdateJobRequest.JobSkillItem item : request.getJobSkills()) {
                    if (item.getSkillId() == null) {
                        throw new BadRequestException("Skill ID cannot be null");
                    }
                    Skill skill = skillRepository.findById(item.getSkillId())
                            .orElseThrow(() -> new NotFoundException("Skill not found"));
                    JobSkillId jobSkillId = new JobSkillId(job.getJobId(), skill.getSkillId());
                    JobSkill jobSkill = new JobSkill();
                    jobSkill.setId(jobSkillId);
                    jobSkill.setJob(job);
                    jobSkill.setSkill(skill);
                    jobSkill.setIsRequired(
                            item.getIsRequired() != null ? item.getIsRequired() : false
                    );
                    jobSkill.setLevel(item.getLevel());
                    newJobSkills.add(jobSkill);
                }
                jobSkillRepository.saveAll(newJobSkills);
                job.setJobSkills(newJobSkills);
            } else {
                job.setJobSkills(new ArrayList<>());
            }
        }
        if (request.getMajorIds() != null) {
            if (!request.getMajorIds().isEmpty()) {
                List<Major> majors = new ArrayList<>();
                for (Integer majorId : request.getMajorIds()) {
                    Major major = majorRepository.findById(majorId)
                            .orElseThrow(() -> new NotFoundException("Major not found: " + majorId));
                    majors.add(major);
                }
                job.setMajors(majors);
            } else {
                job.setMajors(new ArrayList<>());
            }
        }
        if (request.getIsActive() != null) {
            job.setIsActive(request.getIsActive());
        }
        if (request.getThumbnailUrl() != null) {
            job.setThumbnailUrl(request.getThumbnailUrl());
        }
        if (request.getApplicationLanguage() != null) {
            if (request.getApplicationLanguage().isBlank()) {
                job.setApplicationLanguage(null);
            } else {
                try {
                    job.setApplicationLanguage(ApplicationLanguage.valueOf(request.getApplicationLanguage().toUpperCase()));
                } catch (IllegalArgumentException e) {
                    throw new BadRequestException("Invalid application language");
                }
            }
        }
        if (request.getGenderRequirement() != null) {
            if (request.getGenderRequirement().isBlank()) {
                job.setGenderRequirement(null);
            } else {
                try {
                    job.setGenderRequirement(GenderRequirement.valueOf(request.getGenderRequirement().toUpperCase()));
                } catch (IllegalArgumentException e) {
                    throw new BadRequestException("Invalid gender requirement");
                }
            }
        }
        if (request.getMinAge() != null) {
            job.setMinAge(request.getMinAge());
        }
        if (request.getMaxAge() != null) {
            job.setMaxAge(request.getMaxAge());
        }
        validateAgeRange(job.getMinAge(), job.getMaxAge());
        if (!Boolean.TRUE.equals(job.getIsApproved())) {
            job.setIsPending(true);
            job.setIsApproved(false);
        }
        jobRepository.save(job);
    }

    @Override
    public void deleteJob(Integer jobId) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new NotFoundException("Job not found"));
        job.setIsDeleted(true);
        job.setIsActive(false);
        jobRepository.save(job);
    }

    @Override
    public List<JobDTO> searchJobs(String keyword, List<String> location, List<Integer> categoryIds) {
        if (location != null && location.isEmpty()) {
            location = null;
        }
        if (categoryIds != null && categoryIds.isEmpty()) {
            categoryIds = null;
        }
        List<Job> jobs = jobRepository.searchJobs(keyword, location, categoryIds).stream()
                .filter(this::isJobAvailable)
                .toList();
        return jobs.stream()
                .map(JobMapper::toJobDTO)
                .toList();
    }

    @Override
    public void approveJob(Integer jobId) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new NotFoundException("Job not found"));
        job.setIsApproved(true);
        job.setIsPending(false);
        job.setIsActive(true);
        job.setNote(null);
        jobRepository.save(job);
        try {
            String toEmail = job.getCompany().getUser().getEmail();
            if (toEmail != null && !toEmail.isBlank()) {
                emailService.sendJobPostApprovedEmail(toEmail, job.getTitle(), job.getCompany().getCompanyName());
            }
        } catch (Exception e) {
            log.error("Failed to send job approved email, jobId={}", jobId, e);
        }
    }

    @Override
    public void rejectJob(RejectJobRequest request) {
        Job job = jobRepository.findById(request.getJobId())
                .orElseThrow(() -> new NotFoundException("Job not found"));
        job.setIsApproved(false);
        job.setIsPending(false);
        job.setIsActive(false);
        job.setNote(request.getReason());
        jobRepository.save(job);
        try {
            String toEmail = job.getCompany().getUser().getEmail();
            if (toEmail != null && !toEmail.isBlank()) {
                emailService.sendJobPostRejectedEmail(toEmail, job.getTitle(), job.getCompany().getCompanyName(), request.getReason());
            }
        } catch (Exception e) {
            log.error("Failed to send job rejected email, jobId={}", request.getJobId(), e);
        }
    }

    @Override
    public List<JobDTO> getAllJobsForAdmin(String status) {
        List<Job> all = jobRepository.findAll();
        if (status != null && !status.isBlank()) {
            String s = status.trim().toLowerCase();
            if ("pending".equals(s)) {
                all = all.stream().filter(job -> Boolean.TRUE.equals(job.getIsPending())).toList();
            } else if ("approved".equals(s)) {
                all = all.stream().filter(job -> Boolean.TRUE.equals(job.getIsApproved())).toList();
            } else if ("rejected".equals(s)) {
                all = all.stream()
                        .filter(job -> !Boolean.TRUE.equals(job.getIsApproved()) && !Boolean.TRUE.equals(job.getIsPending()))
                        .toList();
            }
        }
        return all.stream().map(JobMapper::toJobDTO).toList();
    }

    @Scheduled(cron = "0 0 0 * * ?")
    public void checkAndUpdateExpiredJobs(){
        LocalDateTime today = LocalDateTime.now();
        List<Job> jobs = jobRepository.findByIsActiveTrueAndIsDeletedFalse();
        for(Job job : jobs){
            if(job.getDeadline() != null && job.getDeadline().isBefore(ChronoLocalDate.from(today))){
                job.setIsExpired(true);
                job.setIsActive(false);
                jobRepository.save(job);
            }
        }
    }
}
