package com.rishika.backend.mapper;
import com.rishika.backend.entity.*;
import com.rishika.backend.enums.DependencyType;
import com.rishika.backend.filter.JWTFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
@Service
public class StageXDependencyMapper {
    public StageXDependency buildStageXDependency(StageX stageX, StageX dependsOn, StageDependency dep) {
        return StageXDependency.builder()
                .stageX(stageX)
                .dependsOn(dependsOn)
                .stageOutcome(dep.getStageOutcome())
                .dependencyType(dep.getDependencyType() != null ? dep.getDependencyType() : DependencyType.ALWAYS)
                .build();
    }

}
