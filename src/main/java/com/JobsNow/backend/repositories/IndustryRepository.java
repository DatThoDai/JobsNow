package com.JobsNow.backend.repositories;

import com.JobsNow.backend.entity.Industry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IndustryRepository extends JpaRepository<Industry, Integer> {
    boolean existsByName(String name);
}