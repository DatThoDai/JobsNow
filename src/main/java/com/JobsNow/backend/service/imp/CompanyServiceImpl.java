package com.JobsNow.backend.service.imp;

import com.JobsNow.backend.dto.CompanyDTO;
import com.JobsNow.backend.dto.CompanyImageDTO;
import com.JobsNow.backend.entity.Company;
import com.JobsNow.backend.entity.CompanyImage;
import com.JobsNow.backend.entity.enums.CompanyImageType;
import com.JobsNow.backend.exception.BadRequestException;
import com.JobsNow.backend.exception.NotFoundException;
import com.JobsNow.backend.mapper.CompanyMapper;
import com.JobsNow.backend.repositories.CompanyImageRepository;
import com.JobsNow.backend.repositories.CompanyRepository;
import com.JobsNow.backend.request.UpdateCompanyRequest;
import com.JobsNow.backend.service.AwsS3Service;
import com.JobsNow.backend.service.CompanyService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CompanyServiceImpl implements CompanyService {
    private final CompanyRepository companyRepository;
    private final CompanyImageRepository companyImageRepository;
    private final AwsS3Service awsS3Service;
    @Override
    public List<CompanyDTO> getAllCompanies() {
        List<Company> companies = companyRepository.findAll();
        return companies.stream()
                .filter(c -> Boolean.TRUE.equals(c.getIsVerified()))
                .map(CompanyMapper::toCompanyDTO)
                .collect(Collectors.toList());
    }

    @Override
    public CompanyDTO getCompanyById(Integer companyId) {
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new NotFoundException("Company not found"));
        return CompanyMapper.toCompanyDTO(company);
    }

    @Override
    public List<CompanyDTO> findCompanyByIndustryOrCompanyName(Integer industryId, String companyName) {
        List<Company> companies = companyRepository.findByIndustryOrCompanyName(industryId, companyName);
        return companies.stream()
                .map(CompanyMapper::toCompanyDTO)
                .collect(Collectors.toList());
    }
    @Override
    public void updateCompany(Integer companyId, UpdateCompanyRequest request) {
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new NotFoundException("Company not found"));
        if (request.getCompanyName() != null && !request.getCompanyName().isEmpty()) {
            if(companyRepository.existsByCompanyName(request.getCompanyName())){
                throw new BadRequestException("Company name already exists");
            }
            company.setCompanyName(request.getCompanyName());
        }
        if(request.getWebsite() != null){
            company.setWebsite(request.getWebsite());
        }
        if(request.getDescription() != null){
            company.setDescription(request.getDescription());
        }
        if(request.getSlogan() != null){
            company.setSlogan(request.getSlogan());
        }
        companyRepository.save(company);
    }

    @Override
    public void uploadLogo(Integer companyId, MultipartFile logoFile) {
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new NotFoundException("Company not found"));
        if(logoFile == null || logoFile.isEmpty()){
            throw new BadRequestException("Logo file is required");
        }
        try {
            String originalFileName = logoFile.getOriginalFilename();
            String extension = originalFileName.substring(originalFileName.lastIndexOf("."));
            String baseName = originalFileName.substring(0, originalFileName.lastIndexOf("."));
            String s3Key = "logos/" + baseName + "_" + System.currentTimeMillis() + extension;
            String logoUrl = awsS3Service.uploadFileToS3(logoFile.getInputStream(), s3Key, logoFile.getContentType());
            company.setLogoUrl(logoUrl);
            companyRepository.save(company);
        }catch (Exception e){
            throw new BadRequestException("Failed to upload logo");
        }
    }

    @Override
    public void uploadBanner(Integer companyId, MultipartFile bannerFile) {
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new NotFoundException("Company not found"));
        if(bannerFile == null || bannerFile.isEmpty()){
            throw new BadRequestException("Banner file is required");
        }
        try {
            String originalFileName = bannerFile.getOriginalFilename();
            String extension = originalFileName.substring(originalFileName.lastIndexOf("."));
            String baseName = originalFileName.substring(0, originalFileName.lastIndexOf("."));
            String s3Key = "banners/" + baseName + "_" + System.currentTimeMillis() + extension;
            String bannerUrl = awsS3Service.uploadFileToS3(bannerFile.getInputStream(), s3Key, bannerFile.getContentType());
            company.setBannerUrl(bannerUrl);
            companyRepository.save(company);
        }catch (Exception e){
            throw new BadRequestException("Failed to upload banner");
        }
    }

    @Override
    public List<CompanyImageDTO> addCompanyImage(Integer companyId, MultipartFile imageFile, String type) {
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new NotFoundException("Company not found"));
        CompanyImageType imageType;
        try {
            imageType = CompanyImageType.valueOf(type.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid image type. Allowed: OFFICE, TEAM, EVENT, PRODUCT, OTHER");
        }
        int currentCount = companyImageRepository.countByCompany_CompanyId(companyId);
        if(currentCount >= 10) {
            throw new BadRequestException("Maximum of 5 images allowed per company");
        }
        try {
            String originalFileName = imageFile.getOriginalFilename();
            String extension = originalFileName.substring(originalFileName.lastIndexOf("."));
            String baseName = originalFileName.substring(0, originalFileName.lastIndexOf("."));
            String s3Key = "company-images/" + baseName + "_" + System.currentTimeMillis() + extension;
            String imageUrl = awsS3Service.uploadFileToS3(imageFile.getInputStream(), s3Key, imageFile.getContentType());
            CompanyImage image = CompanyImage.builder()
                    .company(company)
                    .imageUrl(imageUrl)
                    .imageType(imageType)
                    .build();
            companyImageRepository.save(image);
        }catch (Exception e){
            throw new BadRequestException("Failed to upload company image");
        }
        return getCompanyImages(companyId);
    }

    @Override
    public void deleteCompanyImage(Integer imageId) {
        CompanyImage image = companyImageRepository.findById(imageId)
                .orElseThrow(() -> new NotFoundException("Company image not found"));
        companyImageRepository.delete(image);
    }

    @Override
    public List<CompanyImageDTO> getCompanyImages(Integer companyId) {
        return companyImageRepository.findByCompany_CompanyId(companyId).stream()
                .map(img -> CompanyImageDTO.builder()
                        .imageId(img.getImageId())
                        .imageUrl(img.getImageUrl())
                        .type(img.getImageType().name())
                        .build())
                .collect(Collectors.toList());
    }
}
