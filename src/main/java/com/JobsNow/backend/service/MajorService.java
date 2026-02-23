package com.JobsNow.backend.service;

import com.JobsNow.backend.entity.Major;

import java.util.List;

public interface MajorService {
    List<Major> getAllMajors();
    void addMajor(String majorName);
    void deleteMajor(Integer majorId);
}
