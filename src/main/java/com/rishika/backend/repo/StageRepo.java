package com.rishika.backend.repo;

import com.rishika.backend.entity.Pipeline;
import com.rishika.backend.entity.Stage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StageRepo  extends JpaRepository<Stage, Long> {
    List<Stage> findByPipeline(Pipeline pipeline);
    Stage findByPipelinePidAndUserStageId(Long pid, int userStageId);


}
