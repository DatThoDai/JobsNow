package com.JobsNow.backend.controllers;

import com.JobsNow.backend.entity.Industry;
import com.JobsNow.backend.repositories.IndustryRepository;
import com.JobsNow.backend.response.ResponseFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/industry")
@RequiredArgsConstructor
public class IndustryController {
    private final IndustryRepository industryRepository;

    @GetMapping("/all")
    public ResponseEntity<?> getAllIndustries() {
        List<Industry> industries = industryRepository.findAll();
        return ResponseFactory.success(industries);
    }
}
