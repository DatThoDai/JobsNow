package com.JobsNow.backend.controllers;

import com.JobsNow.backend.response.ResponseFactory;
import com.JobsNow.backend.service.MajorService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/major")
@RequiredArgsConstructor
public class MajorController {
    private final MajorService majorService;

    @GetMapping
    public ResponseEntity<?> getAllMajors() {
        return ResponseFactory.success(majorService.getAllMajors());
    }

    @PostMapping
    public ResponseEntity<?> addMajor(String majorName) {
        majorService.addMajor(majorName);
        return ResponseFactory.successMessage("Major added successfully");
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteMajor(@PathVariable Integer id) {
        majorService.deleteMajor(id);
        return ResponseFactory.successMessage("Major deleted successfully");
    }
}
