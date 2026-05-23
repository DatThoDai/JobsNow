package com.JobsNow.backend.repositories;

import com.JobsNow.backend.entity.Skill;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SkillRepository extends JpaRepository<Skill, Integer> {
    boolean existsBySkillName(String skillName);

    Optional<Skill> findFirstBySkillNameIgnoreCase(String skillName);

    Page<Skill> findAllByOrderBySkillNameAsc(Pageable pageable);
}
