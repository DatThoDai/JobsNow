package com.JobsNow.backend.service;

import com.JobsNow.backend.dto.IndustryDTO;
import com.JobsNow.backend.request.CreateIndustryRequest;
import com.JobsNow.backend.request.UpdateIndustryRequest;

import java.util.List;

public interface IndustryService {
    List<IndustryDTO> getAllIndustries();
    void addIndustry(CreateIndustryRequest request);
    void updateIndustry(UpdateIndustryRequest request);
    void deleteIndustry(Integer industryId);
}
