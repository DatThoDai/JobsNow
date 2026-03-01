package com.JobsNow.backend.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateIndustryRequest {
    @NotBlank(message = "Industry name is required")
    private String name;
}
