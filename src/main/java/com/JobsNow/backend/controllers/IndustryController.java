package com.JobsNow.backend.controllers;

import com.JobsNow.backend.request.CreateIndustryRequest;
import com.JobsNow.backend.request.UpdateIndustryRequest;
import com.JobsNow.backend.response.ResponseFactory;
import com.JobsNow.backend.service.IndustryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/industry")
@RequiredArgsConstructor
public class IndustryController {
    private final IndustryService industryService;

    @GetMapping("/all")
    public ResponseEntity<?> getAllIndustries() {
        return ResponseFactory.success(industryService.getAllIndustries());
    }

    @PostMapping("/add")
    public ResponseEntity<?> addIndustry(@Valid @RequestBody CreateIndustryRequest request) {
        industryService.addIndustry(request);
        return ResponseFactory.successMessage("Industry added successfully");
    }

    @PutMapping("/update")
    public ResponseEntity<?> updateIndustry(@RequestBody UpdateIndustryRequest request) {
        industryService.updateIndustry(request);
        return ResponseFactory.successMessage("Industry updated successfully");
    }

    @DeleteMapping("/delete/{industryId}")
    public ResponseEntity<?> deleteIndustry(@PathVariable Integer industryId) {
        industryService.deleteIndustry(industryId);
        return ResponseFactory.successMessage("Industry deleted successfully");
    }
}
