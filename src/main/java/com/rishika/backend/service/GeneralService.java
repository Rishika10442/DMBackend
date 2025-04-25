package com.rishika.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rishika.backend.dto.PipelineSummaryDTO;
import com.rishika.backend.dto.PipelineXSummaryDTO;
import com.rishika.backend.entity.*;
import com.rishika.backend.filter.JWTFilter;
import com.rishika.backend.mapper.UserMapper;
import com.rishika.backend.repo.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class GeneralService {
    private final PipelineXRepo pipelineXRepository;
    private final PipelineRepo pipelineRepository;
    private final StageRepo stageRepository;
    private final ActionRepo actionRepository;
    private final UserRepo userRepository;
    private final UserMapper mapper;

    private static final Logger logger = LoggerFactory.getLogger(JWTFilter.class);

    public Map<String, Object> getPipelineX(Long pxId) {
        try {
         //   logger.info("AT getPipelineX ,{}", pxId);
            // Fetch the PipelineX entity by pxId
            PipelineX pipelineX = pipelineXRepository.findByPxId(pxId);

            // If not found, return an error message
            if (pipelineX == null) {
                logger.info(" PipelineX with pxId {} not found.", pxId);
                return Map.of(
                        "success", false,
                        "message", "PipelineX with pxId " + pxId + " not found"
                );
            }

            // Prepare the response map with required fields
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("name", pipelineX.getName());
            response.put("dag", pipelineX.getDag());
            response.put("logInfo", pipelineX.getLogInfo());
            response.put("pipelineProgress", pipelineX.getPipelineProgress());
            response.put("status", pipelineX.getStatus());
            response.put("createdAt", pipelineX.getCreatedAt());
            response.put("finishedAt", pipelineX.getFinishedAt());
            return response;
        } catch (Exception e) {
            // Log the exception (use appropriate logging framework in production)
           e.printStackTrace();

            // Return a response indicating failure
            return Map.of(
                    "success", false,
                    "message", "An error occurred while retrieving the PipelineX data.",
                    "error", e.getMessage()
            );
        }
    }
    public Map<String, Object> getPipeline(Long pId) {
        try {
            //   logger.info("AT getPipelineX ,{}", pxId);
            // Fetch the PipelineX entity by pxId
            Pipeline pipeline = pipelineRepository.findByPid(pId);

            // If not found, return an error message
            if (pipeline == null) {
                logger.info(" PipelineX with pxId {} not found.", pId);
                return Map.of(
                        "success", false,
                        "message", "Pipeline with pId " + pId + " not found"
                );
            }

            // Prepare the response map with required fields
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("name", pipeline.getPName());
            response.put("dag", pipeline.getDag());
            response.put("status", pipeline.getStatus());
            response.put("createdAt", pipeline.getCreatedAt());

            return response;
        } catch (Exception e) {
            // Log the exception (use appropriate logging framework in production)
            e.printStackTrace();

            // Return a response indicating failure
            return Map.of(
                    "success", false,
                    "message", "An error occurred while retrieving the Pipeline data.",
                    "error", e.getMessage()
            );
        }
    }

    public Map<String, Object> deletePipeline(Long pid) {
        try {


            Pipeline pipeline = pipelineRepository.findById(pid).orElse(null);

            if (pipeline == null) {

                return Map.of(
                        "success", false,
                        "message", "Pipeline with pid " + pid + " not found."
                );
            }

            pipeline.setStatus("DISABLED");
            pipelineRepository.save(pipeline);



            return Map.of(
                    "success", true,
                    "message", "Pipeline disabled successfully."
            );

        } catch (Exception e) {

            return Map.of(
                    "success", false,
                    "message", "An error occurred while disabling the pipeline.",
                    "error", e.getMessage()
            );
        }
    }

    @Transactional
    public Map<String, Object> updatePipelineStages(Map<String, Object> requestData) {
        try {
            Long pid = Long.valueOf(requestData.get("pid").toString());
            Pipeline pipeline = pipelineRepository.findById(pid)
                    .orElseThrow(() -> new RuntimeException("Pipeline with pid " + pid + " not found."));
            // Extract "stages" map
            logger.info(" PipelineX with pId {} found.", pid);
            Map<String, Map<String, Object>> stages = (Map<String, Map<String, Object>>) requestData.get("stages");

            if (stages == null || stages.isEmpty()) {
                return Map.of(
                        "success", false,
                        "message", "No stages provided for update."
                );
            }

            for (Map.Entry<String, Map<String, Object>> entry : stages.entrySet()) {
                String userStageIdStr = entry.getKey();
                int userStageId = Integer.parseInt(userStageIdStr);
                Map<String, Object> updatedPayload = entry.getValue();

                Stage stage = stageRepository.findByPipelinePidAndUserStageId(pid, userStageId);
                if (stage != null) {
                    // Update payload
                    ObjectMapper mapper = new ObjectMapper();

                    // Convert the updatedPayload Map to a JSON String
                    String updatedPayloadJson = mapper.writeValueAsString(updatedPayload);

                    stage.setPayload(updatedPayloadJson);
                    stageRepository.save(stage);
                } else {
                    logger.warn("Stage not found for pid={} and userStageId={}", pid, userStageId);
                }
            }

            return Map.of(
                    "success", true,
                    "message", "Stages updated successfully."
            );

        } catch (Exception e) {
            logger.error("Exception while updating pipeline stages: ", e);
            return Map.of(
                    "success", false,
                    "message", "Error occurred while updating pipeline stages.",
                    "error", e.getMessage()
            );
        }
    }

    public ResponseEntity<Map<String, Object>> getPollingInfo(Long pxId) {
        Map<String, Object> response = new HashMap<>();

        try {
            // Attempt to find the PipelineX entity by pxId
            PipelineX pipelineX = pipelineXRepository.findByPxId(pxId);

            if (pipelineX == null) {
                response.put("success", false);
                response.put("message", "PipelineX with pxId " + pxId + " not found.");
                return ResponseEntity.badRequest().body(response);
            }

            // Fill the response if PipelineX is found
            response.put("success", true);
            response.put("status", pipelineX.getStatus());
            response.put("finishedAt", pipelineX.getFinishedAt());
            response.put("pipelineProgress", pipelineX.getPipelineProgress());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            // Handle any exception that occurs and log it
            response.put("success", false);
            response.put("message", "An error occurred while fetching the PipelineX data.");
            response.put("error", e.getMessage());

            return ResponseEntity.status(500).body(response);
        }
    }

    public ResponseEntity<Map<String, Object>> getAllActions() {
        Map<String, Object> response = new HashMap<>();

        try {
            List<Action> actions = actionRepository.findAll();

            response.put("success", true);
            response.put("actions", actions);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Failed to fetch actions.");
            response.put("error", e.getMessage());

            return ResponseEntity.internalServerError().body(response);
        }
    }
    public Map<String, Object> getDashboard(Long userId) {
        Map<String, Object> response = new HashMap<>();

        try {
            // Step 1: Fetch the user
            Optional<User> optionalUser = userRepository.findById(userId);
            if (optionalUser.isEmpty()) {
                response.put("success", false);
                response.put("message", "User not found for ID: " + userId);
                return response;
            }

            User user = optionalUser.get();

            // Step 2: Fetch pipelines and pipelineX
            List<Pipeline> pipelines = pipelineRepository.findByUser(user);
            List<PipelineX> pipelineXList = pipelineXRepository.findByUser(user);

            // Step 3: Convert to DTOs with safe null handling and sorting
            List<PipelineSummaryDTO> pipelineSummaries = (pipelines != null) ? pipelines.stream()
                    .sorted(Comparator.comparing(
                            Pipeline::getCreatedAt,
                            Comparator.nullsLast(Comparator.naturalOrder())
                    ).reversed())
                    .map(p -> new PipelineSummaryDTO(p.getPid(), p.getPName()))
                    .toList() : new ArrayList<>();

            List<PipelineXSummaryDTO> pipelineXSummaries = (pipelineXList != null) ? pipelineXList.stream()
                    .sorted(Comparator.comparing(
                            PipelineX::getCreatedAt,
                            Comparator.nullsLast(Comparator.naturalOrder())
                    ).reversed())
                    .map(px -> new PipelineXSummaryDTO(px.getPxId(), px.getName(), px.getStatus()))
                    .toList() : new ArrayList<>();

            // Step 4: Build the response
            response.put("success", true);
            response.put("pipelines", pipelineSummaries);
            response.put("pipelinesX", pipelineXSummaries);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "An error occurred while fetching dashboard: " + e.getMessage());
        }

        return response;
    }

}
