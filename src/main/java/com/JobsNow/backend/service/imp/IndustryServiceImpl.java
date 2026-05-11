package com.JobsNow.backend.service.imp;

import com.JobsNow.backend.dto.IndustryDTO;
import com.JobsNow.backend.entity.Industry;
import com.JobsNow.backend.exception.BadRequestException;
import com.JobsNow.backend.repositories.CompanyRepository;
import com.JobsNow.backend.repositories.IndustryRepository;
import com.JobsNow.backend.repositories.JobCategoryRepository;
import com.JobsNow.backend.request.CreateIndustryRequest;
import com.JobsNow.backend.request.UpdateIndustryRequest;
import com.JobsNow.backend.response.PagedResponse;
import com.JobsNow.backend.service.IndustryService;
import com.JobsNow.backend.util.PagingUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class IndustryServiceImpl implements IndustryService {
    private final IndustryRepository industryRepository;
    private final CompanyRepository companyRepository;
    private final JobCategoryRepository jobCategoryRepository;

    @Override
    public List<IndustryDTO> getAllIndustries() {
        return industryRepository.findAll().stream()
                .map(i -> new IndustryDTO(i.getIndustryId(), i.getName()))
                .collect(Collectors.toList());
    }

    @Override
    public PagedResponse<IndustryDTO> getIndustriesPage(int page, int limit) {
        int p = PagingUtil.safePage(page);
        int lim = PagingUtil.safeLimit(limit, 100);
        Page<Industry> pg = industryRepository.findAllByOrderByNameAsc(PageRequest.of(p - 1, lim));
        return PagedResponse.<IndustryDTO>builder()
                .items(pg.getContent().stream()
                        .map(i -> new IndustryDTO(i.getIndustryId(), i.getName()))
                        .toList())
                .totalCount(pg.getTotalElements())
                .page(p)
                .limit(lim)
                .hasNext(pg.hasNext())
                .build();
    }

    @Override
    public void addIndustry(CreateIndustryRequest request) {
        if (industryRepository.existsByName(request.getName())) {
            throw new BadRequestException("Industry already exists");
        }
        Industry industry = new Industry();
        industry.setName(request.getName());
        industryRepository.save(industry);
    }

    @Override
    public void updateIndustry(UpdateIndustryRequest request) {
        Industry industry = industryRepository.findById(request.getIndustryId())
                .orElseThrow(() -> new BadRequestException("Industry not found"));
        if (request.getName() != null && !request.getName().isBlank()) {
            if (industryRepository.existsByName(request.getName())
                    && !request.getName().equalsIgnoreCase(industry.getName())) {
                throw new BadRequestException("Industry name already exists");
            }
            industry.setName(request.getName());
        }
        industryRepository.save(industry);
    }

    @Override
    public void deleteIndustry(Integer industryId) {
        industryRepository.findById(industryId)
                .orElseThrow(() -> new BadRequestException("Industry not found"));
        if (jobCategoryRepository.findByIndustry_IndustryId(industryId).size() > 0) {
            throw new BadRequestException("Cannot delete industry: job categories are using it");
        }
        if (companyRepository.existsByIndustryId(industryId)) {
            throw new BadRequestException("Cannot delete industry: companies are using it");
        }
        industryRepository.deleteById(industryId);
    }
}
