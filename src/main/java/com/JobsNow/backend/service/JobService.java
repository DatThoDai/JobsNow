package com.JobsNow.backend.service;

import com.JobsNow.backend.dto.JobDTO;
import com.JobsNow.backend.request.CreateJobRequest;
import com.JobsNow.backend.request.RejectJobRequest;
import com.JobsNow.backend.request.UpdateJobRequest;

import java.util.List;

public interface JobService {
    void createJob(CreateJobRequest request);
    JobDTO getJobById(Integer jobId);
    List<JobDTO> getAllJobs();
    List<JobDTO> getJobsByCompanyId(Integer companyId);
    void updateJob(UpdateJobRequest request);
    void deleteJob(Integer jobId);
    List<JobDTO> searchJobs(String keyword, List<String> location, Integer categoryId);
    void approveJob(Integer jobId);
    void rejectJob(RejectJobRequest request);
}
