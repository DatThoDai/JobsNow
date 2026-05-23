package com.JobsNow.backend.service;

import com.JobsNow.backend.request.SuggestJobDraftRequest;
import com.JobsNow.backend.response.JobDraftSuggestionResponse;

public interface AIJobDraftService {
    JobDraftSuggestionResponse suggest(SuggestJobDraftRequest request);
}
