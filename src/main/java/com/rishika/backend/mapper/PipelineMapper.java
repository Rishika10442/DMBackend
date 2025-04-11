package com.rishika.backend.mapper;


import com.rishika.backend.dto.PipelineRequest;
import com.rishika.backend.entity.Pipeline;
import com.rishika.backend.entity.User;
import org.springframework.stereotype.Service;

@Service
public class PipelineMapper {

    public  Pipeline toPipelineEntity(PipelineRequest request, User user) {
        return Pipeline.builder()
                .user(user)
                .pName(request.pName())
                .dag(request.dag())
                .status(null)
                .firstStage(null)
                .build();
    }
}
