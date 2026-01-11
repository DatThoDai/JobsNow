package com.JobsNow.backend.service;

import com.JobsNow.backend.request.ApplicationRequest;
import com.JobsNow.backend.response.ApplicationDetailResponse;
import com.JobsNow.backend.response.ApplicationOfJobResponse;

import java.util.List;

public interface ApplicationService {
    void applyForJob(ApplicationRequest request);
    List<ApplicationDetailResponse> getApplicationsByJobSeeker(Integer profileId);
    ApplicationDetailResponse getApplicationDetail(Integer applicationId);

    List<ApplicationOfJobResponse> getApplicationsByJob(Integer jobId);
    void updateApplicationStatus(Integer applicationId, String status);
}
