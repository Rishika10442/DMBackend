package com.rishika.backend.controller;

import com.rishika.backend.entity.StageX;
import com.rishika.backend.repo.StageXRepo;
import com.rishika.backend.service.ActionService;
import com.rishika.backend.service.ExecutionService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/action")
@RequiredArgsConstructor
public class ActionController {
    private final StageXRepo stageXRepository;
    private final ActionService actionService;
    private static final Logger logger = LoggerFactory.getLogger(ExecutionService.class);
    // Simulate a stage action returning a positive outcome
    @PostMapping("/data")
    public ResponseEntity<Map<String, Object>> runStageAction(@RequestBody Map<String, Object> request) {
        // Extract stage info from the request
        logger.info("call in controller action 1");



        Long stageXId = Long.parseLong(request.get("stageXId").toString());
        logger.info("stage  x id is {}", stageXId);
        String payload = request.get("actionPayload").toString();

//        if (stageXId % 2 == 0) {
//            Map<String, Object> failureResponse = new HashMap<>();
//            failureResponse.put("outcome", "error");
//            failureResponse.put("log_info", "Simulated failure because stageXId is even: " + stageXId);
//
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(failureResponse);
//
//            }

        List<StageX> stageXs = stageXRepository.findAll();
        List<Long> last10Ids = stageXs.stream()
                .map(StageX::getSxId)
                .skip(Math.max(0, stageXs.size() - 10))
                .toList();

        logger.info("Last 10 StageX IDs: {}", last10Ids);


        // Step 2: Fetch StageX using sxId
        StageX stageX = stageXRepository.findById(stageXId).orElse(null);
        if (stageX == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "StageX not found for ID: " + stageXId));
        }

        // Step 3: Get pxId from StageX
        Long pxId = stageX.getPipelineX().getPxId();

        // Step 4: Find data collection stage for this pxId
        StageX dataCollectionStage = stageXRepository.findDataCollectionStageByPipelineXId(pxId);
        if (dataCollectionStage == null) {
            logger.info("data collection stage not found for ID: " + pxId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "No data collection stage found for pipelineXId: " + pxId));
        }

        logger.info("data collection fetched,{}", dataCollectionStage.getPayload());

        // Simulated logic (replace this with actual logic as needed)
        String outcome = "positive";
        String logInfo = "Stage " + stageXId + " executed successfully with payload: " + payload;

        // Construct response
        Map<String, Object> response = new HashMap<>();
        response.put("outcome", outcome);
        response.put("log_info", logInfo);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/event")
    public ResponseEntity<Map<String, Object>> runStageAction2(@RequestBody Map<String, Object> request) {
        // Extract stage info from the request
        Long stageXId = Long.parseLong(request.get("stageXId").toString());
        String payload = request.get("actionPayload").toString();

        // Simulated logic (replace this with actual logic as needed)
        String outcome = "positive";
        String logInfo = "Stage " + stageXId + " executed successfully with payload: " + payload;

        // Construct response
        Map<String, Object> response = new HashMap<>();
        response.put("outcome", outcome);
        response.put("log_info", logInfo);

        return ResponseEntity.ok(response);
    }

    // Simulate a stage action returning a negative outcome
    @PostMapping("/condition")
    public ResponseEntity<Map<String, Object>> runStageActionNegative(@RequestBody Map<String, Object> request) {
        Long stageXId = Long.parseLong(request.get("stageXId").toString());
        String payload = request.get("actionPayload").toString();

        String outcome = "negative";
        String logInfo = "Stage " + stageXId + " failed conditionally with payload: " + payload;

        Map<String, Object> response = new HashMap<>();
        response.put("outcome", outcome);
        response.put("log_info", logInfo);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/write-report")
    public ResponseEntity<String> writeReport(@RequestBody Map<String, Object> payload) {
        try {
            String fileName = "report_" + System.currentTimeMillis() + ".json";
            actionService.writeReportToFile(payload, fileName);
            return ResponseEntity.ok("Report written successfully: " + fileName);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Failed to write report: " + e.getMessage());
        }
    }
}
