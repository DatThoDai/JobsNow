package com.JobsNow.backend.service.imp;

import com.JobsNow.backend.dto.CompanyDTO;
import com.JobsNow.backend.dto.CompanyImageDTO;
import com.JobsNow.backend.entity.Company;
import com.JobsNow.backend.entity.CompanyFollower;
import com.JobsNow.backend.entity.CompanyImage;
import com.JobsNow.backend.entity.CompanyPost;
import com.JobsNow.backend.entity.CompanyReview;
import com.JobsNow.backend.entity.Industry;
import com.JobsNow.backend.entity.Job;
import com.JobsNow.backend.entity.JobViewEvent;
import com.JobsNow.backend.entity.Social;
import com.JobsNow.backend.entity.User;
import com.JobsNow.backend.entity.enums.ApplicationStatus;
import com.JobsNow.backend.entity.enums.CompanyImageType;
import com.JobsNow.backend.entity.enums.CompanyPostStatus;
import com.JobsNow.backend.entity.enums.CompanyReviewStatus;
import com.JobsNow.backend.entity.enums.SocialPlatform;
import com.JobsNow.backend.exception.BadRequestException;
import com.JobsNow.backend.exception.NotFoundException;
import com.JobsNow.backend.mapper.CompanyMapper;
import com.JobsNow.backend.repositories.CompanyImageRepository;
import com.JobsNow.backend.repositories.CompanyFollowerRepository;
import com.JobsNow.backend.repositories.CompanyPostRepository;
import com.JobsNow.backend.repositories.CompanyRepository;
import com.JobsNow.backend.repositories.CompanyReviewRepository;
import com.JobsNow.backend.repositories.ApplicationRepository;
import com.JobsNow.backend.repositories.IndustryRepository;
import com.JobsNow.backend.repositories.JobRepository;
import com.JobsNow.backend.repositories.JobViewEventRepository;
import com.JobsNow.backend.repositories.UserRepository;
import com.JobsNow.backend.request.CreateCompanyRequest;
import com.JobsNow.backend.request.SocialLinkItem;
import com.JobsNow.backend.request.UpdateCompanyRequest;
import com.JobsNow.backend.response.CompanyDashboardMetricsResponse;
import com.JobsNow.backend.service.AwsS3Service;
import com.JobsNow.backend.service.CompanyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CompanyServiceImpl implements CompanyService {
    private final CompanyRepository companyRepository;
    private final CompanyImageRepository companyImageRepository;
    private final CompanyFollowerRepository companyFollowerRepository;
    private final CompanyPostRepository companyPostRepository;
    private final CompanyReviewRepository companyReviewRepository;
    private final ApplicationRepository applicationRepository;
    private final JobRepository jobRepository;
    private final JobViewEventRepository jobViewEventRepository;
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
        company.setCreatedAt(LocalDateTime.now());
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

    @Override
    public CompanyDashboardMetricsResponse getMyDashboardMetrics(
            String email,
            String preset,
            String from,
            String to,
            String timezone,
            boolean comparePrevious
    ) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("User not found"));
        Company company = companyRepository.findByUser_UserId(user.getUserId())
                .orElseThrow(() -> new NotFoundException("Company not found"));

        ZoneId zoneId = resolveZoneId(timezone);
        TimeRange currentRange = resolveRange(preset, from, to, zoneId);
        TimeRange previousRange = comparePrevious ? buildPreviousRange(currentRange) : null;
        BucketType bucketType = resolveBucketType(currentRange, preset);

        long followersCurrent = companyFollowerRepository.countByCompanyCompanyIdAndCreatedAtBetween(
                company.getCompanyId(),
                currentRange.start.toLocalDateTime(),
                currentRange.end.toLocalDateTime()
        );
        long followersPrevious = previousRange == null ? 0 : companyFollowerRepository.countByCompanyCompanyIdAndCreatedAtBetween(
                company.getCompanyId(),
                previousRange.start.toLocalDateTime(),
                previousRange.end.toLocalDateTime()
        );

        long reviewsCurrent = companyReviewRepository.countByCompanyCompanyIdAndStatusAndCreatedAtBetween(
                company.getCompanyId(),
                CompanyReviewStatus.APPROVED,
                currentRange.start.toLocalDateTime(),
                currentRange.end.toLocalDateTime()
        );
        long reviewsPrevious = previousRange == null ? 0 : companyReviewRepository.countByCompanyCompanyIdAndStatusAndCreatedAtBetween(
                company.getCompanyId(),
                CompanyReviewStatus.APPROVED,
                previousRange.start.toLocalDateTime(),
                previousRange.end.toLocalDateTime()
        );

        Double avgRatingCurrentRaw = companyReviewRepository.getAverageRatingByCompanyIdAndStatusAndCreatedAtBetween(
                company.getCompanyId(),
                CompanyReviewStatus.APPROVED,
                currentRange.start.toLocalDateTime(),
                currentRange.end.toLocalDateTime()
        );
        Double avgRatingPreviousRaw = previousRange == null ? 0d : companyReviewRepository.getAverageRatingByCompanyIdAndStatusAndCreatedAtBetween(
                company.getCompanyId(),
                CompanyReviewStatus.APPROVED,
                previousRange.start.toLocalDateTime(),
                previousRange.end.toLocalDateTime()
        );
        long avgRatingCurrentScaled = Math.round((avgRatingCurrentRaw == null ? 0d : avgRatingCurrentRaw));
        long avgRatingPreviousScaled = Math.round((avgRatingPreviousRaw == null ? 0d : avgRatingPreviousRaw));

        long applicationsCurrent = nullToZero(applicationRepository.countByJob_Company_CompanyIdAndAppliedAtBetween(
                company.getCompanyId(),
                currentRange.start.toLocalDateTime(),
                currentRange.end.toLocalDateTime()
        ));
        long applicationsPrevious = previousRange == null ? 0 : nullToZero(applicationRepository.countByJob_Company_CompanyIdAndAppliedAtBetween(
                company.getCompanyId(),
                previousRange.start.toLocalDateTime(),
                previousRange.end.toLocalDateTime()
        ));

        long approvedPostsCurrent = companyPostRepository.countByCompany_CompanyIdAndStatusAndPublishedAtBetween(
                company.getCompanyId(),
                CompanyPostStatus.PUBLISHED,
                currentRange.start.toLocalDateTime(),
                currentRange.end.toLocalDateTime()
        );
        long approvedPostsPrevious = previousRange == null ? 0 : companyPostRepository.countByCompany_CompanyIdAndStatusAndPublishedAtBetween(
                company.getCompanyId(),
                CompanyPostStatus.PUBLISHED,
                previousRange.start.toLocalDateTime(),
                previousRange.end.toLocalDateTime()
        );

        long jobViewsCurrent = jobViewEventRepository.countByCompanyInRange(
                company.getCompanyId(),
                currentRange.start.toLocalDateTime(),
                currentRange.end.toLocalDateTime()
        );
        long jobViewsPrevious = previousRange == null ? 0 : jobViewEventRepository.countByCompanyInRange(
                company.getCompanyId(),
                previousRange.start.toLocalDateTime(),
                previousRange.end.toLocalDateTime()
        );
        long totalJobApplies = applicationsCurrent;
        long totalJobAppliesPrevious = applicationsPrevious;

        List<Job> companyJobs = jobRepository.findByCompany_CompanyId(company.getCompanyId());

        List<CompanyDashboardMetricsResponse.RatingDistributionItem> ratingDistribution = buildRatingDistribution(
                company.getCompanyId(),
                currentRange
        );
        List<CompanyDashboardMetricsResponse.StatusCountItem> applicationPipeline = buildApplicationPipeline(
                company.getCompanyId(),
                currentRange
        );
        List<CompanyDashboardMetricsResponse.StatusCountItem> postStatus = buildPostStatus(company.getCompanyId());
        List<CompanyDashboardMetricsResponse.TrendPoint> trend = buildTrend(
                company.getCompanyId(),
                currentRange,
                previousRange,
                bucketType,
                comparePrevious
        );
        Map<Integer, Long> viewCountByJobId = new HashMap<>();
        for (Object[] row : jobViewEventRepository.countViewsByJobInRange(
                company.getCompanyId(),
                currentRange.start.toLocalDateTime(),
                currentRange.end.toLocalDateTime()
        )) {
            Integer jobId = row[0] == null ? null : ((Number) row[0]).intValue();
            long views = row[1] == null ? 0L : ((Number) row[1]).longValue();
            if (jobId != null) {
                viewCountByJobId.put(jobId, views);
            }
        }
        Map<Integer, Long> applyCountByJobId = new HashMap<>();
        for (Object[] row : applicationRepository.countAppliesByJobInRange(
                company.getCompanyId(),
                currentRange.start.toLocalDateTime(),
                currentRange.end.toLocalDateTime()
        )) {
            Integer jobId = row[0] == null ? null : ((Number) row[0]).intValue();
            long applies = row[1] == null ? 0L : ((Number) row[1]).longValue();
            if (jobId != null) {
                applyCountByJobId.put(jobId, applies);
            }
        }
        List<CompanyDashboardMetricsResponse.TopJobItem> topJobs = companyJobs.stream()
                .sorted(Comparator.comparingLong(
                                (Job j) -> applyCountByJobId.getOrDefault(j.getJobId(), 0L) * 10L
                                        + viewCountByJobId.getOrDefault(j.getJobId(), 0L)
                        ).reversed())
                .limit(8)
                .map(job -> toTopJobItem(
                        job,
                        viewCountByJobId.getOrDefault(job.getJobId(), 0L),
                        applyCountByJobId.getOrDefault(job.getJobId(), 0L)
                ))
                .collect(Collectors.toList());

        return CompanyDashboardMetricsResponse.builder()
                .range(CompanyDashboardMetricsResponse.RangeInfo.builder()
                        .preset(normalizePreset(preset).name().toLowerCase(Locale.ROOT))
                        .bucket(bucketType.name().toLowerCase(Locale.ROOT))
                        .timezone(zoneId.getId())
                        .from(currentRange.start.toOffsetDateTime().toString())
                        .to(currentRange.end.toOffsetDateTime().toString())
                        .build())
                .kpis(CompanyDashboardMetricsResponse.KpiBlock.builder()
                        .followers(kpiValue(followersCurrent, followersPrevious, comparePrevious))
                        .reviews(kpiValue(reviewsCurrent, reviewsPrevious, comparePrevious))
                        .approvedPosts(kpiValue(approvedPostsCurrent, approvedPostsPrevious, comparePrevious))
                        .applications(kpiValue(applicationsCurrent, applicationsPrevious, comparePrevious))
                        .avgRatingX100(kpiValue(avgRatingCurrentScaled, avgRatingPreviousScaled, comparePrevious))
                        .jobViews(kpiValue(jobViewsCurrent, jobViewsPrevious, comparePrevious))
                        .jobApplies(kpiValue(totalJobApplies, totalJobAppliesPrevious, comparePrevious))
                        .build())
                .trend(trend)
                .ratingDistribution(ratingDistribution)
                .applicationPipeline(applicationPipeline)
                .postStatus(postStatus)
                .topJobs(topJobs)
                .build();
    }

    private CompanyDashboardMetricsResponse.KpiValue kpiValue(long current, long previous, boolean comparePrevious) {
        return CompanyDashboardMetricsResponse.KpiValue.builder()
                .value(current)
                .deltaPercent(comparePrevious ? calculateDeltaPercent(current, previous) : null)
                .build();
    }

    private List<CompanyDashboardMetricsResponse.RatingDistributionItem> buildRatingDistribution(
            Integer companyId,
            TimeRange currentRange
    ) {
        Map<Integer, Long> counts = new HashMap<>();
        for (Object[] row : companyReviewRepository.countByRatingInRange(
                companyId,
                CompanyReviewStatus.APPROVED,
                currentRange.start.toLocalDateTime(),
                currentRange.end.toLocalDateTime()
        )) {
            Integer rating = row[0] == null ? 0 : ((Number) row[0]).intValue();
            Long count = row[1] == null ? 0L : ((Number) row[1]).longValue();
            counts.put(rating, count);
        }

        List<CompanyDashboardMetricsResponse.RatingDistributionItem> out = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            out.add(CompanyDashboardMetricsResponse.RatingDistributionItem.builder()
                    .star(i)
                    .count(counts.getOrDefault(i, 0L))
                    .build());
        }
        return out;
    }

    private List<CompanyDashboardMetricsResponse.StatusCountItem> buildApplicationPipeline(
            Integer companyId,
            TimeRange currentRange
    ) {
        List<ApplicationStatus> order = List.of(
                ApplicationStatus.PENDING,
                ApplicationStatus.REVIEWING,
                ApplicationStatus.SHORTLISTED,
                ApplicationStatus.INTERVIEWING,
                ApplicationStatus.HIRED,
                ApplicationStatus.REJECTED
        );
        Map<ApplicationStatus, Long> counts = new EnumMap<>(ApplicationStatus.class);
        for (Object[] row : applicationRepository.countByCompanyAndStatusInRange(
                companyId,
                currentRange.start.toLocalDateTime(),
                currentRange.end.toLocalDateTime()
        )) {
            ApplicationStatus status = row[0] == null ? null : (ApplicationStatus) row[0];
            if (status != null) {
                counts.put(status, ((Number) row[1]).longValue());
            }
        }

        List<CompanyDashboardMetricsResponse.StatusCountItem> out = new ArrayList<>();
        for (ApplicationStatus status : order) {
            out.add(CompanyDashboardMetricsResponse.StatusCountItem.builder()
                    .status(status.name())
                    .count(counts.getOrDefault(status, 0L))
                    .build());
        }
        return out;
    }

    private List<CompanyDashboardMetricsResponse.StatusCountItem> buildPostStatus(Integer companyId) {
        List<CompanyPostStatus> statuses = List.of(
                CompanyPostStatus.DRAFT,
                CompanyPostStatus.PENDING_REVIEW,
                CompanyPostStatus.PUBLISHED,
                CompanyPostStatus.REJECTED
        );
        Map<CompanyPostStatus, Long> counts = new EnumMap<>(CompanyPostStatus.class);
        for (Object[] row : companyPostRepository.countByStatusForCompany(companyId, statuses)) {
            CompanyPostStatus status = row[0] == null ? null : (CompanyPostStatus) row[0];
            if (status != null) {
                counts.put(status, ((Number) row[1]).longValue());
            }
        }

        List<CompanyDashboardMetricsResponse.StatusCountItem> out = new ArrayList<>();
        for (CompanyPostStatus status : statuses) {
            out.add(CompanyDashboardMetricsResponse.StatusCountItem.builder()
                    .status(status.name())
                    .count(counts.getOrDefault(status, 0L))
                    .build());
        }
        return out;
    }

    private List<CompanyDashboardMetricsResponse.TrendPoint> buildTrend(
            Integer companyId,
            TimeRange currentRange,
            TimeRange previousRange,
            BucketType bucketType,
            boolean comparePrevious
    ) {
        List<TimeBucket> currentBuckets = buildBuckets(currentRange, bucketType);
        List<TimeBucket> previousBuckets = previousRange == null ? List.of() : buildBuckets(previousRange, bucketType);

        List<CompanyFollower> followerCurrent = companyFollowerRepository.findByCompanyCompanyIdAndCreatedAtBetweenOrderByCreatedAtAsc(
                companyId,
                currentRange.start.toLocalDateTime(),
                currentRange.end.toLocalDateTime()
        );
        List<com.JobsNow.backend.entity.Application> applicationCurrent = applicationRepository.findByJob_Company_CompanyIdAndAppliedAtBetween(
                companyId,
                currentRange.start.toLocalDateTime(),
                currentRange.end.toLocalDateTime()
        );
        List<CompanyReview> reviewCurrent = companyReviewRepository.findByCompanyCompanyIdAndStatusAndCreatedAtBetweenOrderByCreatedAtAsc(
                companyId,
                CompanyReviewStatus.APPROVED,
                currentRange.start.toLocalDateTime(),
                currentRange.end.toLocalDateTime()
        );
        List<CompanyPost> approvedPostCurrent = companyPostRepository.findByCompany_CompanyIdAndStatusAndPublishedAtBetweenOrderByPublishedAtAsc(
                companyId,
                CompanyPostStatus.PUBLISHED,
                currentRange.start.toLocalDateTime(),
                currentRange.end.toLocalDateTime()
        );
        List<JobViewEvent> jobViewCurrent = jobViewEventRepository.findByJob_Company_CompanyIdAndViewedAtBetweenOrderByViewedAtAsc(
                companyId,
                currentRange.start.toLocalDateTime(),
                currentRange.end.toLocalDateTime()
        );
        List<CompanyFollower> followerPrevious = previousRange == null ? List.of() :
                companyFollowerRepository.findByCompanyCompanyIdAndCreatedAtBetweenOrderByCreatedAtAsc(
                        companyId,
                        previousRange.start.toLocalDateTime(),
                        previousRange.end.toLocalDateTime()
                );
        List<com.JobsNow.backend.entity.Application> applicationPrevious = previousRange == null ? List.of() :
                applicationRepository.findByJob_Company_CompanyIdAndAppliedAtBetween(
                        companyId,
                        previousRange.start.toLocalDateTime(),
                        previousRange.end.toLocalDateTime()
                );
        List<CompanyReview> reviewPrevious = previousRange == null ? List.of() :
                companyReviewRepository.findByCompanyCompanyIdAndStatusAndCreatedAtBetweenOrderByCreatedAtAsc(
                        companyId,
                        CompanyReviewStatus.APPROVED,
                        previousRange.start.toLocalDateTime(),
                        previousRange.end.toLocalDateTime()
                );
        List<CompanyPost> approvedPostPrevious = previousRange == null ? List.of() :
                companyPostRepository.findByCompany_CompanyIdAndStatusAndPublishedAtBetweenOrderByPublishedAtAsc(
                        companyId,
                        CompanyPostStatus.PUBLISHED,
                        previousRange.start.toLocalDateTime(),
                        previousRange.end.toLocalDateTime()
                );
        List<JobViewEvent> jobViewPrevious = previousRange == null ? List.of() :
                jobViewEventRepository.findByJob_Company_CompanyIdAndViewedAtBetweenOrderByViewedAtAsc(
                        companyId,
                        previousRange.start.toLocalDateTime(),
                        previousRange.end.toLocalDateTime()
                );

        long[] currentFollowerCounts = new long[currentBuckets.size()];
        long[] currentApplicationCounts = new long[currentBuckets.size()];
        long[] currentReviewCounts = new long[currentBuckets.size()];
        long[] currentApprovedPostCounts = new long[currentBuckets.size()];
        long[] currentJobViewCounts = new long[currentBuckets.size()];
        long[] currentJobApplyCounts = new long[currentBuckets.size()];
        long[] currentAvgRating = new long[currentBuckets.size()];
        long[] currentReviewRatingSum = new long[currentBuckets.size()];
        long[] currentReviewRatingCount = new long[currentBuckets.size()];
        long[] previousFollowerCounts = new long[currentBuckets.size()];
        long[] previousApplicationCounts = new long[currentBuckets.size()];
        long[] previousReviewCounts = new long[currentBuckets.size()];
        long[] previousApprovedPostCounts = new long[currentBuckets.size()];
        long[] previousJobViewCounts = new long[currentBuckets.size()];
        long[] previousJobApplyCounts = new long[currentBuckets.size()];
        long[] previousAvgRating = new long[currentBuckets.size()];
        long[] previousReviewRatingSum = new long[currentBuckets.size()];
        long[] previousReviewRatingCount = new long[currentBuckets.size()];

        for (CompanyFollower follower : followerCurrent) {
            int idx = findBucketIndex(currentBuckets, follower.getCreatedAt().atZone(currentRange.start.getZone()));
            if (idx >= 0) currentFollowerCounts[idx]++;
        }
        for (com.JobsNow.backend.entity.Application app : applicationCurrent) {
            int idx = findBucketIndex(currentBuckets, app.getAppliedAt().atZone(currentRange.start.getZone()));
            if (idx >= 0) {
                currentApplicationCounts[idx]++;
                currentJobApplyCounts[idx]++;
            }
        }
        for (CompanyReview review : reviewCurrent) {
            int idx = findBucketIndex(currentBuckets, review.getCreatedAt().atZone(currentRange.start.getZone()));
            if (idx >= 0) {
                currentReviewCounts[idx]++;
                currentReviewRatingSum[idx] += review.getRating() == null ? 0 : review.getRating();
                currentReviewRatingCount[idx]++;
            }
        }
        for (CompanyPost post : approvedPostCurrent) {
            if (post.getPublishedAt() == null) continue;
            int idx = findBucketIndex(currentBuckets, post.getPublishedAt().atZone(currentRange.start.getZone()));
            if (idx >= 0) currentApprovedPostCounts[idx]++;
        }
        for (JobViewEvent viewEvent : jobViewCurrent) {
            int idx = findBucketIndex(currentBuckets, viewEvent.getViewedAt().atZone(currentRange.start.getZone()));
            if (idx >= 0) currentJobViewCounts[idx]++;
        }
        for (int i = 0; i < currentBuckets.size(); i++) {
            currentAvgRating[i] = currentReviewRatingCount[i] <= 0
                    ? 0
                    : Math.round(currentReviewRatingSum[i] / currentReviewRatingCount[i]);
        }
        if (comparePrevious) {
            for (CompanyFollower follower : followerPrevious) {
                int idx = findBucketIndex(previousBuckets, follower.getCreatedAt().atZone(previousRange.start.getZone()));
                if (idx >= 0 && idx < previousFollowerCounts.length) previousFollowerCounts[idx]++;
            }
            for (com.JobsNow.backend.entity.Application app : applicationPrevious) {
                int idx = findBucketIndex(previousBuckets, app.getAppliedAt().atZone(previousRange.start.getZone()));
                if (idx >= 0 && idx < previousApplicationCounts.length) {
                    previousApplicationCounts[idx]++;
                    previousJobApplyCounts[idx]++;
                }
            }
            for (CompanyReview review : reviewPrevious) {
                int idx = findBucketIndex(previousBuckets, review.getCreatedAt().atZone(previousRange.start.getZone()));
                if (idx >= 0 && idx < previousReviewCounts.length) {
                    previousReviewCounts[idx]++;
                    previousReviewRatingSum[idx] += review.getRating() == null ? 0 : review.getRating();
                    previousReviewRatingCount[idx]++;
                }
            }
            for (CompanyPost post : approvedPostPrevious) {
                if (post.getPublishedAt() == null) continue;
                int idx = findBucketIndex(previousBuckets, post.getPublishedAt().atZone(previousRange.start.getZone()));
                if (idx >= 0 && idx < previousApprovedPostCounts.length) previousApprovedPostCounts[idx]++;
            }
            for (JobViewEvent viewEvent : jobViewPrevious) {
                int idx = findBucketIndex(previousBuckets, viewEvent.getViewedAt().atZone(previousRange.start.getZone()));
                if (idx >= 0 && idx < previousJobViewCounts.length) previousJobViewCounts[idx]++;
            }
            for (int i = 0; i < previousBuckets.size() && i < previousAvgRating.length; i++) {
                previousAvgRating[i] = previousReviewRatingCount[i] <= 0
                        ? 0
                        : Math.round(previousReviewRatingSum[i] / previousReviewRatingCount[i]);
            }
        }

        List<CompanyDashboardMetricsResponse.TrendPoint> out = new ArrayList<>();
        for (int i = 0; i < currentBuckets.size(); i++) {
            out.add(CompanyDashboardMetricsResponse.TrendPoint.builder()
                    .label(currentBuckets.get(i).label)
                    .currentFollowers(currentFollowerCounts[i])
                    .previousFollowers(comparePrevious ? previousFollowerCounts[i] : 0)
                    .currentApplications(currentApplicationCounts[i])
                    .previousApplications(comparePrevious ? previousApplicationCounts[i] : 0)
                    .currentReviews(currentReviewCounts[i])
                    .previousReviews(comparePrevious ? previousReviewCounts[i] : 0)
                    .currentApprovedPosts(currentApprovedPostCounts[i])
                    .previousApprovedPosts(comparePrevious ? previousApprovedPostCounts[i] : 0)
                    .currentJobViews(currentJobViewCounts[i])
                    .previousJobViews(comparePrevious ? previousJobViewCounts[i] : 0)
                    .currentJobApplies(currentJobApplyCounts[i])
                    .previousJobApplies(comparePrevious ? previousJobApplyCounts[i] : 0)
                    .currentAvgRating(currentAvgRating[i])
                    .previousAvgRating(comparePrevious ? previousAvgRating[i] : 0)
                    .build());
        }
        return out;
    }

    private CompanyDashboardMetricsResponse.TopJobItem toTopJobItem(Job job, long views, long applies) {
        double conversion = views <= 0 ? 0d : (applies * 100d) / views;
        return CompanyDashboardMetricsResponse.TopJobItem.builder()
                .jobId(job.getJobId())
                .title(job.getTitle())
                .viewCount(views)
                .applyCount(applies)
                .conversionRate(Math.round(conversion * 100d) / 100d)
                .active(Boolean.TRUE.equals(job.getIsActive()))
                .approved(Boolean.TRUE.equals(job.getIsApproved()))
                .build();
    }

    private ZoneId resolveZoneId(String timezone) {
        String value = (timezone == null || timezone.isBlank()) ? "Asia/Ho_Chi_Minh" : timezone.trim();
        try {
            return ZoneId.of(value);
        } catch (Exception ex) {
            return ZoneId.of("Asia/Ho_Chi_Minh");
        }
    }

    private TimeRange resolveRange(String preset, String from, String to, ZoneId zoneId) {
        PresetType presetType = normalizePreset(preset);
        ZonedDateTime now = ZonedDateTime.now(zoneId);
        return switch (presetType) {
            case DAY -> new TimeRange(
                    now.toLocalDate().atStartOfDay(zoneId),
                    now.toLocalDate().plusDays(1).atStartOfDay(zoneId).minusNanos(1)
            );
            case MONTH -> {
                LocalDate firstDay = now.toLocalDate().withDayOfMonth(1);
                LocalDate nextMonthDay = firstDay.plusMonths(1);
                yield new TimeRange(firstDay.atStartOfDay(zoneId), nextMonthDay.atStartOfDay(zoneId).minusNanos(1));
            }
            case YEAR -> {
                LocalDate firstDay = now.toLocalDate().withDayOfYear(1);
                LocalDate nextYearDay = firstDay.plusYears(1);
                yield new TimeRange(firstDay.atStartOfDay(zoneId), nextYearDay.atStartOfDay(zoneId).minusNanos(1));
            }
            case CUSTOM -> {
                if (from == null || from.isBlank() || to == null || to.isBlank()) {
                    throw new BadRequestException("from and to are required for custom preset");
                }
                LocalDate startDate = parseDate(from);
                LocalDate endDate = parseDate(to);
                if (endDate.isBefore(startDate)) {
                    throw new BadRequestException("to must be greater than or equal to from");
                }
                long days = endDate.toEpochDay() - startDate.toEpochDay() + 1;
                if (days > 366) {
                    throw new BadRequestException("custom date range cannot exceed 366 days");
                }
                yield new TimeRange(
                        startDate.atStartOfDay(zoneId),
                        endDate.plusDays(1).atStartOfDay(zoneId).minusNanos(1)
                );
            }
        };
    }

    private PresetType normalizePreset(String preset) {
        if (preset == null || preset.isBlank()) return PresetType.MONTH;
        try {
            return PresetType.valueOf(preset.trim().toUpperCase(Locale.ROOT));
        } catch (Exception ex) {
            throw new BadRequestException("Invalid preset. Allowed: day, month, year, custom");
        }
    }

    private LocalDate parseDate(String value) {
        try {
            return LocalDate.parse(value, DateTimeFormatter.ISO_LOCAL_DATE);
        } catch (DateTimeParseException ex) {
            throw new BadRequestException("Invalid date format. Expected yyyy-MM-dd");
        }
    }

    private TimeRange buildPreviousRange(TimeRange current) {
        long nanos = Math.max(1, current.nanos());
        ZonedDateTime previousEnd = current.start.minusNanos(1);
        ZonedDateTime previousStart = previousEnd.minusNanos(nanos - 1);
        return new TimeRange(previousStart, previousEnd);
    }

    private BucketType resolveBucketType(TimeRange range, String presetRaw) {
        PresetType preset = normalizePreset(presetRaw);
        if (preset == PresetType.DAY) return BucketType.HOUR;
        if (preset == PresetType.MONTH) return BucketType.DAY;
        if (preset == PresetType.YEAR) return BucketType.MONTH;
        long days = range.daySpan();
        if (days <= 31) return BucketType.DAY;
        if (days <= 180) return BucketType.WEEK;
        return BucketType.MONTH;
    }

    private List<TimeBucket> buildBuckets(TimeRange range, BucketType bucketType) {
        List<TimeBucket> buckets = new ArrayList<>();
        ZoneId zoneId = range.start.getZone();
        switch (bucketType) {
            case HOUR -> {
                ZonedDateTime cursor = range.start;
                for (int i = 0; i < 24; i++) {
                    ZonedDateTime next = cursor.plusHours(1);
                    buckets.add(new TimeBucket(cursor, next.minusNanos(1), cursor.format(DateTimeFormatter.ofPattern("HH:mm"))));
                    cursor = next;
                }
            }
            case DAY -> {
                ZonedDateTime cursor = range.start.toLocalDate().atStartOfDay(zoneId);
                ZonedDateTime endExclusive = range.end.plusNanos(1);
                while (cursor.isBefore(endExclusive)) {
                    ZonedDateTime next = cursor.plusDays(1);
                    if (next.isAfter(endExclusive)) next = endExclusive;
                    buckets.add(new TimeBucket(cursor, next.minusNanos(1), cursor.format(DateTimeFormatter.ofPattern("dd/MM"))));
                    cursor = next;
                }
            }
            case WEEK -> {
                ZonedDateTime cursor = range.start.toLocalDate().atStartOfDay(zoneId);
                ZonedDateTime endExclusive = range.end.plusNanos(1);
                int index = 1;
                while (cursor.isBefore(endExclusive)) {
                    ZonedDateTime next = cursor.plusDays(7);
                    if (next.isAfter(endExclusive)) next = endExclusive;
                    buckets.add(new TimeBucket(cursor, next.minusNanos(1), "W" + index));
                    cursor = next;
                    index++;
                }
            }
            case MONTH -> {
                YearMonth startYm = YearMonth.from(range.start);
                YearMonth endYm = YearMonth.from(range.end);
                YearMonth cursor = startYm;
                while (!cursor.isAfter(endYm)) {
                    ZonedDateTime start = cursor.atDay(1).atStartOfDay(zoneId);
                    ZonedDateTime nextStart = cursor.plusMonths(1).atDay(1).atStartOfDay(zoneId);
                    ZonedDateTime end = nextStart.minusNanos(1);
                    if (end.isAfter(range.end)) end = range.end;
                    if (start.isBefore(range.start)) start = range.start;
                    buckets.add(new TimeBucket(start, end, "T" + cursor.getMonthValue()));
                    cursor = cursor.plusMonths(1);
                }
            }
        }
        return buckets;
    }

    private int findBucketIndex(List<TimeBucket> buckets, ZonedDateTime value) {
        for (int i = 0; i < buckets.size(); i++) {
            TimeBucket bucket = buckets.get(i);
            if (!value.isBefore(bucket.start) && !value.isAfter(bucket.end)) return i;
        }
        return -1;
    }

    private Double calculateDeltaPercent(long current, long previous) {
        if (previous <= 0) return current <= 0 ? 0d : 100d;
        return Math.round((((current - previous) * 100d) / previous) * 100d) / 100d;
    }

    private long nvlInt(Integer value) {
        return value == null ? 0 : value.longValue();
    }

    private long nullToZero(Long value) {
        return value == null ? 0 : value;
    }

    private enum PresetType {
        DAY,
        MONTH,
        YEAR,
        CUSTOM
    }

    private enum BucketType {
        HOUR,
        DAY,
        WEEK,
        MONTH
    }

    private record TimeRange(ZonedDateTime start, ZonedDateTime end) {
        long daySpan() {
            return java.time.Duration.between(start, end).toDays() + 1;
        }

        long nanos() {
            return java.time.Duration.between(start, end).toNanos() + 1;
        }
    }

    private record TimeBucket(ZonedDateTime start, ZonedDateTime end, String label) {}
}
