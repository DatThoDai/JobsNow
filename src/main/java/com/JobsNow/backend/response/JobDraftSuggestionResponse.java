package com.JobsNow.backend.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobDraftSuggestionResponse {
    private String description;
    private String requirements;
    private String benefits;
    private String location;
    private String jobType;
    private String yearsOfExperience;
    private String educationLevel;
    private String salaryType;
    private String salaryCurrency;
    private Double salaryMin;
    private Double salaryMax;
    private String applicationLanguage;
    private String genderRequirement;
    private String suggestedCategoryName;
    private List<SuggestedSkillItem> suggestedSkills;
    private List<String> suggestedMajorNames;
}
