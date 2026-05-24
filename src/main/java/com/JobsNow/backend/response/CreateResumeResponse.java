package com.JobsNow.backend.response;

import com.JobsNow.backend.dto.ResumeDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateResumeResponse {
    private ResumeDTO resume;
    private CVImportResult.ParseStatus parseStatus;
    private ParsedCVData parsedCv;

    @Builder.Default
    private List<String> parseWarnings = new ArrayList<>();

    private Integer sectionsSynced;
}
