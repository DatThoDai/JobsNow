package com.JobsNow.backend.service;

import com.JobsNow.backend.dto.AiSearchRequest;
import com.JobsNow.backend.dto.AiSearchResponse;

public interface CandidateRAGService {
    AiSearchResponse searchCandidates(AiSearchRequest request, Integer companyId);
    void indexAllProfiles();
    void indexProfile(Integer profileId);
}
