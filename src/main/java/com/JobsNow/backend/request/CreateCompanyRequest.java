package com.JobsNow.backend.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
    @JsonProperty("industry_ids")
    private List<Integer> industryIds;

    @JsonProperty("name_user_contact")
    private String nameUserContact;

    @JsonProperty("tutorial_apply")
    private String tutorialApply;

    private List<SocialLinkItem> socials;

    @JsonProperty("thumbnail_image_urls")
    private List<String> thumbnailImageUrls;

    @JsonSetter("industry_ids")
    public void setIndustryIdsFromJson(List<?> raw) {
        if (raw == null || raw.isEmpty()) {
            industryIds = Collections.emptyList();
            return;
        }
        List<Integer> list = new ArrayList<>();
        for (Object o : raw) {
            if (o instanceof Number) {
                list.add(((Number) o).intValue());
            } else {
                try {
                    list.add(Integer.parseInt(String.valueOf(o)));
                } catch (NumberFormatException ignored) { }
            }
        }
        industryIds = list;
    }
}
