package com.JobsNow.backend.mapper;

import com.JobsNow.backend.dto.ResumeDTO;
import com.JobsNow.backend.entity.Resume;
import org.springframework.stereotype.Component;

@Component
public class ResumeMapper {
    public static ResumeDTO toResumeDTO(Resume resume) {
        ResumeDTO resumeDTO = new ResumeDTO();
        resumeDTO.setResumeId(resume.getResumeId());
        resumeDTO.setResumeName(resume.getResumeName());
        resumeDTO.setResumeUrl(resume.getResumeUrl());
        resumeDTO.setSummary(resume.getSummary());
        resumeDTO.setTemplateKey(resume.getTemplateKey());
        resumeDTO.setUploadedAt(resume.getUploadedAt());
        resumeDTO.setIsPrimary(resume.getIsPrimary());
        return resumeDTO;
    }
}
