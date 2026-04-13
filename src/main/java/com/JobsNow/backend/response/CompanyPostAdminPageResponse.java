package com.JobsNow.backend.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompanyPostAdminPageResponse {
    private List<CompanyPostAdminItemResponse> items;
    private long totalCount;
    private int page;
    private int limit;
    private boolean hasNext;
}
