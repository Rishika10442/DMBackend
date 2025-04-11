package com.rishika.backend.mapper;

import com.rishika.backend.dto.PipelineRequest;
import com.rishika.backend.entity.*;
import com.rishika.backend.filter.JWTFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class StageMapper {
    private static final Logger logger = LoggerFactory.getLogger(JWTFilter.class);
    public Stage toStageEntity(PipelineRequest.StageData stageData, Pipeline pipeline, User user, Action action) {
        logger.info("Mapping stage: name={}, userStageId={}", stageData.stageName(), stageData.userStageID());
        return Stage.builder()
                .pipeline(pipeline)
                .user(user)
                .action(action)
                .userStageId(stageData.userStageID())
                .name(stageData.stageName())
                .payload(stageData.payload())
                .build();
    }
}
