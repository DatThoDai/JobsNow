package com.JobsNow.backend.service.imp;

import com.JobsNow.backend.dto.EducationDTO;
import com.JobsNow.backend.entity.Education;
import com.JobsNow.backend.entity.Major;
import com.JobsNow.backend.entity.Resume;
import com.JobsNow.backend.exception.BadRequestException;
import com.JobsNow.backend.exception.NotFoundException;
import com.JobsNow.backend.mapper.EducationMapper;
import com.JobsNow.backend.repositories.EducationRepository;
import com.JobsNow.backend.repositories.MajorRepository;
import com.JobsNow.backend.repositories.ResumeRepository;
import com.JobsNow.backend.request.EducationRequest;
import com.JobsNow.backend.service.EducationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EducationServiceImpl implements EducationService {
    private final EducationRepository educationRepository;
    private final MajorRepository majorRepository;
    private final ResumeRepository resumeRepository;

    @Override
    public List<EducationDTO> getByResumeId(Integer resumeId) {
        Resume resume = resumeRepository.findById(resumeId)
                .orElseThrow(() -> new NotFoundException("Resume not found"));
        return educationRepository.findByResume_ResumeIdOrderBySortOrderAsc(resume.getResumeId())
                .stream()
                .map(EducationMapper::toDTO)
                .toList();
    }

    @Override
    @Transactional
    public EducationDTO create(Integer resumeId, EducationRequest request) {
        Resume resume = resumeRepository.findById(resumeId)
                .orElseThrow(() -> new NotFoundException("Resume not found"));
        var educationLevel = parseEducationLevel(request.getEducationLevel());
        Major major = request.getMajorId() != null
                ? majorRepository.findById(request.getMajorId()).orElse(null)
                : null;
        int sortOrder = request.getSortOrder() != null ? request.getSortOrder() : nextSortOrder(resumeId);
        Education entity = Education.builder()
                .jobSeekerProfile(resume.getJobSeekerProfile())
                .resume(resume)
                .title(request.getTitle())
                .educationLevel(educationLevel)
                .major(major)
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .description(request.getDescription())
                .sortOrder(sortOrder)
                .build();
        entity = educationRepository.save(entity);
        return EducationMapper.toDTO(entity);
    }

    @Override
    @Transactional
    public EducationDTO update(Integer resumeId, Integer id, EducationRequest request) {
        Education entity = educationRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Education not found"));
        if (entity.getResume() == null || !entity.getResume().getResumeId().equals(resumeId)) {
            throw new BadRequestException("Education does not belong to this resume");
        }
        entity.setTitle(request.getTitle());
        entity.setEducationLevel(parseEducationLevel(request.getEducationLevel()));
        entity.setMajor(request.getMajorId() != null
                ? majorRepository.findById(request.getMajorId()).orElse(null)
                : null);
        entity.setStartDate(request.getStartDate());
        entity.setEndDate(request.getEndDate());
        entity.setDescription(request.getDescription());
        if (request.getSortOrder() != null) entity.setSortOrder(request.getSortOrder());
        entity = educationRepository.save(entity);
        return EducationMapper.toDTO(entity);
    }

    @Override
    @Transactional
    public void delete(Integer resumeId, Integer id) {
        Education entity = educationRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Education not found"));
        if (entity.getResume() == null || !entity.getResume().getResumeId().equals(resumeId)) {
            throw new BadRequestException("Education does not belong to this resume");
        }
        educationRepository.delete(entity);
    }

    private com.JobsNow.backend.entity.enums.EducationLevel parseEducationLevel(String value) {
        if (value == null || value.isBlank()) throw new BadRequestException("Education level is required");
        try {
            return com.JobsNow.backend.entity.enums.EducationLevel.valueOf(value.toUpperCase().trim());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid education level. Allowed: HIGH_SCHOOL, VOCATIONAL, ASSOCIATE, BACHELOR, MASTER, DOCTORATE, OTHER");
        }
    }

    private int nextSortOrder(Integer resumeId) {
        List<Education> list = educationRepository.findByResume_ResumeIdOrderBySortOrderAsc(resumeId);
        if (list.isEmpty()) return 0;
        Integer last = list.get(list.size() - 1).getSortOrder();
        return last != null ? last + 1 : 0;
    }
}
