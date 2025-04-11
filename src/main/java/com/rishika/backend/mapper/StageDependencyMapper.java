package com.rishika.backend.mapper;

import com.rishika.backend.dto.PipelineRequest;
import com.rishika.backend.entity.Stage;
import com.rishika.backend.entity.StageDependency;
import com.rishika.backend.enums.StageOutcome;
import com.rishika.backend.enums.DependencyType;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class StageDependencyMapper {

    public List<StageDependency> fromSuccessAndFailureMappings(
            Map<Integer, Stage> stageMap,
            PipelineRequest.StageData stageData,
            Map<Integer, Boolean> stageIdToCFlag,
            Stage sourceStage
    ) {
        List<StageDependency> dependencies = new ArrayList<>();

        List<Long> successIds = Optional.ofNullable(stageData.nextSidSuccess()).orElse(List.of());
        for (Long targetId : successIds) {
            Stage targetStage = stageMap.get(targetId.intValue());
            if (targetStage != null) {
                boolean isConditional = stageIdToCFlag.getOrDefault(targetId.intValue(), false);
                dependencies.add(StageDependency.builder()
                        .stage(targetStage)
                        .dependsOn(sourceStage)
                        .stageOutcome(StageOutcome.POSITIVE)
                        .dependencyType(isConditional ? DependencyType.CONDITIONAL : DependencyType.ALWAYS)
                        .build());
            }
        }

        List<Long> failureIds = Optional.ofNullable(stageData.nextSidFaliure()).orElse(List.of());
        for (Long targetId : failureIds) {
            Stage targetStage = stageMap.get(targetId.intValue());
            if (targetStage != null) {
                boolean isConditional = stageIdToCFlag.getOrDefault(targetId.intValue(), false);
                dependencies.add(StageDependency.builder()
                        .stage(targetStage)
                        .dependsOn(sourceStage)
                        .stageOutcome(StageOutcome.NEGATIVE)
                        .dependencyType(isConditional ? DependencyType.CONDITIONAL : DependencyType.ALWAYS)
                        .build());
            }
        }

        return dependencies;
    }

}
