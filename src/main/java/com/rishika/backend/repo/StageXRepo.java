package com.rishika.backend.repo;

import com.rishika.backend.entity.PipelineX;
import com.rishika.backend.entity.Stage;
import com.rishika.backend.entity.StageX;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StageXRepo extends JpaRepository<StageX, Long> {
    Optional<StageX> findByStage(Stage stage);
    // Fetch all stages for a pipeline
//    List<StageX> findAllByPxid(Long pxid);
//
//    // Optional: Find by sxId if needed individually
//    StageX findBySxid(Long sxid);
//
//    // Optional: Find by userStageId
//    StageX findByUserStageId(String userStageId);// To link dependencies later
    List<StageX> findAllByPipelineX_PxId(Long pxId);

    Optional<StageX> findByPipelineXAndStage(PipelineX pipelineX, Stage stage);
}
