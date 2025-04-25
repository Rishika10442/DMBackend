package com.rishika.backend.repo;

import com.rishika.backend.entity.PipelineX;
import com.rishika.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;


public interface PipelineXRepo extends JpaRepository<PipelineX, Long> {
    // Optional: add custom methods here if needed
    List<PipelineX> findByUser(User user);
    PipelineX findByPxId(Long pxId);
}
