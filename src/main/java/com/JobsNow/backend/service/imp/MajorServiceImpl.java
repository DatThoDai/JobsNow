package com.JobsNow.backend.service.imp;

import com.JobsNow.backend.entity.Major;
import com.JobsNow.backend.exception.BadRequestException;
import com.JobsNow.backend.repositories.MajorRepository;
import com.JobsNow.backend.response.PagedResponse;
import com.JobsNow.backend.service.MajorService;
import com.JobsNow.backend.util.PagingUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
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
    public PagedResponse<Major> getMajorsPage(int page, int limit) {
        int p = PagingUtil.safePage(page);
        int lim = PagingUtil.safeLimit(limit, 100);
        Page<Major> pg = majorRepository.findAllByOrderByNameAsc(PageRequest.of(p - 1, lim));
        return PagedResponse.<Major>builder()
                .items(pg.getContent())
                .totalCount(pg.getTotalElements())
                .page(p)
                .limit(lim)
                .hasNext(pg.hasNext())
                .build();
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

    @Override
    public void updateMajor(Integer majorId, String newName) {
        Major major = majorRepository.findById(majorId)
                .orElseThrow(() -> new BadRequestException("Major not found"));

        if(majorRepository.existsByName(newName) && !newName.equals(major.getName())) {
            throw new BadRequestException("Major already exists");
        }

        major.setName(newName);
        majorRepository.save(major);
    }
}
