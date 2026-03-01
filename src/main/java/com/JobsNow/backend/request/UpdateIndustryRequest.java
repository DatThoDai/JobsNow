package com.JobsNow.backend.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateIndustryRequest {
    private Integer industryId;
    private String name;
}
