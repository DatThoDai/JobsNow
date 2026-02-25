package com.JobsNow.backend.repositories;

import com.JobsNow.backend.entity.Major;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MajorRepository extends JpaRepository<Major, Integer> {
    Optional<Major> findByName(String name);
    boolean existsByName(String name);
}