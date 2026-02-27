package com.JobsNow.backend.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import lombok.Getter;
import lombok.Setter;

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
    private Integer industryId;

    @JsonSetter("industry_id")
    public void setIndustryIdFromJson(Object val) {
        if (val == null) {
            industryId = null;
        } else if (val instanceof Number) {
            industryId = ((Number) val).intValue();
        } else {
            try {
                industryId = Integer.parseInt(String.valueOf(val));
            } catch (NumberFormatException e) {
                industryId = null;
            }
        }
    }
}
