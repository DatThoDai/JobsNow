package com.JobsNow.backend.service.imp;

import com.JobsNow.backend.dto.SavedJobDTO;
import com.JobsNow.backend.entity.Job;
import com.JobsNow.backend.entity.JobSeekerProfile;
import com.JobsNow.backend.entity.SavedJob;
import com.JobsNow.backend.exception.BadRequestException;
import com.JobsNow.backend.mapper.SavedJobMapper;
import com.JobsNow.backend.repositories.JobRepository;
import com.JobsNow.backend.repositories.JobSeekerProfileRepository;
import com.JobsNow.backend.repositories.SavedJobRepository;
import com.JobsNow.backend.service.SavedJobService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SavedJobServiceImpl implements SavedJobService {
    private final SavedJobRepository savedJobRepository;
    private final SavedJobMapper savedJobMapper;
    private final JobSeekerProfileRepository jobSeekerProfileRepository;
    private final JobRepository jobRepository;
    @Override
    public SavedJobDTO saveJob(Integer profileId, Integer jobId) {
        if(savedJobRepository.existsByJobSeekerProfile_ProfileIdAndJob_JobId(profileId, jobId)) {
            throw new BadRequestException("Job already saved");
        }
        JobSeekerProfile jobSeekerProfile = jobSeekerProfileRepository.findById(profileId)
                .orElseThrow(() -> new BadRequestException("Job seeker profile not found"));
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new BadRequestException("Job not found"));
        SavedJob savedJob = SavedJob.builder()
                .jobSeekerProfile(jobSeekerProfile)
                .job(job)
                .build();
        return savedJobMapper.toDTO(savedJobRepository.save(savedJob));
    }

    @Override
    public void unsaveJob(Integer profileId, Integer jobId) {
        SavedJob savedJob = savedJobRepository.findByJobSeekerProfile_ProfileIdAndJob_JobId(profileId, jobId)
                .orElseThrow(() -> new BadRequestException("Saved job not found"));
        savedJobRepository.delete(savedJob);
    }

    @Override
    public List<SavedJobDTO> getSavedJobs(Integer userId) {
        return savedJobRepository.findByJobSeekerProfile_ProfileIdOrderBySavedAtDesc(userId).stream()
                .map(savedJobMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public boolean isJobSaved(Integer profileId, Integer jobId) {
        return savedJobRepository.existsByJobSeekerProfile_ProfileIdAndJob_JobId(profileId, jobId);
    }
}
