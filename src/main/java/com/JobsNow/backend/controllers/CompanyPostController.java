package com.JobsNow.backend.controllers;

import com.JobsNow.backend.request.CreateCompanyPostRequest;
import com.JobsNow.backend.request.RejectCompanyPostRequest;
import com.JobsNow.backend.request.UpdateCompanyPostRequest;
import com.JobsNow.backend.response.ResponseFactory;
import com.JobsNow.backend.service.CompanyPostService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/company")
@RequiredArgsConstructor
public class CompanyPostController {

    private final CompanyPostService companyPostService;

    @GetMapping("/me/posts/{postId}")
    public ResponseEntity<?> myPost(Authentication auth, @PathVariable Integer postId) {
        return ResponseFactory.success(companyPostService.getMyPost(auth.getName(), postId));
    }

    @GetMapping("/me/posts")
    public ResponseEntity<?> myPosts(
            Authentication auth,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int limit
    ) {
        return ResponseFactory.success(companyPostService.getMyPosts(auth.getName(), page, limit));
    }

    @PostMapping("/me/posts")
    public ResponseEntity<?> create(Authentication auth, @RequestBody CreateCompanyPostRequest request) {
        return ResponseFactory.success(companyPostService.createPost(auth.getName(), request));
    }

    @PutMapping("/me/posts/{postId}")
    public ResponseEntity<?> update(
            Authentication auth,
            @PathVariable Integer postId,
            @RequestBody UpdateCompanyPostRequest request
    ) {
        return ResponseFactory.success(companyPostService.updatePost(auth.getName(), postId, request));
    }

    @PutMapping("/me/posts/{postId}/submit")
    public ResponseEntity<?> submit(Authentication auth, @PathVariable Integer postId) {
        return ResponseFactory.success(companyPostService.submitPost(auth.getName(), postId));
    }

    @PutMapping("/me/posts/{postId}/trash")
    public ResponseEntity<?> trashMine(Authentication auth, @PathVariable Integer postId) {
        companyPostService.trashMyPost(auth.getName(), postId);
        return ResponseFactory.successMessage("Post removed");
    }

    @GetMapping("/admin/posts/pending")
    public ResponseEntity<?> pending(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int limit
    ) {
        return ResponseFactory.success(companyPostService.getPendingForAdmin(page, limit));
    }

    @PutMapping("/admin/posts/{postId}/approve")
    public ResponseEntity<?> approve(@PathVariable Integer postId) {
        companyPostService.approvePost(postId);
        return ResponseFactory.successMessage("Post approved");
    }

    @PutMapping("/admin/posts/{postId}/reject")
    public ResponseEntity<?> reject(@PathVariable Integer postId, @RequestBody RejectCompanyPostRequest request) {
        companyPostService.rejectPost(postId, request);
        return ResponseFactory.successMessage("Post rejected");
    }

    @PutMapping("/admin/posts/{postId}/trash")
    public ResponseEntity<?> trashAdmin(@PathVariable Integer postId) {
        companyPostService.trashPostByAdmin(postId);
        return ResponseFactory.successMessage("Post removed");
    }
}
