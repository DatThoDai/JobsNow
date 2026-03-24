package com.JobsNow.backend.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SocialDTO {
    private Integer id;
    private String platform;
    private String url;
    private String logoUrl;
}
