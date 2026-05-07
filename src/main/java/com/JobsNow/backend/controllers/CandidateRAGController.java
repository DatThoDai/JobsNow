package com.JobsNow.backend.controllers;

import com.JobsNow.backend.dto.AiSearchRequest;
import com.JobsNow.backend.dto.AiSearchResponse;
import com.JobsNow.backend.service.CandidateRAGService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/ai/rag")
@RequiredArgsConstructor
public class CandidateRAGController {

    private final CandidateRAGService candidateRAGService;

    @PostMapping("/search")
    public ResponseEntity<AiSearchResponse> searchCandidates(@RequestBody AiSearchRequest request) {
        AiSearchResponse response = candidateRAGService.searchCandidates(request, null);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/index/all")
    public ResponseEntity<Map<String, String>> indexAllProfiles() {
        candidateRAGService.indexAllProfiles();
        return ResponseEntity.ok(Map.of("message", "Đánh chỉ mục toàn bộ hồ sơ thành công"));
    }

    @PostMapping("/index/{profileId}")
    public ResponseEntity<Map<String, String>> indexProfile(@PathVariable Integer profileId) {
        candidateRAGService.indexProfile(profileId);
        return ResponseEntity.ok(Map.of("message", "Đánh chỉ mục hồ sơ #" + profileId + " thành công"));
    }
}
