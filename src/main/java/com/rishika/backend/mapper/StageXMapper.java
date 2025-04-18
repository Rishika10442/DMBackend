package com.rishika.backend.mapper;
import com.rishika.backend.entity.*;
import com.rishika.backend.filter.JWTFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class StageXMapper {
    public StageX buildStageX(Stage stage, PipelineX pipelineX) {
        return StageX.builder()
                .stage(stage)
                .userStageId(stage.getUserStageId())
                .pipelineX(pipelineX)
                .user(stage.getUser())
                .pipeline(stage.getPipeline())
                .action(stage.getAction())
                .status("PENDING")
                .stageName(stage.getName())
                .payload(stage.getPayload())
                .build();
    }
}
