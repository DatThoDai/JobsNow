package com.JobsNow.backend.controllers;

import com.JobsNow.backend.request.CreateSkillRequest;
import com.JobsNow.backend.response.BaseResponse;
import com.JobsNow.backend.response.ResponseFactory;
import com.JobsNow.backend.service.SkillService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/skill")
@RequiredArgsConstructor
public class SkillController {
    private final SkillService skillService;
    @GetMapping("/all")
    public ResponseEntity<?> getAllSkills() {
        return ResponseFactory.success(skillService.getAllSkills());
    }

    @PostMapping("/add")
    public ResponseEntity<?> addSkill(@RequestBody CreateSkillRequest request){
        skillService.addSkill(request);
        return ResponseFactory.successMessage("Skill added successfully");
    }

    @DeleteMapping("/delete/{skillId}")
    public ResponseEntity<?> deleteSkill(@PathVariable Integer skillId) {
        skillService.deleteSkill(skillId);
        return ResponseFactory.successMessage("Skill deleted successfully");
    }

    @PutMapping("/update")
    public ResponseEntity<?> updateSkill(@RequestParam Integer skillId, @RequestParam String skillName) {
        skillService.updateSkill(skillId, skillName);
        return ResponseFactory.successMessage("Skill updated successfully");
    }
}
