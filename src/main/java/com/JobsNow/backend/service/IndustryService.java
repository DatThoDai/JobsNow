package com.JobsNow.backend.service;

import com.JobsNow.backend.dto.IndustryDTO;
import com.JobsNow.backend.request.CreateIndustryRequest;
import com.JobsNow.backend.request.UpdateIndustryRequest;
import com.JobsNow.backend.response.PagedResponse;

import java.util.List;

public interface IndustryService {
    List<IndustryDTO> getAllIndustries();

    PagedResponse<IndustryDTO> getIndustriesPage(int page, int limit);
    void addIndustry(CreateIndustryRequest request);
    void updateIndustry(UpdateIndustryRequest request);
    void deleteIndustry(Integer industryId);
}
