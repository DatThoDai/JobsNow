package com.JobsNow.backend.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class UpdateCompanyRequest {
    @JsonProperty("name")
    private String companyName;
    private String slogan;
    private String website;
    private String description;
    private String address;
    @JsonProperty("company_size")
    private String companySize;
    @JsonProperty("industry_ids")
    private List<Integer> industryIds;
}
