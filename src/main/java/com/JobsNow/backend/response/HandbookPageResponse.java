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
public class HandbookPageResponse {
    private List<HandbookPostResponse> items;
    private long totalCount;
    private int page;
    private int size;
    private boolean hasNext;
}
