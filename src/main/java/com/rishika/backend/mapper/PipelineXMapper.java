package com.rishika.backend.mapper;

import com.rishika.backend.entity.Pipeline;
import com.rishika.backend.entity.PipelineX;
import org.springframework.stereotype.Component;

@Component
public class PipelineXMapper {

    public PipelineX toPipelineX(Pipeline pipeline) {
        return PipelineX.builder()
                .pipeline(pipeline)                             // set Pipeline entity reference
                .user(pipeline.getUser())                       // set User entity reference
                .firstStage(pipeline.getFirstStage())           // check done in service before calling
                .name(pipeline.getPName())
                .dag(pipeline.getDag())
                .status("created")
                .logInfo("")                                    // will fill later
                .pipelineProgress("")                           // will fill later
                .build();
    }
}
