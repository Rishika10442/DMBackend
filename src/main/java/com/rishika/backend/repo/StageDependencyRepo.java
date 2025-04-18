package com.rishika.backend.repo;

import com.rishika.backend.entity.Stage;
import com.rishika.backend.entity.StageDependency;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface StageDependencyRepo extends JpaRepository<StageDependency, Long> {
    // saveAll() is already available via JpaRepository
    List<StageDependency> findByDependsOnSid(Long dependsOnSid); // For reverse lookup
    List<StageDependency> findByStage(Stage stage);

    @Query("SELECT sd FROM StageDependency sd WHERE sd.stage.pipeline.pid = :pipelineId")
    List<StageDependency> findAllByPipelineId(@Param("pipelineId") Long pipelineId);
}
