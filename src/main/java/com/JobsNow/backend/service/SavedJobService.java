package com.JobsNow.backend.service;

import com.JobsNow.backend.dto.SavedJobDTO;

import java.util.List;

public interface SavedJobService {
    SavedJobDTO saveJob(Integer profileId, Integer jobId);
    void unsaveJob(Integer profileId, Integer jobId);
    List<SavedJobDTO> getSavedJobs(Integer profileId);
    boolean isJobSaved(Integer profileId, Integer jobId);
}
