package com.JobsNow.backend.service;

import com.JobsNow.backend.dto.CertificateDTO;
import com.JobsNow.backend.request.CertificateRequest;

import java.util.List;

public interface CertificateService {
    List<CertificateDTO> getByResumeId(Integer resumeId);
    CertificateDTO create(Integer resumeId, CertificateRequest request);
    CertificateDTO update(Integer resumeId, Integer id, CertificateRequest request);
    void delete(Integer resumeId, Integer id);
}
