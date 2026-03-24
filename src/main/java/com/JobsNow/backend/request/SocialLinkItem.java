package com.JobsNow.backend.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SocialLinkItem {
    private String platform;
    private String url;
    @JsonProperty("logo_url")
    private String logoUrl;
}
