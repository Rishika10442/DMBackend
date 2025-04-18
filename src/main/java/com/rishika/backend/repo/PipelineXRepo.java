package com.rishika.backend.repo;

import com.rishika.backend.entity.PipelineX;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


public interface PipelineXRepo extends JpaRepository<PipelineX, Long> {
    // Optional: add custom methods here if needed
}
