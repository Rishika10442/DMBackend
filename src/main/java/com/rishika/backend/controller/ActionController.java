package com.rishika.backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/action")
public class ActionController {

    // Simulate a stage action returning a positive outcome
    @PostMapping("/data")
    public ResponseEntity<Map<String, Object>> runStageAction(@RequestBody Map<String, Object> request) {
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
}
