package com.JobsNow.backend.controllers;

import com.JobsNow.backend.request.CreateCompanyRequest;
import com.JobsNow.backend.request.UpdateCompanyRequest;
import com.JobsNow.backend.response.ResponseFactory;
import com.JobsNow.backend.service.CompanyFollowerService;
import com.JobsNow.backend.service.CompanyService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/company")
@RequiredArgsConstructor
public class CompanyController {
    private final CompanyService companyService;
    private final CompanyFollowerService companyFollowerService;

    @GetMapping("/me")
    public ResponseEntity<?> getMyCompany(org.springframework.security.core.Authentication auth) {
        String email = auth.getName();
        return ResponseFactory.success(companyService.getMyCompany(email));
    }

    @PostMapping("/me")
    public ResponseEntity<?> createMyCompany(
            org.springframework.security.core.Authentication auth,
            @RequestPart("company") CreateCompanyRequest request,
            @RequestPart(value = "logoFile", required = false) MultipartFile logoFile) {
        String email = auth.getName();
        return ResponseFactory.success(companyService.createMyCompany(email, request, logoFile));
    }

    @GetMapping("/all")
    public ResponseEntity<?> getAllCompanies() {
        return ResponseFactory.success(companyService.getAllCompanies());
    }

    @GetMapping("/{companyId}")
    public ResponseEntity<?> getCompanyById(@PathVariable Integer companyId){
        return ResponseFactory.success(companyService.getCompanyById(companyId));
    }

    @GetMapping("/search")
    public ResponseEntity<?> searchCompanies(@RequestParam Integer industryId ,@RequestParam String companyName){
        return ResponseFactory.success(companyService.findCompanyByIndustryOrCompanyName(industryId, companyName));
    }

    @PostMapping("/{companyId}/logo")
    public ResponseEntity<?> uploadLogo(@PathVariable Integer companyId, @RequestParam("logo") MultipartFile logoFile){
        companyService.uploadLogo(companyId, logoFile);
        return ResponseFactory.successMessage("Logo uploaded successfully");
    }

    @DeleteMapping("/{companyId}/logo")
    public ResponseEntity<?> deleteLogo(@PathVariable Integer companyId){
        companyService.deleteLogo(companyId);
        return ResponseFactory.successMessage("Logo deleted successfully");
    }

    @PutMapping("/update/{companyId}")
    public ResponseEntity<?> updateCompany(
            @PathVariable Integer companyId,
            @RequestPart("company") UpdateCompanyRequest request,
            @RequestPart(value = "logoFile", required = false) MultipartFile logoFile,
            @RequestPart(value = "bannerFile", required = false) MultipartFile bannerFile,
            @RequestPart(value = "thumbnailFiles", required = false) List<MultipartFile> thumbnailFiles) {
        companyService.updateCompany(companyId, request, logoFile, bannerFile, thumbnailFiles);
        return ResponseFactory.successMessage("Company updated successfully");
    }

    @PostMapping("/{companyId}/banner")
    public ResponseEntity<?> uploadBanner(@PathVariable Integer companyId, @RequestParam("banner") MultipartFile bannerFile){
        companyService.uploadBanner(companyId, bannerFile);
        return ResponseFactory.successMessage("Banner uploaded successfully");
    }

    @DeleteMapping("/{companyId}/banner")
    public ResponseEntity<?> deleteBanner(@PathVariable Integer companyId){
        companyService.deleteBanner(companyId);
        return ResponseFactory.successMessage("Banner deleted successfully");
    }

    @GetMapping("/{companyId}/images")
    public ResponseEntity<?> getCompanyImages(@PathVariable Integer companyId){
        return ResponseFactory.success(companyService.getCompanyImages(companyId));
    }

    @PostMapping("/{companyId}/images")
    public ResponseEntity<?> addCompanyImage(@PathVariable Integer companyId, @RequestParam("image") MultipartFile imageFile, @RequestParam String type){
        return ResponseFactory.success(companyService.addCompanyImage(companyId, imageFile, type));
    }

    @DeleteMapping("/images/{imageId}")
    public ResponseEntity<?> deleteCompanyImage(@PathVariable Integer imageId){
        companyService.deleteCompanyImage(imageId);
        return ResponseFactory.successMessage("Company image deleted successfully");
    }

    @PostMapping("/{companyId}/follow")
    public ResponseEntity<?> followCompany(
            org.springframework.security.core.Authentication auth,
            @PathVariable Integer companyId
    ) {
        companyFollowerService.followCompany(companyId, auth.getName());
        return ResponseFactory.successMessage("Followed company successfully");
    }

    @DeleteMapping("/{companyId}/follow")
    public ResponseEntity<?> unfollowCompany(
            org.springframework.security.core.Authentication auth,
            @PathVariable Integer companyId
    ) {
        companyFollowerService.unfollowCompany(companyId, auth.getName());
        return ResponseFactory.successMessage("Unfollowed company successfully");
    }

    @GetMapping("/{companyId}/follow")
    public ResponseEntity<?> isFollowing(
            org.springframework.security.core.Authentication auth,
            @PathVariable Integer companyId
    ) {
        return ResponseFactory.success(companyFollowerService.isFollowing(companyId, auth.getName()));
    }

    @GetMapping("/{companyId}/followers")
    public ResponseEntity<?> getCompanyFollowers(
            org.springframework.security.core.Authentication auth,
            @PathVariable Integer companyId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        int safeSize = Math.min(Math.max(size, 1), 100);
        Pageable pageable = PageRequest.of(Math.max(page, 0), safeSize);
        return ResponseFactory.success(
                companyFollowerService.getFollowersForCompanyOwner(companyId, auth.getName(), pageable));
    }
}
