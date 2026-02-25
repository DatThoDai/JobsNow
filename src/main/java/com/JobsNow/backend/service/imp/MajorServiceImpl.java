package com.JobsNow.backend.service.imp;

import com.JobsNow.backend.entity.Major;
import com.JobsNow.backend.exception.BadRequestException;
import com.JobsNow.backend.repositories.MajorRepository;
import com.JobsNow.backend.service.MajorService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MajorServiceImpl implements MajorService {
    private final MajorRepository majorRepository;
    @Override
    public List<Major> getAllMajors() {
        return majorRepository.findAll();
    }

    @Override
    public void addMajor(String majorName) {
        if(majorRepository.existsByName(majorName)) {
            throw new BadRequestException("Major already exists");
        }
        Major major = Major.builder()
                .name(majorName)
                .build();
        majorRepository.save(major);
    }

    @Override
    public void deleteMajor(Integer majorId) {
        if(!majorRepository.existsById(majorId)) {
            throw new BadRequestException("Major not found");
        }
        majorRepository.deleteById(majorId);
    }
}
