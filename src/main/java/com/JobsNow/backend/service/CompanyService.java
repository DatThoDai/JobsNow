package com.JobsNow.backend.service;

import com.JobsNow.backend.dto.CompanyDTO;
import com.JobsNow.backend.request.CreateCompanyRequest;
import com.JobsNow.backend.dto.CompanyImageDTO;
import com.JobsNow.backend.request.UpdateCompanyRequest;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface CompanyService {
    List<CompanyDTO> getAllCompanies();
    CompanyDTO getCompanyById(Integer companyId);
    CompanyDTO getMyCompany(String email);
    CompanyDTO createMyCompany(String email, CreateCompanyRequest request, MultipartFile logoFile);
    List<CompanyDTO> findCompanyByIndustryOrCompanyName(Integer industryId, String companyName);
    void updateCompany(Integer companyId, UpdateCompanyRequest request, MultipartFile logoFile, MultipartFile bannerFile, List<MultipartFile> thumbnailFiles);
    void uploadLogo(Integer companyId, MultipartFile logoFile);
    void deleteLogo(Integer companyId);

    void uploadBanner(Integer companyId, MultipartFile bannerFile);
    void deleteBanner(Integer companyId);
    List<CompanyImageDTO> addCompanyImage(Integer companyId, MultipartFile imageFile, String type);
    void deleteCompanyImage(Integer imageId);
    List<CompanyImageDTO> getCompanyImages(Integer companyId);
}
