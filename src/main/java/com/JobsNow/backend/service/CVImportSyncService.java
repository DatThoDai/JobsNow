package com.JobsNow.backend.service;

import com.JobsNow.backend.response.ParsedCVData;

public interface CVImportSyncService {
    /**
     * Maps parsed CV JSON into resume-scoped section tables for CVContentForm editing.
     * @return number of section rows created
     */
    int syncParsedCvToResumeSections(Integer resumeId, ParsedCVData parsed, boolean replaceExisting);
}
