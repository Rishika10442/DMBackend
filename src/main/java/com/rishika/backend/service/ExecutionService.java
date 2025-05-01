package com.rishika.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rishika.backend.controller.ActionController;
import com.rishika.backend.filter.JWTFilter;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import java.io.BufferedReader;
import java.io.InputStreamReader;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
@RequiredArgsConstructor
@Service
public class ExecutionService {
    private static final Logger logger = LoggerFactory.getLogger(ExecutionService.class);
    @Autowired
    private ActionController actionController;
    @Autowired
    private ActionService actionService;
    @Async("taskExecutor")
    public void triggerPipelineExecution(Long pxid, String jwtToken) {
        try {
            String url = "http://localhost:8000/api/execute/run/pipelineX";
            HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setRequestMethod("POST");
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(15000);
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Authorization", "Bearer " + jwtToken);

            // Create a JSON body with pxid
            String jsonBody = "{\"pxid\": " + pxid + "}";

            // Send the request
            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = jsonBody.getBytes("utf-8");
                os.write(input, 0, input.length);
            }

            int responseCode = conn.getResponseCode();

            if (responseCode >= 200 && responseCode < 300) {
                try (BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                    String inputLine;
                    StringBuilder response = new StringBuilder();
                    while ((inputLine = in.readLine()) != null) {
                        response.append(inputLine);
                    }
                    logger.info("PipelineX execution triggered. Response: {}", response.toString());
                }
            } else {
                try (BufferedReader err = new BufferedReader(new InputStreamReader(conn.getErrorStream()))) {
                    String inputLine;
                    StringBuilder errorResponse = new StringBuilder();
                    while ((inputLine = err.readLine()) != null) {
                        errorResponse.append(inputLine);
                    }
                    logger.error("Execution failed. Code: {}, Error: {}", responseCode, errorResponse.toString());
                }
            }

        } catch (Exception e) {
            logger.error("Failed to trigger pipeline execution", e);
        }
    }


//    @Async("taskExecutor")
//    public void executeStageAction(Long stageXId, String stageName, String actionPayload, String jwtToken) {
//        try {
//            String url = "http://localhost:8000/api/execute/run/action"; // Replace with actual action URL
//            HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
//            conn.setRequestMethod("POST");
//            conn.setConnectTimeout(5000);
//            conn.setReadTimeout(5000);
//            conn.setDoOutput(true);
//            conn.setRequestProperty("Content-Type", "application/json");
//            conn.setRequestProperty("Authorization", "Bearer " + jwtToken);
//
//            // Create a JSON body with the action data
//            String jsonBody = "{\"stageXId\": " + stageXId + ", \"actionPayload\": \"" + actionPayload + "\"}";
//
//            // Send the request
//            try (OutputStream os = conn.getOutputStream()) {
//                byte[] input = jsonBody.getBytes("utf-8");
//                os.write(input, 0, input.length);
//            }
//
//            int responseCode = conn.getResponseCode();
//            if (responseCode >= 200 && responseCode < 300) {
//                try (BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
//                    String inputLine;
//                    StringBuilder response = new StringBuilder();
//                    while ((inputLine = in.readLine()) != null) {
//                        response.append(inputLine);
//                    }
//                    logger.info("Stage action executed successfully. Response: {}", response.toString());
//                }
//            } else {
//                try (BufferedReader err = new BufferedReader(new InputStreamReader(conn.getErrorStream()))) {
//                    String inputLine;
//                    StringBuilder errorResponse = new StringBuilder();
//                    while ((inputLine = err.readLine()) != null) {
//                        errorResponse.append(inputLine);
//                    }
//                    logger.error("Execution failed. Code: {}, Error: {}", responseCode, errorResponse.toString());
//                }
//            }
//
//        } catch (Exception e) {
//            logger.error("Failed to execute stage action", e);
//        }
//    }


//    public Map<String, Object> executeStageActionWithResponse(Long stageXId, String stageName, String actionPayload,
//                                                              String jwtToken, String actionUrl, StringBuilder stageLog) {
//        Map<String, Object> result = new HashMap<>();
//        if (!actionUrl.startsWith("http")) {
//            actionUrl = "http://localhost:8000" + actionUrl;
//        }
//        try {
//            HttpURLConnection conn = (HttpURLConnection) new URL(actionUrl).openConnection();
//            conn.setRequestMethod("POST");
//            conn.setConnectTimeout(15000);
//            conn.setReadTimeout(15000);
//            conn.setDoOutput(true);
//            conn.setRequestProperty("Content-Type", "application/json");
//            conn.setRequestProperty("Authorization", "Bearer " + jwtToken);
//
//            String jsonBody = "{\"stageXId\": " + stageXId + ", \"actionPayload\": \"" + actionPayload + "\"}";
//
//            try (OutputStream os = conn.getOutputStream()) {
//                byte[] input = jsonBody.getBytes("utf-8");
//                os.write(input, 0, input.length);
//            }
//
//            int responseCode = conn.getResponseCode();
//            StringBuilder response = new StringBuilder();
//            BufferedReader reader = new BufferedReader(
//                    new InputStreamReader(responseCode >= 200 && responseCode < 300
//                            ? conn.getInputStream() : conn.getErrorStream()));
//
//            String line;
//            while ((line = reader.readLine()) != null) {
//                response.append(line);
//            }
//
//            String outcome = "positive"; // parse from response if real outcome is returned
//            try {
//                // Assuming the response is a JSON and contains an "outcome" field
//                ObjectMapper objectMapper = new ObjectMapper();
//                JsonNode responseJson = objectMapper.readTree(response.toString());
//
//                // Check if outcome exists in the response JSON
//                if (responseJson.has("outcome")) {
//                    outcome = responseJson.get("outcome").asText(); // Get the outcome value from the response
//                }
//            } catch (Exception e) {
//                logger.error("Failed to parse the response: {}", e.getMessage());
//                // Optionally, handle the error if outcome parsing fails
//            }
//
//            logger.info("Received outcome: {}", outcome);
//            logger.info("Received responseCode: {}", responseCode);
////            logger.info("Received log: {}", responseCode);
//            stageLog.append("Stage ").append(stageXId).append(" completed. Outcome: ").append(outcome)
//                    .append(". Log: ").append(response).append("\n");
//
//            result.put("outcome", outcome);
//            result.put("responseCode", responseCode);
//            result.put("fullResponse", response.toString());
//
//        } catch (Exception e) {
//            stageLog.append("Failed to execute stage ").append(stageXId).append(": ").append(e.getMessage()).append("\n");
//            String errorLog = "Failed to execute stage " + stageXId + ": " + e.getMessage() + "\n";
//            stageLog.append(errorLog);
//            logger.error(errorLog);
//
//            result.put("outcome", "error");
//            result.put("responseCode", 500);
//            result.put("fullResponse", errorLog);
//        }
//        return result;
//    }
//

    public Map<String, Object> executeStageActionWithResponse(Long stageXId, String actionPath, String payload, StringBuilder stageLog) {
        Map<String, Object> result = new HashMap<>();
        try {
            Map<String, Object> request = Map.of(
                    "stageXId", stageXId,
                    "actionPayload", payload
            );

            ResponseEntity<Map<String, Object>> response;
            switch (actionPath) {
                case "/api/action/data-collect":
                    response = actionService.handleDataCollection(request);
                    break;
                case "/api/action/extract":
                    response = actionService.extract(request);
                    break;
                case "/api/action/transform":
                    response = actionService.transform(request);
                    break;
                case "/api/action/load":
                    response = actionService.load(request);
                    break;
                case "/api/action/analysis":
                    response = actionService.analysis(request);
                    break;
                case "/api/action/report":
                    response = actionService.handleReportGeneration2(request);
                    break;
                case "/api/action/email":
                    response = actionService.sendEmails(request);
                    break;
                case "/api/action/loadjsonData":
                    response = actionService.loadJsonData(request);
                    break;
                case "/api/action/compareSales":
                    response = actionService.compareCategorySales(request);
                    break;
                case "/api/action/sqlQuery":
                    response = actionService.sqlQuery(request);
                    break;
                case "/api/action/export":
                    response = actionService.export(request);
                    break;
                default:
                    throw new IllegalArgumentException("Unknown action path: " + actionPath);
            }


            Map<String, Object> body = response.getBody();
            logger.info("body of response at execution {}",body.toString());
            String outcome = (String) body.getOrDefault("outcome", "unknown");
            stageLog.append("Stage ").append(body.getOrDefault("log_info", "")).append("\n");
//edit here stage ke sucess code ke hisaab se
            result.put("outcome", outcome);
            result.put("responseCode", response.getStatusCodeValue());
            result.put("fullResponse", body);

        } catch (Exception e) {
            logger.info("Failed to execute stage action", e);
            stageLog.append("Error in local stage call: ").append(e.getMessage()).append("\n");
            result.put("outcome", "error");
            result.put("responseCode", 500);
            result.put("fullResponse", e.getMessage());
        }

        return result;
    }
}




