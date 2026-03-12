package com.JobsNow.backend.service.imp;

import com.JobsNow.backend.dto.WorkExperienceDTO;
import com.JobsNow.backend.entity.Resume;
import com.JobsNow.backend.entity.WorkExperience;
import com.JobsNow.backend.entity.enums.WorkExperienceLevel;
import com.JobsNow.backend.exception.BadRequestException;
import com.JobsNow.backend.exception.NotFoundException;
import com.JobsNow.backend.mapper.WorkExperienceMapper;
import com.JobsNow.backend.repositories.ResumeRepository;
import com.JobsNow.backend.repositories.WorkExperienceRepository;
import com.JobsNow.backend.request.WorkExperienceRequest;
import com.JobsNow.backend.service.WorkExperienceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class WorkExperienceServiceImpl implements WorkExperienceService {
    private final WorkExperienceRepository workExperienceRepository;
    private final ResumeRepository resumeRepository;

    @Override
    public List<WorkExperienceDTO> getByResumeId(Integer resumeId) {
        Resume resume = resumeRepository.findById(resumeId)
                .orElseThrow(() -> new NotFoundException("Resume not found"));
        return workExperienceRepository
                .findByResume_ResumeIdOrderBySortOrderAsc(resume.getResumeId())
                .stream()
                .map(WorkExperienceMapper::toDTO)
                .toList();
    }

    @Override
    @Transactional
    public WorkExperienceDTO create(Integer resumeId, WorkExperienceRequest request) {
        Resume resume = resumeRepository.findById(resumeId)
                .orElseThrow(() -> new NotFoundException("Resume not found"));
        WorkExperienceLevel level = parseLevel(request.getLevel());
        int sortOrder = request.getSortOrder() != null
                ? request.getSortOrder()
                : nextSortOrder(resumeId);
        WorkExperience entity = WorkExperience.builder()
                .jobSeekerProfile(resume.getJobSeekerProfile())
                .resume(resume)
                .title(request.getTitle())
                .level(level)
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .description(request.getDescription())
                .sortOrder(sortOrder)
                .build();
        entity = workExperienceRepository.save(entity);
        return WorkExperienceMapper.toDTO(entity);
    }

    @Override
    @Transactional
    public WorkExperienceDTO update(Integer resumeId, Integer id, WorkExperienceRequest request) {
        WorkExperience entity = workExperienceRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Work experience not found"));
        if (entity.getResume() == null || !entity.getResume().getResumeId().equals(resumeId)) {
            throw new BadRequestException("Work experience does not belong to this resume");
        }
        entity.setTitle(request.getTitle());
        entity.setLevel(parseLevel(request.getLevel()));
        entity.setStartDate(request.getStartDate());
        entity.setEndDate(request.getEndDate());
        entity.setDescription(request.getDescription());
        if (request.getSortOrder() != null) {
            entity.setSortOrder(request.getSortOrder());
        }
        entity = workExperienceRepository.save(entity);
        return WorkExperienceMapper.toDTO(entity);
    }

    @Override
    @Transactional
    public void delete(Integer resumeId, Integer id) {
        WorkExperience entity = workExperienceRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Work experience not found"));
        if (entity.getResume() == null || !entity.getResume().getResumeId().equals(resumeId)) {
            throw new BadRequestException("Work experience does not belong to this resume");
        }
        workExperienceRepository.delete(entity);
    }

    private WorkExperienceLevel parseLevel(String level) {
        if (level == null || level.isBlank()) throw new BadRequestException("Level is required");
        try {
            return WorkExperienceLevel.valueOf(level.toUpperCase().trim());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid level. Allowed: INTERN, FRESHER, JUNIOR, MIDDLE, SENIOR, LEAD, OTHER");
        }
    }

    private int nextSortOrder(Integer resumeId) {
        List<WorkExperience> list = workExperienceRepository.findByResume_ResumeIdOrderBySortOrderAsc(resumeId);
        if (list.isEmpty()) return 0;
        Integer last = list.get(list.size() - 1).getSortOrder();
        return last != null ? last + 1 : 0;
    }
}
