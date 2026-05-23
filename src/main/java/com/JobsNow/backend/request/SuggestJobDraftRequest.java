package com.JobsNow.backend.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SuggestJobDraftRequest {
    @NotBlank
    @Size(min = 3, max = 200)
    private String title;

    /** vi | en — defaults to vi when blank */
    private String locale;

    /** Category names from DB — AI must pick one exactly from this list when provided */
    private List<String> categoryNames;

    /** Skill names from DB — AI should prefer names from this list for suggestedSkills */
    private List<String> skillNames;

    /** Major names from DB — AI should prefer names from this list */
    private List<String> majorNames;
}
