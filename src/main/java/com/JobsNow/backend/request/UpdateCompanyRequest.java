package com.JobsNow.backend.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateCompanyRequest {
    private String companyName;
    public String slogan;
    private String website;
    private String description;
}
