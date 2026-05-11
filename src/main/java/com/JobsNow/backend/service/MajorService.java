package com.JobsNow.backend.service;

import com.JobsNow.backend.entity.Major;
import com.JobsNow.backend.response.PagedResponse;

import java.util.List;

public interface MajorService {
    List<Major> getAllMajors();

    PagedResponse<Major> getMajorsPage(int page, int limit);
    void addMajor(String majorName);
    void deleteMajor(Integer majorId);
    void updateMajor(Integer majorId, String newName);
}
