package com.JobsNow.backend.service;

import com.JobsNow.backend.dto.JobDTO;
import com.JobsNow.backend.request.CreateJobRequest;
import com.JobsNow.backend.request.RejectJobRequest;
import com.JobsNow.backend.request.UpdateJobRequest;
import com.JobsNow.backend.response.PagedResponse;

import java.io.IOException;
import java.util.List;

public interface JobService {
    void createJob(CreateJobRequest request);
    JobDTO getJobById(Integer jobId);

    List<JobDTO> getRelatedJobs(Integer jobId, int limit);

    List<JobDTO> getAllJobs();
    List<JobDTO> getJobsByCompanyId(Integer companyId);
    void updateJob(UpdateJobRequest request);
    void deleteJob(Integer jobId);
    void unpublishJobByAdmin(Integer jobId);
    List<JobDTO> searchJobs(String keyword, String location, String jobType, List<Integer> categoryIds);
    void approveJob(Integer jobId);
    void rejectJob(RejectJobRequest request);
    PagedResponse<JobDTO> getJobsForAdmin(String status, int page, int limit);
    List<JobDTO> getHotJobs(int limit);
    void pushJobsToAlgolia() throws IOException;
    void pushSingleJobToAlgolia(Integer jobId);

    void checkAndUpdateExpiredJobs();
}
