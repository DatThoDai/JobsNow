package com.JobsNow.backend.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CompanyDTO {
    private Integer companyId;
    private String companyName;
    private String logoUrl;
    private String website;
    private String description;
}
