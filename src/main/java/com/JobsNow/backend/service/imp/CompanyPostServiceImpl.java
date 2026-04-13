package com.JobsNow.backend.service.imp;

import com.JobsNow.backend.constants.JobsNowConstant;
import com.JobsNow.backend.entity.Company;
import com.JobsNow.backend.entity.CompanyPost;
import com.JobsNow.backend.entity.User;
import com.JobsNow.backend.entity.enums.CompanyPostStatus;
import com.JobsNow.backend.exception.BadRequestException;
import com.JobsNow.backend.exception.NotFoundException;
import com.JobsNow.backend.repositories.CompanyPostRepository;
import com.JobsNow.backend.repositories.CompanyRepository;
import com.JobsNow.backend.repositories.UserRepository;
import com.JobsNow.backend.request.CreateCompanyPostRequest;
import com.JobsNow.backend.request.CreateNotificationRequest;
import com.JobsNow.backend.request.RejectCompanyPostRequest;
import com.JobsNow.backend.request.UpdateCompanyPostRequest;
import com.JobsNow.backend.response.CompanyPostAdminItemResponse;
import com.JobsNow.backend.response.CompanyPostAdminPageResponse;
import com.JobsNow.backend.response.CompanyPostMinePageResponse;
import com.JobsNow.backend.response.CompanyPostMineResponse;
import com.JobsNow.backend.response.HandbookPageResponse;
import com.JobsNow.backend.response.HandbookPostDetailResponse;
import com.JobsNow.backend.response.HandbookPostResponse;
import com.JobsNow.backend.response.NotificationResponse;
import com.JobsNow.backend.service.CompanyPostService;
import com.JobsNow.backend.service.NotificationService;
import com.JobsNow.backend.util.SlugUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class CompanyPostServiceImpl implements CompanyPostService {

    private final CompanyPostRepository companyPostRepository;
    private final CompanyRepository companyRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final SimpMessagingTemplate messagingTemplate;

    private static final List<CompanyPostStatus> RECRUITER_LIST_STATUSES = List.of(
            CompanyPostStatus.DRAFT,
            CompanyPostStatus.PENDING_REVIEW,
            CompanyPostStatus.PUBLISHED,
            CompanyPostStatus.REJECTED
    );

    private Company requireCompanyForRecruiter(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("User not found"));
        return companyRepository.findByUser_UserId(user.getUserId())
                .orElseThrow(() -> new BadRequestException("Company profile not found"));
    }

    private CompanyPost requireOwnedPost(Company company, Integer postId) {
        CompanyPost post = companyPostRepository.findById(postId)
                .orElseThrow(() -> new NotFoundException("Post not found"));
        if (!post.getCompany().getCompanyId().equals(company.getCompanyId())) {
            throw new BadRequestException("Not allowed");
        }
        return post;
    }

    @Override
    @Transactional(readOnly = true)
    public CompanyPostMineResponse getMyPost(String email, Integer postId) {
        Company company = requireCompanyForRecruiter(email);
        CompanyPost post = requireOwnedPost(company, postId);
        if (post.getStatus() == CompanyPostStatus.TRASHED) {
            throw new NotFoundException("Post not found");
        }
        return toMine(post);
    }

    @Override
    @Transactional(readOnly = true)
    public CompanyPostMinePageResponse getMyPosts(String email, int page, int limit) {
        Company company = requireCompanyForRecruiter(email);
        int safePage = Math.max(page, 1);
        int safeLimit = Math.max(Math.min(limit, 50), 1);
        Page<CompanyPost> pg = companyPostRepository.findByCompany_CompanyIdAndStatusInOrderByUpdatedAtDesc(
                company.getCompanyId(),
                RECRUITER_LIST_STATUSES,
                PageRequest.of(safePage - 1, safeLimit)
        );
        return CompanyPostMinePageResponse.builder()
                .items(pg.getContent().stream().map(this::toMine).toList())
                .totalCount(pg.getTotalElements())
                .page(safePage)
                .limit(safeLimit)
                .hasNext(pg.hasNext())
                .build();
    }

    @Override
    @Transactional
    public CompanyPostMineResponse createPost(String email, CreateCompanyPostRequest request) {
        Company company = requireCompanyForRecruiter(email);
        validateDraftContent(request.getTitle(), request.getCategoryKey());
        String baseSlug = request.getSlug() != null && !request.getSlug().isBlank()
                ? SlugUtil.slugify(request.getSlug())
                : SlugUtil.slugify(request.getTitle());
        String slug = uniqueSlug(baseSlug);
        LocalDateTime now = LocalDateTime.now();
        CompanyPost post = CompanyPost.builder()
                .company(company)
                .title(request.getTitle().trim())
                .slug(slug)
                .excerpt(trimToNull(request.getExcerpt()))
                .content(trimToNull(request.getContent()))
                .featuredImageUrl(trimToNull(request.getFeaturedImageUrl()))
                .categoryKey(request.getCategoryKey().trim())
                .status(CompanyPostStatus.DRAFT)
                .rejectionNote(null)
                .createdAt(now)
                .updatedAt(now)
                .build();
        companyPostRepository.save(post);
        return toMine(post);
    }

    @Override
    @Transactional
    public CompanyPostMineResponse updatePost(String email, Integer postId, UpdateCompanyPostRequest request) {
        Company company = requireCompanyForRecruiter(email);
        CompanyPost post = requireOwnedPost(company, postId);
        if (post.getStatus() != CompanyPostStatus.DRAFT
                && post.getStatus() != CompanyPostStatus.REJECTED
                && post.getStatus() != CompanyPostStatus.PENDING_REVIEW) {
            throw new BadRequestException("Only draft, rejected, or pending review posts can be edited");
        }
        validateDraftContent(request.getTitle(), request.getCategoryKey());
        if (request.getSlug() != null && !request.getSlug().isBlank()) {
            String newSlug = SlugUtil.slugify(request.getSlug());
            if (!newSlug.equals(post.getSlug()) && companyPostRepository.findBySlug(newSlug).isPresent()) {
                throw new BadRequestException("Slug already in use");
            }
            post.setSlug(newSlug);
        }
        post.setTitle(request.getTitle().trim());
        post.setExcerpt(trimToNull(request.getExcerpt()));
        post.setContent(trimToNull(request.getContent()));
        post.setFeaturedImageUrl(trimToNull(request.getFeaturedImageUrl()));
        post.setCategoryKey(request.getCategoryKey().trim());
        post.setUpdatedAt(LocalDateTime.now());
        companyPostRepository.save(post);
        return toMine(post);
    }

    @Override
    @Transactional
    public CompanyPostMineResponse submitPost(String email, Integer postId) {
        Company company = requireCompanyForRecruiter(email);
        CompanyPost post = requireOwnedPost(company, postId);
        if (post.getStatus() != CompanyPostStatus.DRAFT && post.getStatus() != CompanyPostStatus.REJECTED) {
            throw new BadRequestException("Invalid status for submit");
        }
        if (post.getTitle() == null || post.getTitle().isBlank()
                || post.getContent() == null || post.getContent().isBlank()) {
            throw new BadRequestException("Title and content are required to submit");
        }
        post.setStatus(CompanyPostStatus.PENDING_REVIEW);
        post.setRejectionNote(null);
        post.setRejectedAt(null);
        post.setUpdatedAt(LocalDateTime.now());
        companyPostRepository.save(post);
        return toMine(post);
    }

    @Override
    @Transactional
    public void trashMyPost(String email, Integer postId) {
        Company company = requireCompanyForRecruiter(email);
        CompanyPost post = requireOwnedPost(company, postId);
        post.setStatus(CompanyPostStatus.TRASHED);
        post.setUpdatedAt(LocalDateTime.now());
        companyPostRepository.save(post);
    }

    @Override
    @Transactional(readOnly = true)
    public CompanyPostAdminPageResponse getPendingForAdmin(int page, int limit) {
        int safePage = Math.max(page, 1);
        int safeLimit = Math.max(Math.min(limit, 50), 1);
        Page<CompanyPost> pg = companyPostRepository.findByStatusOrderByCreatedAtDesc(
                CompanyPostStatus.PENDING_REVIEW,
                PageRequest.of(safePage - 1, safeLimit)
        );
        return CompanyPostAdminPageResponse.builder()
                .items(pg.getContent().stream().map(this::toAdminItem).toList())
                .totalCount(pg.getTotalElements())
                .page(safePage)
                .limit(safeLimit)
                .hasNext(pg.hasNext())
                .build();
    }

    @Override
    @Transactional
    public void approvePost(Integer postId) {
        CompanyPost post = companyPostRepository.findById(postId)
                .orElseThrow(() -> new NotFoundException("Post not found"));
        if (post.getStatus() != CompanyPostStatus.PENDING_REVIEW) {
            throw new BadRequestException("Post is not pending review");
        }
        post.setStatus(CompanyPostStatus.PUBLISHED);
        post.setPublishedAt(LocalDateTime.now());
        post.setRejectionNote(null);
        post.setRejectedAt(null);
        post.setUpdatedAt(LocalDateTime.now());
        companyPostRepository.save(post);
    }

    @Override
    @Transactional
    public void rejectPost(Integer postId, RejectCompanyPostRequest request) {
        if (request == null || request.getRejectionNote() == null || request.getRejectionNote().isBlank()) {
            throw new BadRequestException("rejectionNote is required");
        }
        CompanyPost post = companyPostRepository.findById(postId)
                .orElseThrow(() -> new NotFoundException("Post not found"));
        if (post.getStatus() != CompanyPostStatus.PENDING_REVIEW) {
            throw new BadRequestException("Post is not pending review");
        }
        String note = request.getRejectionNote().trim();
        post.setStatus(CompanyPostStatus.REJECTED);
        post.setRejectionNote(note);
        post.setRejectedAt(LocalDateTime.now());
        post.setUpdatedAt(LocalDateTime.now());
        companyPostRepository.save(post);

        Company company = post.getCompany();
        User owner = company.getUser();
        String content = String.format(
                Locale.ROOT,
                "Bài viết \"%s\" bị từ chối duyệt. Lý do: %s",
                truncate(post.getTitle(), 80),
                truncate(note, 400)
        );
        CreateNotificationRequest noti = CreateNotificationRequest.builder()
                .userId(owner.getUserId())
                .applicationId(null)
                .content(content)
                .type("COMPANY_POST")
                .build();
        NotificationResponse nr = notificationService.createNotification(noti);
        messagingTemplate.convertAndSend(
                JobsNowConstant.WS_TOPIC_NOTIFICATION + owner.getUserId(),
                nr
        );
    }

    @Override
    @Transactional
    public void trashPostByAdmin(Integer postId) {
        CompanyPost post = companyPostRepository.findById(postId)
                .orElseThrow(() -> new NotFoundException("Post not found"));
        post.setStatus(CompanyPostStatus.TRASHED);
        post.setUpdatedAt(LocalDateTime.now());
        companyPostRepository.save(post);
    }

    @Override
    @Transactional(readOnly = true)
    public List<HandbookPostResponse> getHandbookFeatured(int limit) {
        int safe = Math.min(Math.max(limit, 1), 24);
        List<CompanyPost> list = companyPostRepository.findByStatusOrderByPublishedAtDesc(
                CompanyPostStatus.PUBLISHED,
                PageRequest.of(0, Math.max(safe, 12))
        ).getContent();
        return list.stream().limit(safe).map(this::toHandbookCard).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<HandbookPostResponse> getHandbookExplore(int limit) {
        int safe = Math.min(Math.max(limit, 1), 24);
        Page<CompanyPost> page = companyPostRepository.findByStatusOrderByPublishedAtDesc(
                CompanyPostStatus.PUBLISHED,
                PageRequest.of(0, 200)
        );
        List<CompanyPost> content = new ArrayList<>(page.getContent());
        Collections.shuffle(content);
        return content.stream().limit(safe).map(this::toHandbookCard).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public HandbookPageResponse getHandbookPublishedPage(String categoryKey, int page, int size) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), 50);
        Page<CompanyPost> pg;
        if (categoryKey == null || categoryKey.isBlank()) {
            pg = companyPostRepository.findByStatusOrderByPublishedAtDesc(
                    CompanyPostStatus.PUBLISHED,
                    PageRequest.of(safePage - 1, safeSize)
            );
        } else {
            pg = companyPostRepository.findByStatusAndCategoryKeyOrderByPublishedAtDesc(
                    CompanyPostStatus.PUBLISHED,
                    categoryKey.trim(),
                    PageRequest.of(safePage - 1, safeSize)
            );
        }
        return HandbookPageResponse.builder()
                .items(pg.getContent().stream().map(this::toHandbookCard).toList())
                .totalCount(pg.getTotalElements())
                .page(safePage)
                .size(safeSize)
                .hasNext(pg.hasNext())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public HandbookPostDetailResponse getHandbookPublishedBySlug(String slug) {
        CompanyPost post = companyPostRepository.findBySlugAndStatus(slug, CompanyPostStatus.PUBLISHED)
                .orElseThrow(() -> new NotFoundException("Post not found"));
        return toHandbookDetail(post);
    }

    private void validateDraftContent(String title, String categoryKey) {
        if (title == null || title.isBlank()) {
            throw new BadRequestException("Title is required");
        }
        if (categoryKey == null || categoryKey.isBlank()) {
            throw new BadRequestException("categoryKey is required");
        }
    }

    private String uniqueSlug(String base) {
        String s = base;
        int i = 0;
        while (companyPostRepository.findBySlug(s).isPresent()) {
            i++;
            s = base + "-" + i;
        }
        return s;
    }

    private static String trimToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private static String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max) + "…";
    }

    private CompanyPostMineResponse toMine(CompanyPost p) {
        return CompanyPostMineResponse.builder()
                .postId(p.getPostId())
                .title(p.getTitle())
                .slug(p.getSlug())
                .excerpt(p.getExcerpt())
                .content(p.getContent())
                .featuredImageUrl(p.getFeaturedImageUrl())
                .categoryKey(p.getCategoryKey())
                .status(p.getStatus())
                .rejectionNote(p.getRejectionNote())
                .publishedAt(p.getPublishedAt())
                .createdAt(p.getCreatedAt())
                .updatedAt(p.getUpdatedAt())
                .build();
    }

    private CompanyPostAdminItemResponse toAdminItem(CompanyPost p) {
        Company c = p.getCompany();
        return CompanyPostAdminItemResponse.builder()
                .postId(p.getPostId())
                .companyId(c.getCompanyId())
                .companyName(c.getCompanyName())
                .title(p.getTitle())
                .slug(p.getSlug())
                .excerpt(p.getExcerpt())
                .categoryKey(p.getCategoryKey())
                .createdAt(p.getCreatedAt())
                .build();
    }

    private HandbookPostResponse toHandbookCard(CompanyPost p) {
        Company c = p.getCompany();
        return HandbookPostResponse.builder()
                .postId(p.getPostId())
                .title(p.getTitle())
                .slug(p.getSlug())
                .excerpt(p.getExcerpt())
                .featuredImageUrl(p.getFeaturedImageUrl())
                .categoryKey(p.getCategoryKey())
                .companyName(c.getCompanyName())
                .companyLogoUrl(c.getLogoUrl())
                .publishedAt(p.getPublishedAt())
                .build();
    }

    private HandbookPostDetailResponse toHandbookDetail(CompanyPost p) {
        Company c = p.getCompany();
        return HandbookPostDetailResponse.builder()
                .postId(p.getPostId())
                .title(p.getTitle())
                .slug(p.getSlug())
                .excerpt(p.getExcerpt())
                .content(p.getContent())
                .featuredImageUrl(p.getFeaturedImageUrl())
                .categoryKey(p.getCategoryKey())
                .companyName(c.getCompanyName())
                .companyLogoUrl(c.getLogoUrl())
                .publishedAt(p.getPublishedAt())
                .build();
    }
}
