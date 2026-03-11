package com.JobsNow.backend.mapper;

import com.JobsNow.backend.dto.CertificateDTO;
import com.JobsNow.backend.entity.Certificate;
import org.springframework.stereotype.Component;

@Component
public class CertificateMapper {
    public static CertificateDTO toDTO(Certificate entity) {
        if (entity == null) return null;
        return CertificateDTO.builder()
                .id(entity.getId())
                .title(entity.getTitle())
                .issueDate(entity.getIssueDate())
                .description(entity.getDescription())
                .sortOrder(entity.getSortOrder())
                .build();
    }
}
