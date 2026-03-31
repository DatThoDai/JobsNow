package com.JobsNow.backend.service.imp;

import com.JobsNow.backend.dto.CompanyDTO;
import com.JobsNow.backend.dto.CompanyImageDTO;
import com.JobsNow.backend.entity.Company;
import com.JobsNow.backend.entity.CompanyImage;
import com.JobsNow.backend.entity.Industry;
import com.JobsNow.backend.entity.Social;
import com.JobsNow.backend.entity.User;
import com.JobsNow.backend.entity.enums.CompanyImageType;
import com.JobsNow.backend.entity.enums.SocialPlatform;
import com.JobsNow.backend.exception.BadRequestException;
import com.JobsNow.backend.exception.NotFoundException;
import com.JobsNow.backend.mapper.CompanyMapper;
import com.JobsNow.backend.repositories.CompanyImageRepository;
import com.JobsNow.backend.repositories.CompanyFollowerRepository;
import com.JobsNow.backend.repositories.CompanyRepository;
import com.JobsNow.backend.repositories.IndustryRepository;
import com.JobsNow.backend.repositories.UserRepository;
import com.JobsNow.backend.request.CreateCompanyRequest;
import com.JobsNow.backend.request.SocialLinkItem;
import com.JobsNow.backend.request.UpdateCompanyRequest;
import com.JobsNow.backend.service.AwsS3Service;
import com.JobsNow.backend.service.CompanyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CompanyServiceImpl implements CompanyService {
    private final CompanyRepository companyRepository;
    private final CompanyImageRepository companyImageRepository;
    private final CompanyFollowerRepository companyFollowerRepository;
    private final UserRepository userRepository;
    private final IndustryRepository industryRepository;
    private final AwsS3Service awsS3Service;

    @Value("${aws.s3.endpointUrl}")
    private String s3PublicBaseUrl;
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
        CompanyDTO dto = CompanyMapper.toCompanyDTO(company);
        dto.setFollowerCount(getFollowerCount(companyId));
        return dto;
    }

    @Override
    public CompanyDTO getMyCompany(String email) {
        var user = userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("User not found"));
        Company company = companyRepository.findByUser_UserId(user.getUserId())
                .orElseThrow(() -> new NotFoundException("Company not found"));
        return CompanyMapper.toCompanyDTO(company);
    }

    @Override
    public CompanyDTO createMyCompany(String email, CreateCompanyRequest request, MultipartFile logoFile) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("User not found"));
        if (companyRepository.findByUser_UserId(user.getUserId()).isPresent()) {
            throw new BadRequestException("User already has a company");
        }
        String companyName = request.getCompanyName() != null && !request.getCompanyName().isEmpty()
                ? request.getCompanyName() : request.getName();
        if (companyName == null || companyName.isEmpty()) {
            throw new BadRequestException("Company name is required");
        }
        if (companyRepository.existsByCompanyName(companyName)) {
            throw new BadRequestException("Company name already exists");
        }

        Company company = new Company();
        company.setUser(user);
        company.setCompanyName(companyName);
        company.setWebsite(request.getWebsite());
        company.setDescription(request.getDescription());
        company.setSlogan(request.getSlogan());
        company.setAddress(request.getAddress());
        company.setCompanySize(request.getCompanySize());
        company.setIsVerified(false);
        company.setJobPostCount(0);
        if (request.getNameUserContact() != null) {
            company.setNameUserContact(request.getNameUserContact());
        }
        if (request.getTutorialApply() != null) {
            company.setTutorialApply(request.getTutorialApply());
        }
        replaceCompanySocialsFromRequest(company, request.getSocials());

        List<Integer> industryIds = request.getIndustryIds() != null ? request.getIndustryIds() : Collections.emptyList();
        if (!industryIds.isEmpty()) {
            List<Industry> industryList = new ArrayList<>();
            for (Integer id : industryIds) {
                Industry industry = industryRepository.findById(id)
                        .orElseThrow(() -> new NotFoundException("Industry not found: " + id));
                industryList.add(industry);
            }
            company.setIndustries(industryList);
        }

        if (logoFile != null && !logoFile.isEmpty()) {
            try {
                String originalFileName = logoFile.getOriginalFilename();
                int dotIdx = originalFileName != null ? originalFileName.lastIndexOf(".") : -1;
                String baseName = dotIdx > 0 ? originalFileName.substring(0, dotIdx) : "logo";
                String extension = dotIdx > 0 ? originalFileName.substring(dotIdx) : "";
                String s3Key = "logos/" + baseName + "_" + System.currentTimeMillis() + extension;
                String logoUrl = awsS3Service.uploadFileToS3(logoFile.getInputStream(), s3Key, logoFile.getContentType());
                company.setLogoUrl(logoUrl);
            } catch (Exception e) {
                throw new BadRequestException("Failed to upload logo");
            }
        }

        companyRepository.save(company);

        if (request.getThumbnailImageUrls() != null && !request.getThumbnailImageUrls().isEmpty()) {
            for (String url : request.getThumbnailImageUrls()) {
                if (url != null && !url.isBlank()) {
                    addCompanyImageFromUrl(company.getCompanyId(), url.trim());
                }
            }
        }

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
    public void updateCompany(Integer companyId, UpdateCompanyRequest request, MultipartFile logoFile, MultipartFile bannerFile, List<MultipartFile> thumbnailFiles) {
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new NotFoundException("Company not found"));

        if (request.getCompanyName() != null && !request.getCompanyName().isEmpty()) {
            if (!request.getCompanyName().equalsIgnoreCase(company.getCompanyName())
                    && companyRepository.existsByCompanyName(request.getCompanyName())) {
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
        if(request.getAddress() != null){
            company.setAddress(request.getAddress());
        }
        if(request.getCompanySize() != null){
            company.setCompanySize(request.getCompanySize());
        }
        if (request.getNameUserContact() != null) {
            company.setNameUserContact(request.getNameUserContact());
        }
        if (request.getTutorialApply() != null) {
            company.setTutorialApply(request.getTutorialApply());
        }
        replaceCompanySocialsFromRequest(company, request.getSocials());
        if (request.getIndustryIds() != null && !request.getIndustryIds().isEmpty()) {
            List<Industry> industryList = new ArrayList<>();
            for (Integer id : request.getIndustryIds()) {
                Industry industry = industryRepository.findById(id)
                        .orElseThrow(() -> new NotFoundException("Industry not found: " + id));
                industryList.add(industry);
            }
            company.setIndustries(industryList);
        }
        companyRepository.save(company);

        if (logoFile != null && !logoFile.isEmpty()) {
            uploadLogo(companyId, logoFile);
        }
        if (bannerFile != null && !bannerFile.isEmpty()) {
            uploadBanner(companyId, bannerFile);
        }
        if (thumbnailFiles != null && !thumbnailFiles.isEmpty()) {
            for (MultipartFile imageFile : thumbnailFiles) {
                if (imageFile != null && !imageFile.isEmpty()) {
                    addCompanyImage(companyId, imageFile, "OTHER");
                }
            }
        }
        if (request.getThumbnailImageUrls() != null && !request.getThumbnailImageUrls().isEmpty()) {
            for (String url : request.getThumbnailImageUrls()) {
                if (url != null && !url.isBlank()) {
                    addCompanyImageFromUrl(companyId, url.trim());
                }
            }
        }
    }

    @Override
    public void deleteLogo(Integer companyId) {
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new NotFoundException("Company not found"));
        company.setLogoUrl(null);
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
    public void deleteBanner(Integer companyId) {
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new NotFoundException("Company not found"));
        company.setBannerUrl(null);
        companyRepository.save(company);
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

    private boolean isTrustedS3AssetUrl(String url) {
        if (url == null || url.isBlank() || s3PublicBaseUrl == null || s3PublicBaseUrl.isBlank()) {
            return false;
        }
        String u = url.trim();
        String base = s3PublicBaseUrl.endsWith("/")
                ? s3PublicBaseUrl.substring(0, s3PublicBaseUrl.length() - 1)
                : s3PublicBaseUrl;
        return u.startsWith(base) || u.startsWith(s3PublicBaseUrl);
    }

    private void addCompanyImageFromUrl(Integer companyId, String imageUrl) {
        if (!isTrustedS3AssetUrl(imageUrl)) {
            throw new BadRequestException("Invalid image URL");
        }
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new NotFoundException("Company not found"));
        int currentCount = companyImageRepository.countByCompany_CompanyId(companyId);
        if (currentCount >= 10) {
            throw new BadRequestException("Maximum of 5 images allowed per company");
        }
        CompanyImage image = CompanyImage.builder()
                .company(company)
                .imageUrl(imageUrl)
                .imageType(CompanyImageType.OTHER)
                .build();
        companyImageRepository.save(image);
    }

    @Override
    public void deleteCompanyImage(Integer imageId) {
        CompanyImage image = companyImageRepository.findById(imageId)
                .orElseThrow(() -> new NotFoundException("Company image not found"));
        companyImageRepository.delete(image);
    }

    private void replaceCompanySocialsFromRequest(Company company, List<SocialLinkItem> items) {
        if (items == null) {
            return;
        }
        if (company.getSocials() == null) {
            company.setSocials(new ArrayList<>());
        } else {
            company.getSocials().clear();
        }
        for (SocialLinkItem item : items) {
            if (item.getUrl() == null || item.getUrl().isBlank()) {
                continue;
            }
            SocialPlatform platform;
            try {
                platform = SocialPlatform.valueOf(item.getPlatform().toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new BadRequestException("Invalid social platform: " + item.getPlatform());
            }
            Social s = new Social();
            s.setPlatform(platform);
            s.setUrl(item.getUrl().trim());
            s.setLogoUrl(item.getLogoUrl());
            s.setCompany(company);
            company.getSocials().add(s);
        }
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

    @Override
    public Long getFollowerCount(Integer companyId) {
        return companyFollowerRepository.countByCompanyCompanyId(companyId);
    }
}
