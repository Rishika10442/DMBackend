package com.rishika.backend.repo;

import com.rishika.backend.entity.StageXDependency;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StageXDependencyRepo extends JpaRepository<StageXDependency, Long> {
    // Fetch all dependencies for a pipeline
//    List<StageXDependency> findAllByPxid(Long pxid);
//
//    // To find all stages that are dependent on a completed stage
//    List<StageXDependency> findAllByDependsOnSxid(Long dependsOnSxid);
//
//    // Optional: Find dependencies of a specific stage
//    List<StageXDependency> findAllBySxid(Long sxid);
    List<StageXDependency> findAllByStageX_PipelineX_PxId(Long pxId);

}
