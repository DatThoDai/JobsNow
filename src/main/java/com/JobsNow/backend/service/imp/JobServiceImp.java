package com.JobsNow.backend.service.imp;

import com.JobsNow.backend.dto.JobDTO;
import com.JobsNow.backend.entity.Company;
import com.JobsNow.backend.entity.Job;
import com.JobsNow.backend.entity.JobCategory;
import com.JobsNow.backend.entity.Skill;
import com.JobsNow.backend.entity.enums.JobType;
import com.JobsNow.backend.exception.NotFoundException;
import com.JobsNow.backend.mapper.JobMapper;
import com.JobsNow.backend.repositories.CompanyRepository;
import com.JobsNow.backend.repositories.JobCategoryRepository;
import com.JobsNow.backend.repositories.JobRepository;
import com.JobsNow.backend.repositories.SkillRepository;
import com.JobsNow.backend.request.CreateJobRequest;
import com.JobsNow.backend.request.RejectJobRequest;
import com.JobsNow.backend.request.UpdateJobRequest;
import com.JobsNow.backend.service.JobService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.chrono.ChronoLocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class JobServiceImp implements JobService {
    private final JobRepository jobRepository;
    private final CompanyRepository companyRepository;
    private final JobCategoryRepository jobCategoryRepository;
    private final SkillRepository skillRepository;
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
        job.setEducationLevel(request.getEducationLevel());
        job.setLocation(request.getLocation());
        job.setDeadline(request.getDeadline());
        job.setPostedAt(LocalDateTime.now());
        job.setIsActive(false);
        job.setIsDeleted(false);
        job.setIsPending(true);
        job.setIsApproved(false);
        job.setIsExpired(false);
        if (request.getJobType() != null) {
            job.setJobType(JobType.valueOf(request.getJobType().toUpperCase()));
        }
        if (request.getCategoryId() != null) {
            JobCategory category = jobCategoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new NotFoundException("Category not found with ID: " + request.getCategoryId()));
            job.setCategory(category);
        }
        if (request.getSkillIds() != null && !request.getSkillIds().isEmpty()) {
            List<Skill> skills = new ArrayList<>(skillRepository.findAllById(request.getSkillIds()));
            job.setSkills(skills);
        }
        int currentCount = company.getCreateJobCount() != null ? company.getCreateJobCount() : 0;
        company.setCreateJobCount(currentCount + 1);
        companyRepository.save(company);

        Job savedJob = jobRepository.save(job);
    }

    @Override
    public JobDTO getJobById(Integer jobId) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new NotFoundException("Job not found"));
        return JobMapper.toJobDTO(job);
    }

    @Override
    public List<JobDTO> getAllJobs() {
        List<Job> jobs = jobRepository.findAll().stream()
                .filter(job -> Boolean.TRUE.equals(job.getIsActive())
                        && !Boolean.TRUE.equals(job.getIsDeleted())
                        && Boolean.TRUE.equals(job.getIsApproved())
                        && !Boolean.TRUE.equals(job.getIsExpired()))
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
    public void updateJob(UpdateJobRequest request) {
        Job job = jobRepository.findById(request.getJobId())
                .orElseThrow(() -> new NotFoundException("Job not found"));
        job.setTitle(request.getTitle());
        job.setDescription(request.getDescription());
        job.setRequirements(request.getRequirements());
        job.setBenefits(request.getBenefits());
        job.setSalaryMin(request.getSalaryMin());
        job.setSalaryMax(request.getSalaryMax());
        job.setYearsOfExperience(request.getYearsOfExperience());
        job.setEducationLevel(request.getEducationLevel());
        job.setLocation(request.getLocation());
        job.setDeadline(request.getDeadline());
        if (request.getJobType() != null) {
            job.setJobType(JobType.valueOf(request.getJobType().toUpperCase()));
        }
        if (request.getCategoryId() != null) {
            JobCategory category = jobCategoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new NotFoundException("Category not found"));
            job.setCategory(category);
        }
        if(request.getSkillIds() != null){
            List<Skill> skills = new ArrayList<>(skillRepository.findAllById(request.getSkillIds()));
            job.setSkills(skills);
        }
        if(request.getIsActive() != null){
            job.setIsActive(request.getIsActive());
        }
        job.setIsPending(true);
        job.setIsApproved(false);
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
    public List<JobDTO> searchJobs(String keyword, List<String> location, Integer categoryId) {
        if (location != null && location.isEmpty()) {
            location = null;
        }
        List<Job> jobs = jobRepository.searchJobs(keyword, location, categoryId).stream()
                .filter(job -> Boolean.TRUE.equals(job.getIsActive())
                        && !Boolean.TRUE.equals(job.getIsDeleted())
                        && Boolean.TRUE.equals(job.getIsApproved())
                        && !Boolean.TRUE.equals(job.getIsExpired()))
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
