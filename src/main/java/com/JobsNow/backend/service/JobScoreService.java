package com.JobsNow.backend.service;

public interface JobScoreService {
    void recalculateAllScores();
    void expireBoosts();
}
