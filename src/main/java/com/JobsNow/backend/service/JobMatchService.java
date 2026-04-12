package com.JobsNow.backend.service;

import com.JobsNow.backend.entity.Job;
import com.JobsNow.backend.entity.JobSeekerProfile;
import com.JobsNow.backend.request.JobMatchRequest;
import com.JobsNow.backend.response.JobMatchResponse;

public interface JobMatchService {
    JobMatchResponse calculateMatch(JobMatchRequest request);

    int calculateQuickScore(JobSeekerProfile profile, Job job);

    void recalculateForProfile(Integer profileId);

    void recalculateForJob(Integer jobId);
}
