package com.JobsNow.backend.service;

import com.JobsNow.backend.request.CreateCompanyPostRequest;
import com.JobsNow.backend.request.RejectCompanyPostRequest;
import com.JobsNow.backend.request.UpdateCompanyPostRequest;
import com.JobsNow.backend.response.*;

public interface CompanyPostService {

    CompanyPostMinePageResponse getMyPosts(String email, int page, int limit);

    CompanyPostMineResponse getMyPost(String email, Integer postId);

    CompanyPostMineResponse createPost(String email, CreateCompanyPostRequest request);

    CompanyPostMineResponse updatePost(String email, Integer postId, UpdateCompanyPostRequest request);

    CompanyPostMineResponse submitPost(String email, Integer postId);

    void trashMyPost(String email, Integer postId);

    CompanyPostAdminPageResponse getPendingForAdmin(int page, int limit);

    void approvePost(Integer postId);

    void rejectPost(Integer postId, RejectCompanyPostRequest request);

    void trashPostByAdmin(Integer postId);

    java.util.List<HandbookPostResponse> getHandbookFeatured(int limit);

    java.util.List<HandbookPostResponse> getHandbookExplore(int limit);

    HandbookPageResponse getHandbookPublishedPage(String categoryKey, int page, int size);

    HandbookPostDetailResponse getHandbookPublishedBySlug(String slug);
}
