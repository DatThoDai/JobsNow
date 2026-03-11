package com.JobsNow.backend.service.imp;

import com.JobsNow.backend.dto.CertificateDTO;
import com.JobsNow.backend.entity.Certificate;
import com.JobsNow.backend.entity.Resume;
import com.JobsNow.backend.exception.BadRequestException;
import com.JobsNow.backend.exception.NotFoundException;
import com.JobsNow.backend.mapper.CertificateMapper;
import com.JobsNow.backend.repositories.CertificateRepository;
import com.JobsNow.backend.repositories.ResumeRepository;
import com.JobsNow.backend.request.CertificateRequest;
import com.JobsNow.backend.service.CertificateService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CertificateServiceImpl implements CertificateService {
    private final CertificateRepository certificateRepository;
    private final ResumeRepository resumeRepository;

    @Override
    public List<CertificateDTO> getByResumeId(Integer resumeId) {
        Resume resume = resumeRepository.findById(resumeId)
                .orElseThrow(() -> new NotFoundException("Resume not found"));
        return certificateRepository.findByResume_ResumeIdOrderBySortOrderAsc(resume.getResumeId())
                .stream()
                .map(CertificateMapper::toDTO)
                .toList();
    }

    @Override
    @Transactional
    public CertificateDTO create(Integer resumeId, CertificateRequest request) {
        Resume resume = resumeRepository.findById(resumeId)
                .orElseThrow(() -> new NotFoundException("Resume not found"));
        int sortOrder = request.getSortOrder() != null ? request.getSortOrder() : nextSortOrder(resumeId);
        Certificate entity = Certificate.builder()
                .jobSeekerProfile(resume.getJobSeekerProfile())
                .resume(resume)
                .title(request.getTitle())
                .issueDate(request.getIssueDate())
                .description(request.getDescription())
                .sortOrder(sortOrder)
                .build();
        entity = certificateRepository.save(entity);
        return CertificateMapper.toDTO(entity);
    }

    @Override
    @Transactional
    public CertificateDTO update(Integer resumeId, Integer id, CertificateRequest request) {
        Certificate entity = certificateRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Certificate not found"));
        if (entity.getResume() == null || !entity.getResume().getResumeId().equals(resumeId)) {
            throw new BadRequestException("Certificate does not belong to this resume");
        }
        entity.setTitle(request.getTitle());
        entity.setIssueDate(request.getIssueDate());
        entity.setDescription(request.getDescription());
        if (request.getSortOrder() != null) entity.setSortOrder(request.getSortOrder());
        entity = certificateRepository.save(entity);
        return CertificateMapper.toDTO(entity);
    }

    @Override
    @Transactional
    public void delete(Integer resumeId, Integer id) {
        Certificate entity = certificateRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Certificate not found"));
        if (entity.getResume() == null || !entity.getResume().getResumeId().equals(resumeId)) {
            throw new BadRequestException("Certificate does not belong to this resume");
        }
        certificateRepository.delete(entity);
    }

    private int nextSortOrder(Integer resumeId) {
        List<Certificate> list = certificateRepository.findByResume_ResumeIdOrderBySortOrderAsc(resumeId);
        if (list.isEmpty()) return 0;
        Integer last = list.get(list.size() - 1).getSortOrder();
        return last != null ? last + 1 : 0;
    }
}
