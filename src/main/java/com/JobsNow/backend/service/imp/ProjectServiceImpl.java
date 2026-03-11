package com.JobsNow.backend.service.imp;

import com.JobsNow.backend.dto.ProjectDTO;
import com.JobsNow.backend.entity.Project;
import com.JobsNow.backend.entity.Resume;
import com.JobsNow.backend.exception.BadRequestException;
import com.JobsNow.backend.exception.NotFoundException;
import com.JobsNow.backend.mapper.ProjectMapper;
import com.JobsNow.backend.repositories.ProjectRepository;
import com.JobsNow.backend.repositories.ResumeRepository;
import com.JobsNow.backend.request.ProjectRequest;
import com.JobsNow.backend.service.ProjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProjectServiceImpl implements ProjectService {
    private final ProjectRepository projectRepository;
    private final ResumeRepository resumeRepository;

    @Override
    public List<ProjectDTO> getByResumeId(Integer resumeId) {
        Resume resume = resumeRepository.findById(resumeId)
                .orElseThrow(() -> new NotFoundException("Resume not found"));
        return projectRepository.findByResume_ResumeIdOrderBySortOrderAsc(resume.getResumeId())
                .stream()
                .map(ProjectMapper::toDTO)
                .toList();
    }

    @Override
    @Transactional
    public ProjectDTO create(Integer resumeId, ProjectRequest request) {
        Resume resume = resumeRepository.findById(resumeId)
                .orElseThrow(() -> new NotFoundException("Resume not found"));
        int sortOrder = request.getSortOrder() != null ? request.getSortOrder() : nextSortOrder(resumeId);
        Project entity = Project.builder()
                .jobSeekerProfile(resume.getJobSeekerProfile())
                .resume(resume)
                .title(request.getTitle())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .description(request.getDescription())
                .sortOrder(sortOrder)
                .build();
        entity = projectRepository.save(entity);
        return ProjectMapper.toDTO(entity);
    }

    @Override
    @Transactional
    public ProjectDTO update(Integer resumeId, Integer id, ProjectRequest request) {
        Project entity = projectRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Project not found"));
        if (entity.getResume() == null || !entity.getResume().getResumeId().equals(resumeId)) {
            throw new BadRequestException("Project does not belong to this resume");
        }
        entity.setTitle(request.getTitle());
        entity.setStartDate(request.getStartDate());
        entity.setEndDate(request.getEndDate());
        entity.setDescription(request.getDescription());
        if (request.getSortOrder() != null) entity.setSortOrder(request.getSortOrder());
        entity = projectRepository.save(entity);
        return ProjectMapper.toDTO(entity);
    }

    @Override
    @Transactional
    public void delete(Integer resumeId, Integer id) {
        Project entity = projectRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Project not found"));
        if (entity.getResume() == null || !entity.getResume().getResumeId().equals(resumeId)) {
            throw new BadRequestException("Project does not belong to this resume");
        }
        projectRepository.delete(entity);
    }

    private int nextSortOrder(Integer resumeId) {
        List<Project> list = projectRepository.findByResume_ResumeIdOrderBySortOrderAsc(resumeId);
        if (list.isEmpty()) return 0;
        Integer last = list.get(list.size() - 1).getSortOrder();
        return last != null ? last + 1 : 0;
    }
}
