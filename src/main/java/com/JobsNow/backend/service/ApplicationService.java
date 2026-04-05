package com.JobsNow.backend.service;

import com.JobsNow.backend.dto.ChartDataDTO;
import com.JobsNow.backend.dto.CompanyJobStatsDTO;
import com.JobsNow.backend.dto.RecentApplicationDTO;
import com.JobsNow.backend.dto.RegionChartDataDTO;
import com.JobsNow.backend.request.ApplicationRequest;
import com.JobsNow.backend.request.UpdateApplicationStatusRequest;
import com.JobsNow.backend.response.ApplicationDetailResponse;
import com.JobsNow.backend.response.ApplicationOfJobResponse;

import java.util.List;

public interface ApplicationService {
    void applyForJob(ApplicationRequest request);
    List<ApplicationDetailResponse> getApplicationsByJobSeeker(Integer profileId);
    ApplicationDetailResponse getApplicationDetail(Integer applicationId);
    List<ApplicationOfJobResponse> getApplicationsByJob(Integer jobId);
    List<ApplicationDetailResponse> getApplicationsByCompany(Integer companyId);
    void updateApplicationStatus(Integer applicationId, UpdateApplicationStatusRequest request);

    List<RecentApplicationDTO> getRecentApplications();
    ChartDataDTO getApplicationTrends(String type, Integer month);
    RegionChartDataDTO getActiveRegions(String type, Integer month);
    CompanyJobStatsDTO getCompanyJobStats(String type, Integer month);
}
