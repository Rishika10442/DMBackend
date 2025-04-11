package com.rishika.backend.service;

import com.rishika.backend.dto.PipelineRequest;
import com.rishika.backend.entity.*;
import com.rishika.backend.filter.JWTFilter;
import com.rishika.backend.mapper.PipelineMapper;
import com.rishika.backend.mapper.StageDependencyMapper;
import com.rishika.backend.mapper.StageMapper;
import com.rishika.backend.repo.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DesignerService {

    private final PipelineRepo pipelineRepository;

    private final PipelineMapper pipelineMapper;
    private final StageMapper stageMapper;
    private final StageDependencyMapper stageDependencyMapper;
    private final UserRepo userRepository;
    private final StageRepo stageRepository;
    private final ActionRepo actionRepository;
    private final StageDependencyRepo stageDependencyRepository;
    private static final Logger logger = LoggerFactory.getLogger(JWTFilter.class);


    @Transactional
    public Map<String, Object> createPipeline(PipelineRequest request) {
        logger.info("Received PipelineRequest: {}", request);
        Map<String, Object> response = new HashMap<>();



        try {
            User user = userRepository.findById(request.userId())
                    .orElseThrow(() -> new RuntimeException("User not found in create pipeline designerService"));

            Pipeline pipeline = pipelineMapper.toPipelineEntity(request, user);

            pipeline = pipelineRepository.save(pipeline);
            logger.debug("Created pipeline with PID: {}", pipeline.getPid());

            response.put("message", "Pipeline created successfully");



            // Step 1: Save all stages
            Map<Integer, Stage> stageMap = new HashMap<>();
            for (PipelineRequest.StageData stageData : request.stages()) {
                Action action = actionRepository.findById(stageData.actionId())
                        .orElseThrow(() -> new RuntimeException("Action not found in create pipeline designerService"));
                Stage stage = stageMapper.toStageEntity(stageData, pipeline, user,action);
                Stage savedStage = stageRepository.save(stage);
                stageMap.put(stage.getUserStageId(), savedStage);
                logger.info("Created stage '{}' with SID: {}", savedStage.getName(), savedStage.getSid());

            }

            // Step 2: Create stage dependencies
            List<StageDependency> dependencies = new ArrayList<>();
            Map<Integer, Boolean> stageIdToCFlag = request.stages().stream()
                    .collect(Collectors.toMap(PipelineRequest.StageData::userStageID, PipelineRequest.StageData::CFlag));

            for (PipelineRequest.StageData stageData : request.stages()) {
                Stage sourceStage = stageMap.get(stageData.userStageID());
                List<StageDependency> depList = stageDependencyMapper.fromSuccessAndFailureMappings(stageMap, stageData,stageIdToCFlag, sourceStage);
                for (StageDependency dep : depList) {
                    logger.info("Created dependency: {} (outcome: {}) -> {}",
                            sourceStage.getSid(), dep.getStageOutcome(), dep.getDependsOn().getSid());
                }
                dependencies.addAll(depList);
            }

            stageDependencyRepository.saveAll(dependencies);

            // Step 3: Update pipeline with status and first stage (resolved by sid)
            Stage entryStage = stageMap.get(1); // userStageId == 1
            if (entryStage == null) {
                throw new RuntimeException("Entry stage with userStageId = 1 not found in designerService");
            }
            pipeline.setStatus("created");
            pipeline.setFirstStage(entryStage);
            pipelineRepository.save(pipeline);

            logger.info("Pipeline status set to 'created' and first stage set to SID: {}", entryStage.getSid());


            //stagedependecy table, create stages, create dependecies, fill pipeline fields

            response.put("status", "success");
            response.put("pid", pipeline.getPid());
        } catch (Exception e) {
            response.put("message", "Error in creating pipeline: in create pipeline designerService " + e.getMessage());
            response.put("status", "error");
            throw e;
        }

        return response;
    }

}
