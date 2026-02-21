package com.JobsNow.backend.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CompanyImageDTO {
    private Integer imageId;
    private String imageUrl;
    private String type;
}
