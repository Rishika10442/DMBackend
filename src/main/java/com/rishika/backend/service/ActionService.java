package com.rishika.backend.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rishika.backend.entity.StageX;
import com.rishika.backend.filter.JWTFilter;
import com.rishika.backend.repo.StageXRepo;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ActionService {
    private static final Logger logger = LoggerFactory.getLogger(JWTFilter.class);
    private final StageXRepo stageXRepository;
    private final ObjectMapper objectMapper;
    @Value("${folderPath}")
    private String datafolderPath;

    // You can call this method as part of your pipeline stage
    public void writeReportToFile(Map<String, Object> data, String fileName) {
        String baseDirectory =  "D:/my-app-reports"; // <-- change to your desired folder
        File outputDir = new File(baseDirectory);
        if (!outputDir.exists()) {
            boolean created = outputDir.mkdirs();
            if (!created) {
                throw new RuntimeException("Failed to create output directory: " + baseDirectory);
            }
        }

        File outputFile = new File(outputDir, fileName);
        if (!outputDir.canWrite()) {
            throw new RuntimeException("No write access to: " + baseDirectory);
        }

        try (FileWriter writer = new FileWriter(outputFile)) {
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(writer, data);
            System.out.println("Report written to: " + outputFile.getAbsolutePath());
        } catch (IOException e) {
            throw new RuntimeException("Failed to write report to file", e);
        }
    }

//    public ResponseEntity<Map<String, Object>> transform(Map<String, Object>request) {
//        Map<String, Object> response = new HashMap<>();
//        try {
//
//            Long stageXId = Long.parseLong(request.get("stageXId").toString());
//            logger.info("stage  x id is {}", stageXId);
//            String payload = request.get("actionPayload").toString();
//            ObjectMapper objectMapper = new ObjectMapper();
//
//            // Parse the JSON string into a Map<String, Object>
//            Map<String, Object> payloadMap = objectMapper.readValue(payload, new TypeReference<Map<String, Object>>(){});
//
//            StageX stageX = stageXRepository.findById(stageXId).orElse(null);
//            if (stageX == null) {
//                return ResponseEntity.status(HttpStatus.NOT_FOUND)
//                        .body(Map.of("error", "StageX not found for ID: " + stageXId));
//            }
//            StageX dataCollectionStage;
//            if ("dataCollection".equalsIgnoreCase(stageX.getStageName())) {
//                logger.info("Current stageX is data collection stage.");
//                dataCollectionStage = stageX;
//            }else {
//
//                // Step 3: Get pxId from StageX
//                Long pxId = stageX.getPipelineX().getPxId();
//
//                // Step 4: Find data collection stage for this pxId
//                dataCollectionStage = stageXRepository.findDataCollectionStageByPipelineXId(pxId);
//                if (dataCollectionStage == null) {
//                    logger.info("data collection stage not found for ID: " + pxId);
//                    return ResponseEntity.status(HttpStatus.NOT_FOUND)
//                            .body(Map.of("error", "No data collection stage found for pipelineXId: " + pxId));
//                }
//
//
//                logger.info("data collection fetched,{}", dataCollectionStage.getPayload());
//                String Datapayload = dataCollectionStage.getPayload();
//                Map<String, Object> DatapayloadMap = objectMapper.readValue(payload, new TypeReference<Map<String, Object>>() {});
//            }
//            // Step 1: Setup datasource
//            DriverManagerDataSource dataSource = new DriverManagerDataSource();
//            dataSource.setUrl(connectionString);
//            dataSource.setUsername(username);
//            dataSource.setPassword(password);
//            dataSource.setDriverClassName("com.mysql.cj.jdbc.Driver"); // Adjust for your DB
//
//            JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
//
//
//            // Step 2: Run the query — DDL or DML
//            jdbcTemplate.execute(query); // handles both create/drop/insert/update etc.
//
//            // Step 3: Build positive response — query ran without exception
//            response.put("status", "success");
//            response.put("message", "Query executed successfully.");
//            response.put("code", 200);
//
//        } catch (Exception e) {
//            // Step 4: Catch and return error
//            response.put("status", "error");
//            response.put("message", e.getMessage());
//            response.put("code", 500);
//        }
//
//        return response;
//    }


}
