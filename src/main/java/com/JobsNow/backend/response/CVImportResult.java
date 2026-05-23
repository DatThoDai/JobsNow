package com.JobsNow.backend.response;

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
public class CVImportResult {
    public enum ParseStatus {
        SUCCESS,
        PARTIAL,
        FAILED,
        SKIPPED
    }

    private ParseStatus parseStatus;
    private ParsedCVData parsedCv;
    private String extractedTextJson;

    @Builder.Default
    private List<String> warnings = new ArrayList<>();
}
