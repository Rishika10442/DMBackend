package com.rishika.backend.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rishika.backend.entity.StageX;
import com.rishika.backend.filter.JWTFilter;
import com.rishika.backend.repo.StageXRepo;
import jakarta.mail.*;

import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.sql.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ActionService {
    private static final Logger logger = LoggerFactory.getLogger(JWTFilter.class);
    private final StageXRepo stageXRepository;
    private final ObjectMapper objectMapper;
    @Value("${folderPath}")
    private String datafolderPath;
    @Value("${spring.mail.username}")
    private String senderEmail;

    @Value("${spring.mail.password}")
    private String senderPassword;

    // You can call this method as part of your pipeline stage
    public void writeReportToFile(Map<String, Object> data, String fileName) {
        String baseDirectory = "D:/my-app-reports"; // <-- change to your desired folder
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

    public ResponseEntity<Map<String, Object>> handleDataCollection(Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();

        try {
            // Step 1: Extract the stageXId and actionPayload
            Long stageXId = Long.parseLong(request.get("stageXId").toString());
            logger.info("stageXId received: {}", stageXId);

            String payload = request.get("actionPayload").toString();
            ObjectMapper objectMapper = new ObjectMapper();
            Map<String, Object> payloadMap;

            try {
                payloadMap = objectMapper.readValue(payload, new TypeReference<>() {
                });
            } catch (Exception e) {
                response.put("outcome", "error");
                response.put("log_info", "Invalid actionPayload JSON: " + e.getMessage());
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }

            // Step 2: Extract connectionString and fileCount from the payload
            String connectionString = (String) payloadMap.get("connectionString");
            int fileCount = (int) payloadMap.get("fileCount");

            boolean hasFiles = fileCount > 0;
            boolean hasConnection = connectionString != null && !connectionString.isEmpty();

//            if (hasFiles && hasConnection) {
//                response.put("outcome", "error");
//                response.put("log_info", "Provide either file input or connection, not both.");
//                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
//            }

            if (!hasFiles && !hasConnection) {
                response.put("outcome", "error");
                response.put("log_info", "No input provided.");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }

            // Step 3: Handle file input case
            if (hasFiles) {
                if (!payloadMap.containsKey("files") || !(payloadMap.get("files") instanceof List)) {
                    response.put("outcome", "error");
                    response.put("log_info", "File count > 0 but files are missing or not in array format.");
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
                }

                List<String> fileNames = (List<String>) payloadMap.get("files");
                if (fileNames.size() < fileCount) {
                    response.put("outcome", "error");
                    response.put("log_info", "File count mismatch: file count is less than the number of files.");
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
                }

                String basePath = "D:/DMBackend/";
                for (String fileName : fileNames) {
                    File file = new File(basePath + fileName);
                    if (!file.exists()) {
                        response.put("outcome", "error");
                        response.put("log_info", "File not found: " + fileName);
                        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
                    }
                }

                response.put("outcome", "positive");
                response.put("log_info", "All files found.");
                return ResponseEntity.ok(response);
            }

            // Step 4: Handle connection string case
            if (hasConnection) {
                try (Connection conn = DriverManager.getConnection(connectionString)) {
                    response.put("outcome", "positive");
                    response.put("log_info", "Successfully connected to database: " + connectionString);
                    return ResponseEntity.ok(response);
                } catch (SQLException e) {
                    response.put("outcome", "error");
                    response.put("log_info", "Database connection failed: " + e.getMessage());
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
                }
            }

        } catch (Exception e) {
            response.put("outcome", "error");
            response.put("log_info", "Data collection failed: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
        response.put("outcome", "error");
        response.put("log_info", "Unhandled case in data collection.");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }


    public ResponseEntity<Map<String, Object>> extract(Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();
        ObjectMapper objectMapper = new ObjectMapper();

        try {
            final String SQL_BASE_DIR = "D:/DMBackend/";
            // 1. Get stageXId
            Long stageXId = Long.parseLong(request.get("stageXId").toString());

            // 2. Parse actionPayload JSON
            String actionPayloadStr = request.get("actionPayload").toString();
            Map<String, Object> payloadMap;
            try {
                payloadMap = objectMapper.readValue(actionPayloadStr, new TypeReference<>() {
                });
            } catch (Exception e) {
                response.put("outcome", "error");
                response.put("log_info", "Invalid actionPayload JSON: " + e.getMessage());
                response.put("code", 400);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }
            // Map<String, Object> payloadMap = objectMapper.readValue(actionPayloadStr, new TypeReference<>() {});

            // 3. Load StageX and related data collection stage
            StageX stageX = stageXRepository.findById(stageXId).orElse(null);
            if (stageX == null) {
                response.put("outcome", "error");
                response.put("log_info", "StageX not found for ID: " + stageXId);
                response.put("code", 404);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }

            Long pipelineXId = stageX.getPipelineX().getPxId();
            StageX dataCollectionStage = stageXRepository.findDataCollectionStageByPipelineXId(pipelineXId);
            if (dataCollectionStage == null) {
                response.put("outcome", "error");
                response.put("log_info", "No data collection stage found for pipelineXId: " + pipelineXId);
                response.put("code", 404);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }

            // 4. Parse payload from data collection stage
            String datapayloadJson = dataCollectionStage.getPayload();
            Map<String, Object> datapayloadMap;
            try {
                datapayloadMap = objectMapper.readValue(datapayloadJson, new TypeReference<>() {
                });
            } catch (Exception e) {
                response.put("outcome", "error");
                response.put("log_info", "Invalid datapayloadMap JSON: " + e.getMessage());
                response.put("code", 400);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }

            //Map<String, Object> datapayloadMap = objectMapper.readValue(datapayloadJson, new TypeReference<>() {});

            // 5. Extract file path and connection string
            String sqlFileName = payloadMap.getOrDefault("sqlFile", "").toString();
            String sqlFilePath = SQL_BASE_DIR + sqlFileName;
            String connectionString = datapayloadMap.getOrDefault("connectionString", "").toString();

            if (sqlFilePath.isEmpty() || connectionString.isEmpty()) {
                response.put("outcome", "error");
                response.put("log_info", "Missing 'file' in actionPayload or 'connectionString' in data collection stage.");
                response.put("code", 400);
                return ResponseEntity.badRequest().body(response);
            }

            // 6. Read SQL file
            StringBuilder sqlBuilder = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new FileReader(sqlFilePath))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    sqlBuilder.append(line).append("\n");
                }
            } catch (IOException e) {
                response.put("outcome", "error");
                response.put("log_info", "Failed to read SQL file: " + e.getMessage());
                response.put("code", 500);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
            }

            // 7. Execute SQL
            DriverManagerDataSource dataSource = new DriverManagerDataSource();
            dataSource.setUrl(connectionString);
            dataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");

            JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
            jdbcTemplate.execute(sqlBuilder.toString());

            response.put("outcome", "positive");
            response.put("log_info", "SQL executed successfully. executedFile {}" + sqlFilePath);
            response.put("executedFile", sqlFilePath);
            response.put("code", 200);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("outcome", "error");
            response.put("log_info", "Execution failed: " + e.getMessage());
            response.put("code", 500);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    public ResponseEntity<Map<String, Object>> transform(Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();
        ObjectMapper objectMapper = new ObjectMapper();

        try {
            final String SQL_BASE_DIR = "d:/DMBackend/";
            Object stageXIdObj = request.get("stageXId");
            Object actionPayloadObj = request.get("actionPayload");

            if (stageXIdObj == null || actionPayloadObj == null) {
                response.put("outcome", "error");
                response.put("log_info", "Missing required parameters: stageXId or actionPayload.");
                response.put("code", 400);
                return ResponseEntity.badRequest().body(response);
            }

            Long stageXId = Long.parseLong(stageXIdObj.toString());
            String actionPayloadStr = actionPayloadObj.toString();
            logger.info("here at transform , stageXid {}, actionPayload,{}", stageXId, actionPayloadStr);
            Map<String, Object> payloadMap;
            try {
                payloadMap = objectMapper.readValue(actionPayloadStr, new TypeReference<>() {
                });
            } catch (Exception e) {
                response.put("outcome", "error");
                response.put("log_info", "Invalid actionPayload JSON: " + e.getMessage());
                response.put("code", 400);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }
            // Map<String, Object> payloadMap = objectMapper.readValue(actionPayloadStr, new TypeReference<>() {});

            // 3. Load StageX and related data collection stage
            StageX stageX = stageXRepository.findById(stageXId).orElse(null);
            if (stageX == null) {
                response.put("outcome", "error");
                response.put("log_info", "StageX not found for ID: " + stageXId);
                response.put("code", 404);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }

            Long pipelineXId = stageX.getPipelineX().getPxId();
            StageX dataCollectionStage = stageXRepository.findDataCollectionStageByPipelineXId(pipelineXId);
            if (dataCollectionStage == null) {
                response.put("outcome", "error");
                response.put("log_info", "No data collection stage found for pipelineXId: " + pipelineXId);
                response.put("code", 404);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }

            // 4. Parse payload from data collection stage
            String datapayloadJson = dataCollectionStage.getPayload();
            Map<String, Object> datapayloadMap;
            try {
                datapayloadMap = objectMapper.readValue(datapayloadJson, new TypeReference<>() {
                });
            } catch (Exception e) {
                response.put("outcome", "error");
                response.put("log_info", "Invalid datapayloadMap JSON: " + e.getMessage());
                response.put("code", 400);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }

            //Map<String, Object> datapayloadMap = objectMapper.readValue(datapayloadJson, new TypeReference<>() {});

            // 5. Extract file path and connection string
            String sqlFileName = payloadMap.getOrDefault("sqlFile", "").toString();
            String sqlFilePath = SQL_BASE_DIR + sqlFileName;
            String connectionString = datapayloadMap.getOrDefault("connectionString", "").toString();

            if (sqlFilePath.isEmpty() || connectionString.isEmpty()) {
                response.put("outcome", "error");
                response.put("log_info", "Missing 'file' in actionPayload or 'connectionString' in data collection stage.");
                response.put("code", 400);
                return ResponseEntity.badRequest().body(response);
            }

            // 6. Read SQL file
            StringBuilder sqlBuilder = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new FileReader(sqlFilePath))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    sqlBuilder.append(line).append("\n");
                }
            } catch (IOException e) {
                response.put("outcome", "error");
                response.put("log_info", "Failed to read SQL file: " + e.getMessage());
                response.put("code", 500);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
            }

            // 7. Execute SQL
            DriverManagerDataSource dataSource = new DriverManagerDataSource();
            dataSource.setUrl(connectionString);
            dataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");

            JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
            jdbcTemplate.execute(sqlBuilder.toString());

            response.put("outcome", "positive");
            response.put("log_info", "SQL executed successfully. executedFile {}" + sqlFilePath);
            response.put("executedFile", sqlFilePath);
            response.put("code", 200);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("outcome", "error");
            response.put("log_info", "Execution failed: " + e.getMessage());
            response.put("code", 500);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    public ResponseEntity<Map<String, Object>> load(Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();
        ObjectMapper objectMapper = new ObjectMapper();
        final String SQL_BASE_DIR = "D:/DMBackend/";
        try {
            // 1. Get stageXId
            Long stageXId = Long.parseLong(request.get("stageXId").toString());

            // 2. Parse actionPayload JSON
            String actionPayloadStr = request.get("actionPayload").toString();
            Map<String, Object> payloadMap;
            try {
                payloadMap = objectMapper.readValue(actionPayloadStr, new TypeReference<>() {
                });
            } catch (Exception e) {
                response.put("outcome", "error");
                response.put("log_info", "Invalid actionPayload JSON: " + e.getMessage());
                response.put("code", 400);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }
            // Map<String, Object> payloadMap = objectMapper.readValue(actionPayloadStr, new TypeReference<>() {});

            // 3. Load StageX and related data collection stage
            StageX stageX = stageXRepository.findById(stageXId).orElse(null);
            if (stageX == null) {
                response.put("outcome", "error");
                response.put("log_info", "StageX not found for ID: " + stageXId);
                response.put("code", 404);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }

            Long pipelineXId = stageX.getPipelineX().getPxId();
            StageX dataCollectionStage = stageXRepository.findDataCollectionStageByPipelineXId(pipelineXId);
            if (dataCollectionStage == null) {
                response.put("outcome", "error");
                response.put("log_info", "No data collection stage found for pipelineXId: " + pipelineXId);
                response.put("code", 404);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }

            // 4. Parse payload from data collection stage
            String datapayloadJson = dataCollectionStage.getPayload();
            Map<String, Object> datapayloadMap;
            try {
                datapayloadMap = objectMapper.readValue(datapayloadJson, new TypeReference<>() {
                });
            } catch (Exception e) {
                response.put("outcome", "error");
                response.put("log_info", "Invalid datapayloadMap JSON: " + e.getMessage());
                response.put("code", 400);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }

            //Map<String, Object> datapayloadMap = objectMapper.readValue(datapayloadJson, new TypeReference<>() {});

            // 5. Extract file path and connection string
            String sqlFileName = payloadMap.getOrDefault("sqlFile", "").toString();
            String sqlFilePath = SQL_BASE_DIR + sqlFileName;
            String connectionString = datapayloadMap.getOrDefault("connectionString", "").toString();

            if (sqlFilePath.isEmpty() || connectionString.isEmpty()) {
                response.put("outcome", "error");
                response.put("log_info", "Missing 'file' in actionPayload or 'connectionString' in data collection stage.");
                response.put("code", 400);
                return ResponseEntity.badRequest().body(response);
            }

            // 6. Read SQL file
            StringBuilder sqlBuilder = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new FileReader(sqlFilePath))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    sqlBuilder.append(line).append("\n");
                }
            } catch (IOException e) {
                response.put("outcome", "error");
                response.put("log_info", "Failed to read SQL file: " + e.getMessage());
                response.put("code", 500);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
            }

            // 7. Execute SQL
            DriverManagerDataSource dataSource = new DriverManagerDataSource();
            dataSource.setUrl(connectionString);
            dataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");

            JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
            jdbcTemplate.execute(sqlBuilder.toString());

            response.put("outcome", "positive");
            response.put("log_info", "SQL executed successfully. executedFile {}" + sqlFilePath);
            response.put("executedFile", sqlFilePath);
            response.put("code", 200);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("outcome", "error");
            response.put("log_info", "Execution failed: " + e.getMessage());
            response.put("code", 500);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    public ResponseEntity<Map<String, Object>> analysis(Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();
        ObjectMapper objectMapper = new ObjectMapper();

        try {
            final String SQL_BASE_DIR = "D:/DMBackend/";
            // 1. Get stageXId
            Long stageXId = Long.parseLong(request.get("stageXId").toString());

            // 2. Parse actionPayload JSON
            String actionPayloadStr = request.get("actionPayload").toString();
            Map<String, Object> payloadMap;
            try {
                payloadMap = objectMapper.readValue(actionPayloadStr, new TypeReference<>() {
                });
            } catch (Exception e) {
                response.put("outcome", "error");
                response.put("log_info", "Invalid actionPayload JSON: " + e.getMessage());
                response.put("code", 400);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }
            // Map<String, Object> payloadMap = objectMapper.readValue(actionPayloadStr, new TypeReference<>() {});

            // 3. Load StageX and related data collection stage
            StageX stageX = stageXRepository.findById(stageXId).orElse(null);
            if (stageX == null) {
                response.put("outcome", "error");
                response.put("log_info", "StageX not found for ID: " + stageXId);
                response.put("code", 404);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }

            Long pipelineXId = stageX.getPipelineX().getPxId();
            StageX dataCollectionStage = stageXRepository.findDataCollectionStageByPipelineXId(pipelineXId);
            if (dataCollectionStage == null) {
                response.put("outcome", "error");
                response.put("log_info", "No data collection stage found for pipelineXId: " + pipelineXId);
                response.put("code", 404);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }

            // 4. Parse payload from data collection stage
            String datapayloadJson = dataCollectionStage.getPayload();
            Map<String, Object> datapayloadMap;
            try {
                datapayloadMap = objectMapper.readValue(datapayloadJson, new TypeReference<>() {
                });
            } catch (Exception e) {
                response.put("outcome", "error");
                response.put("log_info", "Invalid datapayloadMap JSON: " + e.getMessage());
                response.put("code", 400);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }

            //Map<String, Object> datapayloadMap = objectMapper.readValue(datapayloadJson, new TypeReference<>() {});

            // 5. Extract file path and connection string
            String sqlFileName = payloadMap.getOrDefault("sqlFile", "").toString();
            String sqlFilePath = SQL_BASE_DIR + sqlFileName;
            String connectionString = datapayloadMap.getOrDefault("connectionString", "").toString();

            if (sqlFilePath.isEmpty() || connectionString.isEmpty()) {
                response.put("outcome", "error");
                response.put("log_info", "Missing 'file' in actionPayload or 'connectionString' in data collection stage.");
                response.put("code", 400);
                return ResponseEntity.badRequest().body(response);
            }

            // 6. Read SQL file
            StringBuilder sqlBuilder = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new FileReader(sqlFilePath))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    sqlBuilder.append(line).append("\n");
                }
            } catch (IOException e) {
                response.put("outcome", "error");
                response.put("log_info", "Failed to read SQL file: " + e.getMessage());
                response.put("code", 500);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
            }

            // 7. Execute SQL
            DriverManagerDataSource dataSource = new DriverManagerDataSource();
            dataSource.setUrl(connectionString);
            dataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");

            JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
            jdbcTemplate.execute(sqlBuilder.toString());

            response.put("outcome", "positive");
            response.put("log_info", "SQL executed successfully. executedFile {}" + sqlFilePath);
            response.put("executedFile", sqlFilePath);
            response.put("code", 200);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("outcome", "error");
            response.put("log_info", "Execution failed: " + e.getMessage());
            response.put("code", 500);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    public void generateReport(List<Map<String, Object>> records, Writer writer) throws IOException {
        writer.write("📦 Total Records: " + records.size() + "\n\n");

        Map<String, List<Object>> fieldValues = new LinkedHashMap<>();
        for (Map<String, Object> record : records) {
            for (Map.Entry<String, Object> entry : record.entrySet()) {
                fieldValues.computeIfAbsent(entry.getKey(), k -> new ArrayList<>()).add(entry.getValue());
            }
        }

        for (Map.Entry<String, List<Object>> entry : fieldValues.entrySet()) {
            String field = entry.getKey();
            List<Object> values = entry.getValue();

            writer.write("🔹 Field: " + field + "\n");

            Object first = values.stream().filter(Objects::nonNull).findFirst().orElse(null);
            if (first instanceof Number) {
                List<Double> nums = values.stream()
                        .filter(v -> v instanceof Number)
                        .map(v -> ((Number) v).doubleValue())
                        .toList();

                double min = nums.stream().mapToDouble(Double::doubleValue).min().orElse(0.0);
                double max = nums.stream().mapToDouble(Double::doubleValue).max().orElse(0.0);
                double avg = nums.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);

                writer.write("📘 Type: Number\n");
                writer.write("📈 Min: " + min + "\n");
                writer.write("📈 Max: " + max + "\n");
                writer.write("📈 Avg: " + String.format("%.2f", avg) + "\n");

            } else if (first instanceof Boolean) {
                long trueCount = values.stream().filter(v -> Boolean.TRUE.equals(v)).count();
                long falseCount = values.size() - trueCount;

                writer.write("📘 Type: Boolean\n");
                writer.write("  - true: " + trueCount + ", false: " + falseCount + "\n");

            } else {
                Set<Object> unique = new LinkedHashSet<>(values);
                writer.write("📘 Type: " + (first != null ? first.getClass().getSimpleName() : "Unknown") + "\n");
                writer.write("  - Unique Values: " + unique.size() + "\n");
                if (unique.size() <= 10) {
                    writer.write("  - Values: " + unique + "\n");
                }
            }

            writer.write("\n");
        }

        writer.write("🧾 Sample Record:\n");
        if (!records.isEmpty()) {
            Map<String, Object> sample = records.get(0);
            for (Map.Entry<String, Object> entry : sample.entrySet()) {
                writer.write("  " + entry.getKey() + ": " + entry.getValue() + "\n");
            }
        }
    }

    public List<Map<String, Object>> fromJsonFile(MultipartFile file) throws IOException {
        String jsonContent = new String(file.getBytes());
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.readValue(jsonContent, List.class);
    }

    public List<Map<String, Object>> fromDatabase(ResultSet rs) throws SQLException {
        List<Map<String, Object>> records = new ArrayList<>();
        ResultSetMetaData meta = rs.getMetaData();
        int columnCount = meta.getColumnCount();

        while (rs.next()) {
            Map<String, Object> row = new LinkedHashMap<>();
            for (int i = 1; i <= columnCount; i++) {
                row.put(meta.getColumnLabel(i), rs.getObject(i));
            }
            records.add(row);
        }

        return records;
    }

    public ResponseEntity<Map<String, Object>> handleReportGeneration2(Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();
        List<Map<String, Object>> records;

        try {
            Object stageXIdObj = request.get("stageXId");
            Object actionPayloadObj = request.get("actionPayload");

            if (stageXIdObj == null || actionPayloadObj == null) {
                response.put("outcome", "error");
                response.put("log_info", "Missing required parameters: stageXId or actionPayload.");
                response.put("code", 400);
                return ResponseEntity.badRequest().body(response);
            }

            Long stageXId = Long.parseLong(stageXIdObj.toString());
            String actionPayloadStr = actionPayloadObj.toString();
            logger.info("here at report , stageXid {}, actionPayload,{}", stageXId, actionPayloadStr);
            ObjectMapper objectMapper = new ObjectMapper();
            Map<String, Object> payloadMap;

            try {
                payloadMap = objectMapper.readValue(actionPayloadStr, new TypeReference<>() {
                });
            } catch (Exception e) {
                response.put("outcome", "error");
                response.put("log_info", "Invalid actionPayload JSON: " + e.getMessage());
                response.put("code", 400);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }

            StageX stageX = stageXRepository.findById(stageXId).orElse(null);
            if (stageX == null) {
                response.put("outcome", "error");
                response.put("log_info", "StageX not found for ID: " + stageXId);
                response.put("code", 404);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }

            Long pxId = stageX.getPipelineX().getPxId();
            StageX dataCollectionStage = stageXRepository.findDataCollectionStageByPipelineXId(pxId);
            if (dataCollectionStage == null) {
                response.put("outcome", "error");
                response.put("log_info", "No data collection stage found for pipelineXId: " + pxId);
                response.put("code", 404);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }

            logger.info("data collection fetched,{}", dataCollectionStage.getPayload());
            Map<String, Object> datapayloadMap;
            String Datapayload = dataCollectionStage.getPayload();

            try {
                datapayloadMap = objectMapper.readValue(Datapayload, new TypeReference<>() {
                });
            } catch (Exception e) {
                response.put("outcome", "error");
                response.put("log_info", "Invalid actionPayload JSON: " + e.getMessage());
                response.put("code", 400);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }

            String file = (payloadMap.containsKey("file")) ? payloadMap.get("file").toString() : null;
            String tableName = (payloadMap.containsKey("tableName")) ? payloadMap.get("tableName").toString() : null;
            String connectionString = (datapayloadMap.containsKey("connectionString")) ? datapayloadMap.get("connectionString").toString() : null;
            String outputFileName = (payloadMap.containsKey("outputFileName")) ? payloadMap.get("outputFileName").toString() : null;

            // Read records from file or database
//            if (file != null && !file.isEmpty()) {
//                //records = fromJsonFile(new File(file));
//            } else if (tableName != null && !tableName.isEmpty()) {
//                if (connectionString == null || connectionString.isEmpty()) {
//                    response.put("outcome", "error");
//                    response.put("log_info", "Missing connection string.");
//                    response.put("code", 400);
//                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
//                }
//
//                try (
//                        Connection conn = DriverManager.getConnection(connectionString);
//                        Statement stmt = conn.createStatement();
//                        ResultSet rs = stmt.executeQuery("SELECT * FROM " + tableName)
//                ) {
//                    records = fromDatabase(rs);
//                }
//            } else {
//                response.put("outcome", "error");
//                response.put("log_info", "Either file or tableName must be provided.");
//                response.put("code", 400);
//                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
//            }


            if (tableName != null && !tableName.isEmpty()) {
                if (connectionString == null || connectionString.isEmpty()) {
                    response.put("outcome", "error");
                    response.put("log_info", "Missing connection string.");
                    response.put("code", 400);
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
                }

                try (
                        Connection conn = DriverManager.getConnection(connectionString);
                        Statement stmt = conn.createStatement();
                        ResultSet rs = stmt.executeQuery("SELECT * FROM " + tableName)
                ) {
                    records = fromDatabase(rs);
                } catch (SQLException e) {
                    response.put("outcome", "error");
                    response.put("log_info", "Database error: " + e.getMessage());
                    response.put("code", 500);
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
                }
            } else {
                response.put("outcome", "error");
                response.put("log_info", "Table name must be provided.");
                response.put("code", 400);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }
            // Prepare output directory and file
            String directoryPath = "D:/DMBackend";
            File directory = new File(directoryPath);
            if (!directory.exists()) {
                directory.mkdirs();
            }

            if (outputFileName == null || outputFileName.trim().isEmpty()) {
                outputFileName = "report_" + System.currentTimeMillis() + ".txt";
            }

            File fileOut = new File(directory, outputFileName);
            try (Writer writer = new FileWriter(fileOut)) {
                generateReport(records, writer);
            }

            response.put("outcome", "positive");
            response.put("log_info", "Report generated successfully and saved to: " + fileOut.getAbsolutePath());
            response.put("outputFile", fileOut.getAbsolutePath());
            response.put("code", 200);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, Object> failureResponse = new HashMap<>();
            failureResponse.put("outcome", "error");
            failureResponse.put("log_info", "Report generation failed: " + e.getMessage());
            failureResponse.put("code", 500);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(failureResponse);
        }
    }


    public ResponseEntity<Map<String, Object>> sendEmails(Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();
        ObjectMapper objectMapper = new ObjectMapper();

        try {
            Long stageXId = Long.parseLong(request.get("stageXId").toString());
            logger.info("Received email event for stageXId: {}", stageXId);

            String actionPayloadStr = request.get("actionPayload").toString();
            Map<String, Object> payloadMap = objectMapper.readValue(actionPayloadStr, new TypeReference<>() {
            });

            // Get pipelineId → Find data collection stage
            StageX stageX = stageXRepository.findById(stageXId).orElse(null);
            if (stageX == null) {
                response.put("outcome", "positive");
                response.put("log_info", "StageX not found for ID: " + stageXId);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }

            Long pipelineXId = stageX.getPipelineX().getPxId();
            StageX dataCollectionStage = stageXRepository.findDataCollectionStageByPipelineXId(pipelineXId);
            if (dataCollectionStage == null) {
                response.put("outcome", "positive");
                response.put("log_info", "No data collection stage found for pipelineXId: " + pipelineXId);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }

            String datapayloadJson = dataCollectionStage.getPayload();
            Map<String, Object> datapayloadMap = objectMapper.readValue(datapayloadJson, new TypeReference<>() {
            });
            String connectionString = datapayloadMap.getOrDefault("connectionString", "").toString();

            if (connectionString.isEmpty()) {
                response.put("outcome", "positive");
                response.put("log_info", "Connection string not found in data collection stage.");
                return ResponseEntity.badRequest().body(response);
            }

            String subject = payloadMap.getOrDefault("subject", "").toString();
            String body = payloadMap.getOrDefault("body", "").toString();
            String attachmentFile = payloadMap.getOrDefault("attachmentFile", "").toString();

            List<String> emailList = new ArrayList<>();

            // Case 1: CSV input
            if (payloadMap.containsKey("csvFile")) {
                String csvFile = payloadMap.get("csvFile").toString();
                String basePath = "D:/DMBackend/";
                File file = new File(basePath + csvFile);
                logger.info("Looking for CSV file at: {}", file.getAbsolutePath());
                if (!file.exists()) {
                    response.put("outcome", "positive");
                    response.put("log_info", "CSV file not found: " + csvFile);
                    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
                }

                try (BufferedReader br = new BufferedReader(new FileReader(file))) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        emailList.add(line.trim());
                    }
                } catch (IOException e) {
                    response.put("outcome", "positive");
                    response.put("log_info", "Error reading CSV file: " + e.getMessage());
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
                }

                logger.info("Fetched {} email(s) from CSV file: {}", emailList.size(), csvFile);
            }

            // Case 2: Database input
            else if (payloadMap.containsKey("tableName") && payloadMap.containsKey("columnName")) {
                String tableName = payloadMap.get("tableName").toString();
                String columnName = payloadMap.get("columnName").toString();

                try (Connection conn = DriverManager.getConnection(connectionString)) {
                    String query = "SELECT " + columnName + " FROM " + tableName;
                    Statement stmt = conn.createStatement();
                    ResultSet rs = stmt.executeQuery(query);
                    while (rs.next()) {
                        emailList.add(rs.getString(columnName));
                    }
                } catch (SQLException e) {
                    response.put("outcome", "positive");
                    response.put("log_info", "Database error while fetching emails: " + e.getMessage());
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
                }

                logger.info("Fetched {} email(s) from database table: {}", emailList.size(), tableName);
            } else {
                response.put("outcome", "positive");
                response.put("log_info", "No valid source for email list found. Provide either 'csvFile' or DB details.");
                return ResponseEntity.badRequest().body(response);
            }

//            // Send emails
//            for (String toEmail : emailList) {
//                sendEmail(toEmail, subject, body, attachmentFile);
//            }

            // Async email sending
            for (String toEmail : emailList) {
                String finalAttachment = attachmentFile;  // for lambda scope
                CompletableFuture.runAsync(() -> {
                    try {
                        sendEmail(toEmail, subject, body, finalAttachment);
                    } catch (Exception e) {
                        logger.error("❌ Failed to send email to {}: {}", toEmail, e.getMessage());
                    }
                });
            }

            logger.info("🚀 Email sending started asynchronously for {} recipient(s)", emailList.size());
            response.put("outcome", "positive");
            response.put("log_info", "Email sending started for " + emailList.size() + " recipient(s).");
            return ResponseEntity.ok(response);

//            logger.info("Emails successfully sent to {} recipient(s)", emailList.size());
//            response.put("outcome", "positive");
//            response.put("log_info", "Emails sent to " + emailList.size() + " recipient(s).");
//            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Unexpected error during email sending: ", e);
            response.put("outcome", "positive");
            response.put("log_info", "Unexpected error occurred: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

//    private void sendEmail(String toEmail, String subject, String body, String attachmentFile) throws Exception {
//        Properties props = new Properties();
//        props.put("mail.smtp.auth", "true");
//        props.put("mail.smtp.starttls.enable", "true");
//        props.put("mail.smtp.host", "smtp.gmail.com");
//        props.put("mail.smtp.port", "587");
//
//        Session session = Session.getInstance(props, new Authenticator() {
//            protected PasswordAuthentication getPasswordAuthentication() {
//                return new PasswordAuthentication(senderEmail, senderPassword);
//            }
//        });
//
//        Message message = new MimeMessage(session);
//        message.setFrom(new InternetAddress(senderEmail));
//        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
//        message.setSubject(subject);
//
//        MimeBodyPart textPart = new MimeBodyPart();
//        textPart.setText(body);
//
//        MimeMultipart multipart = new MimeMultipart();
//        multipart.addBodyPart(textPart);
//
//        if (attachmentFile != null && !attachmentFile.isEmpty()) {
//            String fullPath = "D:/DMBackend/"+ attachmentFile;
//            File file = new File(fullPath);
//            if (file.exists() && file.canRead()) {
//                MimeBodyPart attachment = new MimeBodyPart();
//                attachment.attachFile(file);
//                multipart.addBodyPart(attachment);
//                logger.info("✅ Attached file: {}", fullPath);
//            } else {
//                logger.warn("⚠️ Attachment file not found or unreadable: {}", fullPath);
//
//            }
//        }
//
//        message.setContent(multipart);
//        logger.info("Sending email to: {}", toEmail);
//        Transport.send(message);
//    }

    private void sendEmail(String toEmail, String subject, String body, String attachmentFile) throws Exception {
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");

        Session session = Session.getInstance(props, new Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(senderEmail, senderPassword);
            }
        });

        Message message = new MimeMessage(session);
        message.setFrom(new InternetAddress(senderEmail));
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
        message.setSubject(subject);

        MimeBodyPart textPart = new MimeBodyPart();
        textPart.setText(body);

        MimeMultipart multipart = new MimeMultipart();
        multipart.addBodyPart(textPart);

        if (attachmentFile != null && !attachmentFile.isEmpty()) {
            String fullPath = "D:/DMBackend/" + attachmentFile;
            File file = new File(fullPath);
            if (file.exists() && file.canRead()) {
                MimeBodyPart attachment = new MimeBodyPart();
                attachment.attachFile(file);
                multipart.addBodyPart(attachment);
                logger.info("✅ Attached file: {}", fullPath);
            } else {
                logger.warn("⚠️ Attachment file not found or unreadable: {}", fullPath);
            }
        }

        message.setContent(multipart);
        logger.info("📧 Sending email to: {}", toEmail);
        Transport.send(message);
    }

    public ResponseEntity<Map<String, Object>> loadJsonData(Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();
        ObjectMapper mapper = new ObjectMapper();

        try {
            Long stageXId = Long.parseLong(request.get("stageXId").toString());
            String payloadStr = request.get("actionPayload").toString();
            Map<String, Object> payloadMap = mapper.readValue(payloadStr, new TypeReference<>() {
            });

            // Fetch the Data Collection Stage to get connection string
            StageX currentStage = stageXRepository.findById(stageXId).orElse(null);
            if (currentStage == null) {
                response.put("outcome", "positive");
                response.put("log_info", "StageX not found for ID: " + stageXId);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }

            Long pipelineId = currentStage.getPipelineX().getPxId();
            StageX dataCollectionStage = stageXRepository.findDataCollectionStageByPipelineXId(pipelineId);
            if (dataCollectionStage == null) {
                response.put("outcome", "positive");
                response.put("log_info", "Data collection stage not found for pipelineXId: " + pipelineId);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }

            String connectionString = mapper.readValue(dataCollectionStage.getPayload(), Map.class)
                    .getOrDefault("connectionString", "").toString();

            if (connectionString.isEmpty()) {
                response.put("outcome", "positive");
                response.put("log_info", "Connection string missing in data collection stage.");
                return ResponseEntity.badRequest().body(response);
            }

            String fileName = payloadMap.getOrDefault("inputFileName", "").toString();
            String tableName = payloadMap.getOrDefault("tableName", "").toString();

            if (fileName.isEmpty() || tableName.isEmpty()) {
                response.put("outcome", "positive");
                response.put("log_info", "fileName or tableName is missing in the payload.");
                return ResponseEntity.badRequest().body(response);
            }

            // Call your JSON-to-DB loader
            Map<String, Object> loadResult = loadJsonToDb(fileName, tableName, connectionString);
            boolean isSuccess = (boolean) loadResult.get("success");
            String message = loadResult.get("message").toString();
            logger.info("Load Result: {}", message);

            if(!isSuccess){
                response.put("outcome", "error");
                response.put("log_info", message);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }

            response.put("outcome", "positive");
            response.put("log_info", message);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("❌ Error in LoadService: ", e);
            response.put("outcome", "positive");
            response.put("log_info", "Exception occurred: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    public Map<String, Object> loadJsonToDb(String fileName, String tableName, String connectionString) {
        Map<String, Object> result = new HashMap<>();
        try (Connection conn = DriverManager.getConnection(connectionString)) {

            ObjectMapper mapper = new ObjectMapper();
            String fullPath = "D:/DMBackend/" + fileName;
            File file = new File(fullPath);
            JsonNode jsonArray = mapper.readTree(file);

            if (!jsonArray.isArray()) {
                result.put("success", false);
                result.put("message", "Invalid JSON: root should be an array of objects.");
                return result;
            }

            DatabaseMetaData metaData = conn.getMetaData();
            ResultSet columnsRs = metaData.getColumns(null, null, tableName, null);
            List<String> tableColumns = new ArrayList<>();

            while (columnsRs.next()) {
                tableColumns.add(columnsRs.getString("COLUMN_NAME"));
            }

            if (tableColumns.isEmpty()) {
                result.put("success", false);
                result.put("message", "No columns found for table: " + tableName);
                return result;
            }

            for (JsonNode record : jsonArray) {
                Set<String> jsonFields = new HashSet<>();
                record.fieldNames().forEachRemaining(jsonFields::add);

                Set<String> unmatchedFields = new HashSet<>(jsonFields);
                unmatchedFields.removeAll(tableColumns);

                if (!unmatchedFields.isEmpty()) {
                    result.put("success", false);
                    result.put("message", "Invalid JSON: Unmatched fields - " + unmatchedFields);
                    return result;
                }

                List<String> matchedCols = new ArrayList<>();
                List<Object> values = new ArrayList<>();

                for (String col : tableColumns) {
                    if (record.has(col)) {
                        matchedCols.add(col);
                        values.add(record.get(col).asText());
                    }
                }

                if (matchedCols.isEmpty()) continue;

                String colsPart = String.join(",", matchedCols);
                String placeholders = matchedCols.stream().map(c -> "?").collect(Collectors.joining(","));
                String sql = "INSERT INTO " + tableName + " (" + colsPart + ") VALUES (" + placeholders + ")";

                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    for (int i = 0; i < values.size(); i++) {
                        stmt.setObject(i + 1, values.get(i));
                    }
                    stmt.executeUpdate();
                }
            }

            result.put("success", true);
            result.put("message", "Success: JSON data inserted into " + tableName);
            return result;

        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "Exception: " + e.getMessage());
            return result;
        }
    }

    public ResponseEntity<Map<String, Object>> compareCategorySales(Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();
        ObjectMapper mapper = new ObjectMapper();

        try {
            Long stageXId = Long.parseLong(request.get("stageXId").toString());
            logger.info("Parsed stageXId: {}", stageXId);
            Map<String, Object> payload = mapper.readValue(request.get("actionPayload").toString(), new TypeReference<>() {});
            logger.info("Parsed payload: {}", payload);
            String category1 = payload.get("category1").toString();
            String category2 = payload.get("category2").toString();
            logger.info("Comparing category1: '{}' with category2: '{}'", category1, category2);
            // Step 1: Fetch connection string from data collection stage
            StageX currentStage = stageXRepository.findById(stageXId).orElse(null);
            if (currentStage == null) {
                response.put("outcome", "positive");
                response.put("log_info", "StageX not found for ID: " + stageXId);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }

            Long pipelineId = currentStage.getPipelineX().getPxId();
            StageX dataCollectionStage = stageXRepository.findDataCollectionStageByPipelineXId(pipelineId);
            if (dataCollectionStage == null) {
                response.put("outcome", "positive");
                response.put("log_info", "No data collection stage found for pipeline ID: " + pipelineId);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }

            Map<String, Object> dataPayload = mapper.readValue(dataCollectionStage.getPayload(), new TypeReference<>() {});
            String connectionString = dataPayload.getOrDefault("connectionString", "").toString();

            if (connectionString.isEmpty()) {
                response.put("outcome", "positive");
                response.put("log_info", "Connection string not found in data collection stage.");
                return ResponseEntity.badRequest().body(response);
            }

            // Step 2: Compare category sales
            double sales1 = 0.0;
            double sales2 = 0.0;

            try (Connection conn = DriverManager.getConnection(connectionString)) {
                String query = """
                        SELECT p.product_category, SUM(f.sales) AS total_sales
                        FROM fact_sales f
                        JOIN dim_prod p ON f.prod_id = p.prod_id
                        WHERE p.product_category IN (?, ?)
                        GROUP BY p.product_category
                        """;

                try (PreparedStatement stmt = conn.prepareStatement(query)) {
                    stmt.setString(1, category1);
                    stmt.setString(2, category2);

                    try (ResultSet rs = stmt.executeQuery()) {
                        while (rs.next()) {
                            String cat = rs.getString("product_category");
                            double total = rs.getDouble("total_sales");

                            if (cat.equalsIgnoreCase(category1)) {
                                sales1 = total;
                            } else if (cat.equalsIgnoreCase(category2)) {
                                sales2 = total;
                            }
                        }
                    }
                }
            }

            String outcome = sales1 > sales2 ? "positive" : "negative";
            String log = String.format("Category1 (%s) sales = %.2f, Category2 (%s) sales = %.2f", category1, sales1, category2, sales2);

            response.put("outcome", outcome);
            response.put("log_info", log);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error in category sales comparison: ", e);
            response.put("outcome", "Error");
            response.put("log_info", "Unexpected error: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    public ResponseEntity<Map<String, Object>> sqlQuery(Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();
        ObjectMapper objectMapper = new ObjectMapper();

        try {
            // 1. Get stageXId
            Long stageXId = Long.parseLong(request.get("stageXId").toString());

            // 2. Parse actionPayload JSON
            String actionPayloadStr = request.get("actionPayload").toString();
            Map<String, Object> payloadMap;
            try {
                payloadMap = objectMapper.readValue(actionPayloadStr, new TypeReference<>() {});
            } catch (Exception e) {
                response.put("outcome", "error");
                response.put("log_info", "Invalid actionPayload JSON: " + e.getMessage());
                response.put("code", 400);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }

            // 3. Load StageX and related data collection stage
            StageX stageX = stageXRepository.findById(stageXId).orElse(null);
            if (stageX == null) {
                response.put("outcome", "error");
                response.put("log_info", "StageX not found for ID: " + stageXId);
                response.put("code", 404);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }

            Long pipelineXId = stageX.getPipelineX().getPxId();
            StageX dataCollectionStage = stageXRepository.findDataCollectionStageByPipelineXId(pipelineXId);
            if (dataCollectionStage == null) {
                response.put("outcome", "error");
                response.put("log_info", "No data collection stage found for pipelineXId: " + pipelineXId);
                response.put("code", 404);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }

            // 4. Parse payload from data collection stage
            String datapayloadJson = dataCollectionStage.getPayload();
            Map<String, Object> datapayloadMap;
            try {
                datapayloadMap = objectMapper.readValue(datapayloadJson, new TypeReference<>() {});
            } catch (Exception e) {
                response.put("outcome", "error");
                response.put("log_info", "Invalid datapayloadMap JSON: " + e.getMessage());
                response.put("code", 400);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }

            // 5. Extract inputs
            String connectionString = datapayloadMap.getOrDefault("connectionString", "").toString();
            String categoryName = payloadMap.getOrDefault("categoryName", "").toString();
            String outputTableName = payloadMap.getOrDefault("outputTableName", "").toString();

            if (connectionString.isEmpty() || categoryName.isEmpty() || outputTableName.isEmpty()) {
                response.put("outcome", "error");
                response.put("log_info", "Missing 'connectionString' or 'categoryName' or 'outputTableName'.");
                response.put("code", 400);
                return ResponseEntity.badRequest().body(response);
            }

            // 6. Prepare SQL query
            String sqlQuery = String.format(
                    "CREATE TABLE IF NOT EXISTS %s AS " +
                            "WITH sales_summary AS (" +
                            "    SELECT " +
                            "        dc.state, " +
                            "        dp.product_category, " +
                            "        dp.product_sub_category, " +
                            "        SUM(fs.sales) AS total_sales, " +
                            "        SUM(fs.profit) AS total_profit " +
                            "    FROM " +
                            "        fact_sales fs " +
                            "    JOIN " +
                            "        dim_cust dc ON fs.cust_id = dc.cust_id " +
                            "    JOIN " +
                            "        dim_prod dp ON fs.prod_id = dp.prod_id " +
                            "    WHERE " +
                            "        dp.product_category = '%s' " +  // Input category here
                            "    GROUP BY " +
                            "        dc.state, dp.product_category, dp.product_sub_category" +
                            "), " +
                            "state_subcat_sales AS (" +
                            "    SELECT " +
                            "        state, " +
                            "        product_sub_category, " +
                            "        SUM(total_sales) AS state_subcat_total_sales " +
                            "    FROM " +
                            "        sales_summary " +
                            "    GROUP BY " +
                            "        state, product_sub_category" +
                            "), " +
                            "state_sales AS (" +
                            "    SELECT " +
                            "        state, " +
                            "        SUM(total_sales) AS total_sales_in_state " +
                            "    FROM " +
                            "        sales_summary " +
                            "    GROUP BY " +
                            "        state" +
                            ") " +
                            "SELECT " +
                            "    s.product_category, " +
                            "    s.product_sub_category, " +
                            "    ROUND(SUM(CASE WHEN s.state = 'Bihar' THEN s.total_sales ELSE 0 END), 2) AS Bihar_Sales, " +
                            "    ROUND(SUM(CASE WHEN s.state = 'Delhi' THEN s.total_sales ELSE 0 END), 2) AS Delhi_Sales, " +
                            "    ROUND(SUM(CASE WHEN s.state = 'Karnataka' THEN s.total_sales ELSE 0 END), 2) AS Karnataka_Sales, " +
                            "    ROUND(SUM(CASE WHEN s.state = 'Kerala' THEN s.total_sales ELSE 0 END), 2) AS Kerala_Sales, " +
                            "    ROUND(SUM(CASE WHEN s.state = 'Maharashtra' THEN s.total_sales ELSE 0 END), 2) AS Maharashtra_Sales, " +
                            "    ROUND(SUM(CASE WHEN s.state = 'Tamil Nadu' THEN s.total_sales ELSE 0 END), 2) AS Tamil_Nadu_Sales, " +
                            "    ROUND(SUM(CASE WHEN s.state = 'Telangana' THEN s.total_sales ELSE 0 END), 2) AS Telangana_Sales, " +
                            "    ROUND(SUM(CASE WHEN s.state = 'West Bengal' THEN s.total_sales ELSE 0 END), 2) AS West_Bengal_Sales, " +
                            "    ROUND(SUM(s.total_sales), 2) AS total_sales_in_subcategory " +
                            "FROM " +
                            "    sales_summary s " +
                            "JOIN " +
                            "    state_subcat_sales st ON s.state = st.state AND s.product_sub_category = st.product_sub_category " +
                            "JOIN " +
                            "    state_sales ss ON s.state = ss.state " +
                            "GROUP BY " +
                            "    s.product_category, s.product_sub_category " +
                            "ORDER BY " +
                            "    s.product_category, s.product_sub_category;",
                    outputTableName, categoryName
            );

            // 7. Execute SQL
            DriverManagerDataSource dataSource = new DriverManagerDataSource();
            dataSource.setUrl(connectionString);
            dataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");

            JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
            jdbcTemplate.execute(sqlQuery);

            response.put("outcome", "positive");
            response.put("log_info", "SQL executed successfully. outputTableName: " + outputTableName);
//            response.put("executedQuery", sqlQuery);
            response.put("code", 200);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("outcome", "error");
            response.put("log_info", "Execution failed: " + e.getMessage());
            response.put("code", 500);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    public ResponseEntity<Map<String, Object>> export(Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();
        ObjectMapper objectMapper = new ObjectMapper();

        try {
            // 1. Get stageXId
            Long stageXId = Long.parseLong(request.get("stageXId").toString());

            // 2. Parse actionPayload JSON
            String actionPayloadStr = request.get("actionPayload").toString();
            Map<String, Object> payloadMap;
            try {
                payloadMap = objectMapper.readValue(actionPayloadStr, new TypeReference<>() {});
            } catch (Exception e) {
                response.put("outcome", "error");
                response.put("log_info", "Invalid actionPayload JSON: " + e.getMessage());
                response.put("code", 400);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }

            // 3. Load StageX and related data collection stage
            StageX stageX = stageXRepository.findById(stageXId).orElse(null);
            if (stageX == null) {
                response.put("outcome", "error");
                response.put("log_info", "StageX not found for ID: " + stageXId);
                response.put("code", 404);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }

            Long pipelineXId = stageX.getPipelineX().getPxId();
            StageX dataCollectionStage = stageXRepository.findDataCollectionStageByPipelineXId(pipelineXId);
            if (dataCollectionStage == null) {
                response.put("outcome", "error");
                response.put("log_info", "No data collection stage found for pipelineXId: " + pipelineXId);
                response.put("code", 404);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }

            // 4. Extract connection string from previous stage
            String datapayloadJson = dataCollectionStage.getPayload();
            Map<String, Object> datapayloadMap;
            try {
                datapayloadMap = objectMapper.readValue(datapayloadJson, new TypeReference<>() {});
            } catch (Exception e) {
                response.put("outcome", "error");
                response.put("log_info", "Invalid datapayloadMap JSON: " + e.getMessage());
                response.put("code", 400);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }

            String connectionString = datapayloadMap.getOrDefault("connectionString", "").toString();
            if (connectionString.isEmpty()) {
                response.put("outcome", "error");
                response.put("log_info", "Missing 'connectionString' in data collection stage payload.");
                response.put("code", 400);
                return ResponseEntity.badRequest().body(response);
            }

            // 5. Extract user inputs
            String tableName = payloadMap.getOrDefault("tableName", "").toString();
            String csvFileName = payloadMap.getOrDefault("csvFileName", "").toString();

            if (tableName.isEmpty() || csvFileName.isEmpty()) {
                response.put("outcome", "error");
                response.put("log_info", "Missing 'tableName' or 'csvFileName' in actionPayload.");
                response.put("code", 400);
                return ResponseEntity.badRequest().body(response);
            }

            // 6. Set up JDBC
            DriverManagerDataSource dataSource = new DriverManagerDataSource();
            dataSource.setUrl(connectionString);
            dataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");

            JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);

            // 7. Query the table
            String query = "SELECT * FROM " + tableName;
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(query);

            if (rows.isEmpty()) {
                response.put("outcome", "error");
                response.put("log_info", "Table '" + tableName + "' is empty.");
                response.put("code", 204);
                return ResponseEntity.status(HttpStatus.NO_CONTENT).body(response);
            }

            // 8. Prepare CSV file
            File dir = new File("D:/DMBackend");
            if (!dir.exists()) dir.mkdirs();

            File csvFile = new File(dir, csvFileName.endsWith(".csv") ? csvFileName : csvFileName + ".csv");

            try (PrintWriter pw = new PrintWriter(csvFile)) {
                // Write header
                Set<String> headers = rows.get(0).keySet();
                pw.println(String.join(",", headers));

                // Write each row
                for (Map<String, Object> row : rows) {
                    List<String> values = new ArrayList<>();
                    for (String header : headers) {
                        Object value = row.get(header);
                        values.add(value != null ? value.toString().replace(",", " ") : "");
                    }
                    pw.println(String.join(",", values));
                }
            }

            response.put("outcome", "positive");
            response.put("log_info", "CSV file created successfully at: " + csvFile.getAbsolutePath());
            response.put("code", 200);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("outcome", "error");
            response.put("log_info", "Execution failed: " + e.getMessage());
            response.put("code", 500);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}





