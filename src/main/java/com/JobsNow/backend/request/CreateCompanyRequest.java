package com.JobsNow.backend.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateCompanyRequest {
    private String companyName;
    @JsonProperty("name")
    private String name;
    private String website;
    private String description;
    private String slogan;
    private String address;
    @JsonProperty("company_size")
    private String companySize;
    @JsonProperty("industry_id")
    private Object industryId; // String or Integer from JSON
}
