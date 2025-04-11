package com.rishika.backend.repo;

import com.rishika.backend.entity.StageDependency;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StageDependencyRepo extends JpaRepository<StageDependency, Long> {
    // saveAll() is already available via JpaRepository
}
