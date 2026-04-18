package com.JobsNow.backend.controllers;

import com.JobsNow.backend.response.ResponseFactory;
import com.JobsNow.backend.service.CompanyPostService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/handbook")
@RequiredArgsConstructor
public class HandbookController {

    private final CompanyPostService companyPostService;

    @GetMapping("/featured")
    public ResponseEntity<?> featured(@RequestParam(defaultValue = "12") int limit) {
        return ResponseFactory.success(companyPostService.getHandbookFeatured(limit));
    }

    @GetMapping("/explore")
    public ResponseEntity<?> explore(@RequestParam(defaultValue = "9") int limit) {
        return ResponseFactory.success(companyPostService.getHandbookExplore(limit));
    }

    @GetMapping
    public ResponseEntity<?> list(
            @RequestParam(required = false) String categoryKey,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "12") int size
    ) {
        return ResponseFactory.success(companyPostService.getHandbookPublishedPage(categoryKey, page, size));
    }

    @GetMapping("/{slug}")
    public ResponseEntity<?> detail(@PathVariable String slug) {
        return ResponseFactory.success(companyPostService.getHandbookPublishedBySlug(slug));
    }
}
