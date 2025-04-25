package com.rishika.backend.service;

import com.rishika.backend.entity.*;
import com.rishika.backend.enums.DependencyType;
import com.rishika.backend.filter.JWTFilter;
import com.rishika.backend.mapper.PipelineXMapper;
import com.rishika.backend.repo.PipelineRepo;
import com.rishika.backend.repo.PipelineXRepo;
import com.rishika.backend.repo.StageRepo;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;

import com.rishika.backend.mapper.StageXMapper;
import com.rishika.backend.repo.StageDependencyRepo;
import com.rishika.backend.repo.StageXDependencyRepo;
import com.rishika.backend.repo.StageXRepo;
import org.springframework.transaction.annotation.Transactional;
import com.fasterxml.jackson.core.type.TypeReference;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EngineService {

    private final PipelineRepo pipelineRepository;
    private final PipelineXRepo pipelineXRepository;
    private final StageRepo stageRepository;
    private final PipelineXMapper pipelineXMapper;
    private final StageXMapper stageXMapper;
    private final StageXRepo stageXRepository;
    private final StageDependencyRepo stageDependencyRepository;
    private final StageXDependencyRepo stageXDependencyRepository;
    ObjectMapper objectMapper = new ObjectMapper();
    private static final Logger logger = LoggerFactory.getLogger(JWTFilter.class);
    @Autowired
    private ExecutionService executionService;
    @Value("${folderPath}")
    private String folderPath;

    @Transactional
    public ResponseEntity<Map<String, Object>> createPipelineX(String pidString,String jwtToken) {
        Map<String, Object> response = new HashMap<>();
        logger.debug("here pipelineX ");
        try {
            Long pid = Long.parseLong(pidString);

            Optional<Pipeline> optionalPipeline = pipelineRepository.findById(pid);
            if (optionalPipeline.isEmpty()) {
                response.put("status", "error");
                response.put("message", "Pipeline not found with PID: " + pid);
                response.put("pxid", null);
                return ResponseEntity.badRequest().body(response);
            }

            Pipeline pipeline = optionalPipeline.get();

            if (pipeline.getUser() == null) {
                response.put("status", "error");
                response.put("message", "Pipeline has no associated user.");
                response.put("pxid", null);
                return ResponseEntity.badRequest().body(response);
            }


            if ("DISABLED".equalsIgnoreCase(pipeline.getStatus())) {
                response.put("status", "error");
                response.put("message", "Pipeline is disabled.");
                response.put("pxid", null);
                return ResponseEntity.badRequest().body(response);
            }

            Stage firstStage = pipeline.getFirstStage();
            if (firstStage == null || !stageRepository.existsById(firstStage.getSid())) {
                response.put("status", "error");
                response.put("message", "First Stage ID is invalid or missing.");
                response.put("pxid", null);

                return ResponseEntity.badRequest().body(response);
            }


            PipelineX pipelineX = pipelineXMapper.toPipelineX(pipeline);
            PipelineX saved = pipelineXRepository.save(pipelineX);
            logger.debug("Created pipelineX with PXID: {}", saved.getPxId());

            Set<String> userStageIds;
            try {
                userStageIds = createStageXAndDependencies(pid, saved);
                if (userStageIds == null || userStageIds.isEmpty()) {
                    response.put("status", "error");
                    response.put("message", "Failed to create StageX dependencies.");
                    return ResponseEntity.internalServerError().body(response);
                }
            } catch (Exception e) {
                response.put("status", "error");
                response.put("message", "Exception during StageX creation: " + e.getMessage());
                throw new RuntimeException("Failed to create StageX and dependencies", e);
            }

            Map<String, Boolean> progress = new HashMap<>();
            for (String userStageId : userStageIds) {
                progress.put(userStageId, false);
            }
            Optional<StageX> optionalFirstStageX = stageXRepository.findByPipelineXAndStage(saved, pipeline.getFirstStage());

            if (optionalFirstStageX.isEmpty()) {
                throw new RuntimeException("Failed to locate first StageX corresponding to original first Stage.");
            }

            StageX firstStageX = optionalFirstStageX.get();
            saved.setFirstStageX(firstStageX);


            try {
                String progressJson = objectMapper.writeValueAsString(progress);
                saved.setPipelineProgress(progressJson);
                pipelineXRepository.save(saved);
            } catch (Exception e) {
                logger.error("Failed to serialize pipeline progress to JSON", e);
                throw new RuntimeException("Error while setting pipeline progress", e);
            }

            try {
                File logsDir = new File(folderPath);
                logger.info("logss Dir {}", logsDir.getAbsolutePath());
                if (!logsDir.exists()) {
                    logsDir.mkdirs();
                }

                String fileName = "pipelineX_" + saved.getPxId() + "_user_" + saved.getUser().getUserId() + ".log";
                logger.info("Writing log file " + fileName);
                File logFile = new File(logsDir, fileName);

                if (logFile.createNewFile()) {
                    FileWriter writer = new FileWriter(logFile);
                    writer.write("Log file created for PipelineX ID: " + saved.getPxId());
                    writer.close();

                    saved.setLogInfo(logFile.getAbsolutePath());
                    pipelineXRepository.flush();
                    pipelineXRepository.save(saved);

                } else {
                    logger.debug("Log file already exists: {}", logFile.getAbsolutePath());
                }
            } catch (IOException e) {
                logger.error("Failed to create log file", e);
                throw new RuntimeException("Error while creating log file", e);
            }


            stageXRepository.flush();
            pipelineXRepository.flush();
            stageXDependencyRepository.flush();

         // String jwtToken2 = SecurityContextHolder.getContext().getAuthentication().getCredentials().toString();
            executionService.triggerPipelineExecution(saved.getPxId(), jwtToken);



            response.put("status", "success");
            response.put("message", "PipelineX created successfully.");
            response.put("pxid", saved.getPxId());
            return ResponseEntity.ok(response);

        } catch (NumberFormatException e) {
            response.put("status", "error");
            response.put("message", "Invalid pipeline ID format.");
            response.put("pxid", null);
            throw e;


        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", "Unexpected error: " + e.getMessage());
            response.put("pxid", null);
           throw e;

        }
        //return ResponseEntity.ok(response);
    }

    public Set<String> createStageXAndDependencies(Long pipelineId, PipelineX pipelineX) {
        try {
            Stage startStage = pipelineX.getPipeline().getFirstStage(); // Use provided first stage
            if (startStage == null) return Collections.emptySet();;

            Map<Long, StageX> stageXMap = new HashMap<>();
            Set<Long> visited = new HashSet<>();
            Queue<Stage> queue = new LinkedList<>();
            List<StageDependency> pendingDependencies = new ArrayList<>();
            queue.offer(startStage);

            logger.debug("Starting StageX and StageXDependency creation...");

            while (!queue.isEmpty()) {
                Stage current = queue.poll();
                if (visited.contains(current.getSid())) {
                    logger.debug("Skipping already visited SID: {}", current.getSid());
                    continue;
                }

                visited.add(current.getSid());
                logger.debug("Processing SID: {}", current.getSid());

                // Build and save StageX using mapper
                StageX stageX = stageXMapper.buildStageX(current, pipelineX);
                StageX savedStageX = stageXRepository.save(stageX);
                logger.debug("Saved StageX with SXID: {} for SID: {}", savedStageX.getSxId(), current.getSid());

                stageXMap.put(current.getSid(), savedStageX);

                // Get all dependencies where current stage is the parent (depends_on)
                List<StageDependency> dependencies = stageDependencyRepository.findByDependsOnSid(current.getSid());
                logger.debug("For SID {} found {} StageDependency(ies)", current.getSid(), dependencies.size());

                for (StageDependency dep : dependencies) {
                    Stage child = dep.getStage(); // Stage that depends on `current`
                    logger.debug("Found child stage SID: {} (depends on SID: {})", child.getSid(), current.getSid());

                    // Enqueue the child stage to process
                    if (!visited.contains(child.getSid())) {
                        queue.offer(child);
                        logger.debug("Enqueued child SID: {} for processing", child.getSid());
                    }

                    // Store for deferred dependency creation
                    pendingDependencies.add(dep);
                }
            }

// ✅ After BFS loop ends: create StageXDependencies now that all StageX entries are saved
            for (StageDependency dep : pendingDependencies) {
                StageX childStageX = stageXMap.get(dep.getStage().getSid());
                StageX parentStageX = stageXMap.get(dep.getDependsOn().getSid());

                if (childStageX != null && parentStageX != null) {
                    logger.debug("Creating StageXDependency (deferred): child SXID: {}, dependsOn SXID: {}",
                            childStageX.getSxId(), parentStageX.getSxId());

                    StageXDependency sxDep = StageXDependency.builder()
                            .stageX(childStageX)
                            .dependsOn(parentStageX)
                            .stageOutcome(dep.getStageOutcome())
                            .dependencyType(dep.getDependencyType())
                            .build();
                    stageXDependencyRepository.save(sxDep);

                    logger.debug("Saved StageXDependency for child SID: {} -> parent SID: {}",
                            dep.getStage().getSid(), dep.getDependsOn().getSid());
                } else {
                    logger.warn("Skipped creating StageXDependency due to missing StageX: child SID: {}, parent SID: {}",
                            dep.getStage().getSid(), dep.getDependsOn().getSid());
                }
            }
            logger.debug("Finished processing all stages and dependencies.");

            Set<String> userStageIds = stageXMap.values().stream()
                    .map(stageX -> String.valueOf(stageX.getUserStageId()))
                    .collect(Collectors.toSet());
            return userStageIds;


        } catch (Exception e) {
            logger.error("Error while creating StageX and dependencies", e);
            throw new RuntimeException("StageX creation failed", e); // rollback
        }
    }


    @Async("taskExecutor")
    public void  runPipelineX(Long pxid,String jwtToken) {
        Map<String, Object> response = new HashMap<>();
        try {
            Optional<PipelineX> optionalPipelineX = pipelineXRepository.findById(pxid);
            if (optionalPipelineX.isEmpty()) {
               logger.info( "error");
               logger.info("PipelineX not found for pxid {}" + pxid);
//                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
                return;
            }

            PipelineX pipelineX = optionalPipelineX.get();

            // Deserialize pipeline_progress
            Map<String, Boolean> progressMap;
            try {
                progressMap = new ObjectMapper().readValue(pipelineX.getPipelineProgress(), new TypeReference<>() {});
            } catch (Exception e) {
                response.put("status", "error");
                response.put("message", "Invalid pipelineProgress JSON");
               // return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
                return;
            }
            List<StageX> allStages = stageXRepository.findAllByPipelineX_PxId(pxid);
            List<StageXDependency> allDependencies = stageXDependencyRepository.findAllByStageX_PipelineX_PxId(pxid);

// Map of sxId -> StageX object
            Map<Long, StageX> sxidToStageX = allStages.stream()
                    .collect(Collectors.toMap(StageX::getSxId, s -> s));

// Map of dependsOn.sxId -> List of dependencies that depend on it
//            Map<Long, List<StageXDependency>> dependencyMap = new HashMap<>();
//            for (StageXDependency dep : allDependencies) {
//                Long dependsOnSxId = dep.getDependsOn().getSxId(); // nested access
//                dependencyMap.computeIfAbsent(dependsOnSxId, k -> new ArrayList<>()).add(dep);
//            }

            // Create a map to hold stages and their dependencies (forward map)
            Map<Long, List<StageXDependency>> stageToDependencies = new HashMap<>();
// Create a map to hold stages that depend on other stages (backward map)
            Map<Long, List<StageXDependency>> dependsOnMap = new HashMap<>();

            for (StageXDependency dep : allDependencies) {
                Long sxId = dep.getStageX().getSxId(); // the stage that has dependencies
                Long dependsOnId = dep.getDependsOn().getSxId(); // the input stage that it depends on

                // Add to the forward map: stage -> dependencies
                stageToDependencies.computeIfAbsent(sxId, k -> new ArrayList<>()).add(dep);

                // Add to the backward map: dependsOnId -> stages that depend on it
                dependsOnMap.computeIfAbsent(dependsOnId, k -> new ArrayList<>()).add(dep);
            }

// Log the maps (optional for debugging)
            logger.info("🔁 Stage to Dependencies Map:");
            stageToDependencies.forEach((sxid, deps) -> {
                String depList = deps.stream()
                        .map(d -> String.format("dependsOn=%d, requiredOutcome=%s",
                                d.getDependsOn().getSxId(),
                                d.getStageOutcome()))
                        .collect(Collectors.joining(" | "));
                logger.info("  🔸 Stage {} depends on [{}]", sxid, depList);
            });
            logger.info("🔄 Depends On Map:");
            dependsOnMap.forEach((dependsOnId, dependents) -> {
                String dependentList = dependents.stream()
                        .map(d -> String.format("dependentStage=%d, requiredOutcome=%s",
                                d.getStageX().getSxId(),
                                d.getStageOutcome()))
                        .collect(Collectors.joining(" | "));
                logger.info("  🔹 Stage {} is required by [{}]", dependsOnId, dependentList);
            });



            //  Set<Long> completedStages = ConcurrentHashMap.newKeySet();
            Set<Long> completedStages = ConcurrentHashMap.newKeySet();

            ExecutorService executor = Executors.newCachedThreadPool();
            Map<Long, String> stageOutcomes = new ConcurrentHashMap<>();
          Set<Long> submittedStages = ConcurrentHashMap.newKeySet();
            Set<Long> skippedStages = ConcurrentHashMap.newKeySet();
            AtomicBoolean pipelineFailed = new AtomicBoolean(false);

            pipelineX.setStatus("Running");
            StringBuilder logBuilder = new StringBuilder();

            // Submit initial stages and monitor progress
//            while (completedStages.size() < allStages.size()) {
//                for (StageX stage : allStages) {
//                    if (canRunStage(stage.getSxId(), stageOutcomes, stageToDependencies,dependsOnMap, completedStages)) {
//                        if (submittedStages.add(stage.getSxId())) {
//                            submitStage(stage, jwtToken, pipelineX, progressMap, stageToDependencies, dependsOnMap,
//                                    sxidToStageX, executor, completedStages, stageOutcomes, logBuilder);
//                        }else {
//                            logger.info("⏭️ Skipping stage {} as it's already submitted");
//                        }
//                        }
//                    }
//                }
//
//                // Sleep to avoid tight loop, adjust duration as needed
//                try {
//                    Thread.sleep(500);
//                } catch (InterruptedException e) {
//                    Thread.currentThread().interrupt();
//                }


            while (completedStages.size() + skippedStages.size() < allStages.size()) {
                for (StageX stage : allStages) {
                    Long sxid = stage.getSxId();
                    if (pipelineFailed.get()) {
                        // If pipeline failed, stop further execution
                        logger.error("Pipeline execution failed. Aborting further stages.");
                        pipelineX.setStatus("FAILED"); // Set pipeline status to failed
                        pipelineXRepository.save(pipelineX); // Save pipeline status
                        return; // Exit the method and stop execution
                    }

                    if (completedStages.contains(sxid) || skippedStages.contains(sxid)) {
                        continue; // Already handled
                    }

                    if (canRunStage(sxid, stageOutcomes, stageToDependencies, dependsOnMap, completedStages)) {
                        if (submittedStages.add(sxid)) {
                            submitStage(stage, jwtToken, pipelineX, progressMap, stageToDependencies, dependsOnMap,
                                    sxidToStageX, executor, completedStages, stageOutcomes, logBuilder,submittedStages,pipelineFailed);
                        } else {

                            logger.info("from here at main pipeline run function, ⏭️ Skipping stage {} as it's already submitted", sxid);
                        }
                    } else {
                        // ✅ If all dependencies are completed and none satisfy condition, mark it as skipped
                        List<StageXDependency> deps = stageToDependencies.getOrDefault(sxid, Collections.emptyList());
                        boolean allDepsCompleted = deps.stream()
                                .allMatch(dep -> completedStages.contains(dep.getDependsOn().getSxId()));

                        if (allDepsCompleted) {
                            logger.info("⛔ Stage {} cannot run due to unmet conditions. Marking as skipped.", sxid);
                            skippedStages.add(sxid);
                        }
                    }
                }

                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }

            // Once all stages are completed, shutdown executor
            executor.shutdown();
            if (!executor.awaitTermination(5, TimeUnit.MINUTES)) {
                logger.warn("Executor did not terminate in the specified time");
            }

//
////            while (completedStages.size() < allStages.size()) {
////                for (StageX stage : allStages) {
////                    if (!completedStages.contains(stage.getSxid()) && canRunStage(stage.getSxid(), stageOutcomes, dependencyMap)) {
////                        submitStage(stage, jwtToken, pipelineX, progressMap, dependencyMap, sxidToStageX, executor, completedStages, logBuilder);
////
////                    }
////                }
////                Thread.sleep(500); // avoid tight loop
////            }
//
//            for (StageX stage : allStages) {
//                if (canRunStage(stage.getSxId(), stageOutcomes, dependencyMap, completedStages)) {
//                    submitStage(stage, jwtToken, pipelineX, progressMap, dependencyMap,
//                            sxidToStageX, executor, completedStages, stageOutcomes, logBuilder);
//                }
//            }
//
//
////            for (StageX stage : allStages) {
////                if (canRunStage(stage.getSxId(), stageOutcomes, dependencyMap)) {
////                    submitStage(stage, jwtToken, pipelineX, progressMap, dependencyMap, sxidToStageX, executor, completedStages, logBuilder);
////                }
////            }
//
//
//            executor.shutdown();
//            executor.awaitTermination(5, TimeUnit.MINUTES);
//
////            pipelineX.setLogInfo(logBuilder.toString());
////            pipelineX.setPipelineProgress(new ObjectMapper().writeValueAsString(progressMap));
////            pipelineXRepository.save(pipelineX);
            logger.info("return here from create pipelineX, all execution is done");
            pipelineX.setStatus("Completed");
            pipelineX.setFinishedAt(LocalDateTime.now());
            pipelineXRepository.save(pipelineX);
            pipelineXRepository.flush();
            logger.info("✅ PipelineX finishedAt timestamp saved as: {}", pipelineX.getFinishedAt());
            response.put("status", "success");
            response.put("message", "Pipeline execution completed or triggered");
            response.put("pipelineProgress", progressMap);
            response.put("logInfo", logBuilder.toString());
           // return ResponseEntity.ok(response);
            return;

        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", "Execution failed: " + e.getMessage());
            logger.error("Pipeline execution aborted: {}", e.getMessage());

          //  return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
            return;
        }

    }



private boolean canRunStage(Long sxId,
                            Map<Long, String> stageOutcomes,
                            Map<Long, List<StageXDependency>> dependencyMap,
                            Map<Long, List<StageXDependency>> dependsOnMap,
                            Set<Long> completedStages) {

//    List<StageXDependency> dependencies = dependencyMap.values().stream()
//            .flatMap(List::stream)
//            .filter(dep -> dep.getStageX().getSxId().equals(sxId))
//            .collect(Collectors.toList());

    // Get the list of dependencies that the current stage (sxId) depends on (forward map)
    List<StageXDependency> dependencies = dependencyMap.getOrDefault(sxId, new ArrayList<>());
    if (dependencies.isEmpty()) return true;

    // boolean isConditional = dependencies.get(0).getDependencyType() == DependencyType.CONDITIONAL;
    boolean isConditional = dependencies.stream()
            .allMatch(dep -> dep.getDependencyType() == DependencyType.CONDITIONAL);

    logger.info("🔍 Evaluating canRunStage for sxid={} (isConditional={}) with {} dependencies",
            sxId, isConditional, dependencies.size());

    // boolean anySatisfied = false;
    int total=dependencies.size();
    int fullfilled = 0;

    for (StageXDependency dep : dependencies) {
        Long dependsOnId = dep.getDependsOn().getSxId(); // Stage it depends on
        String requiredOutcome = String.valueOf(dep.getStageOutcome()); // Required outcome
        String actualOutcome = stageOutcomes.get(dependsOnId); // Actual outcome from completed stages


        logger.info("➡️ Dependency: stage {} depends on stage {} with requiredOutcome='{}', actualOutcome='{}'",
                sxId, dependsOnId, requiredOutcome, actualOutcome);

        // ✅ Check if the dependency stage is completed
        if (!completedStages.contains(dependsOnId)) {
            logger.info("⛔ Dependency stage {} not completed yet. Skipping for now.", dependsOnId);
            continue; // ⚠️ DON'T return false here!
        }

        boolean satisfied = actualOutcome != null && actualOutcome.equalsIgnoreCase(requiredOutcome);
        if(satisfied)fullfilled++;

//        if (isConditional && satisfied) {
//            logger.info("✅ Conditional dependency satisfied for stage {}", sxId);
//            return true; // early return only when one is satisfied
//        }
//
//        if (!isConditional && !satisfied) {
//            logger.info("❌ Non-conditional dependency not satisfied for stage {}", sxId);
//            return false;
//        }


    }
    if(isConditional && fullfilled > 0 ) {return true;}
    if(fullfilled==total){return true;}
    return false;
}


public void submitStage(StageX stage,
                        String jwtToken,
                        PipelineX pipelineX,
                        Map<String, Boolean> progressMap,
                        Map<Long, List<StageXDependency>> dependencyMap,
                        Map<Long, List<StageXDependency>> dependsOnMap,
                        Map<Long, StageX> sxidToStageX,
                        ExecutorService executor,
                        Set<Long> completedStages,
                        Map<Long, String> stageOutcomes,
                        StringBuilder logBuilder,
                        Set<Long> submittedStages,
                        AtomicBoolean pipelineFailed) {

    if (completedStages.contains(stage.getSxId())) {
        logger.info("📤Returning because already run stage sxid={}",stage.getSxId());
        return;
    }
    logger.info("📤 Attempting to submit stage sxid={}, dependencies={}", stage.getSxId(), dependencyMap.get(stage.getSxId()));


    executor.submit(() -> {
        try {
            Long sxid = stage.getSxId();
            String userStageId = String.valueOf(stage.getUserStageId());

            String payload = stage.getPayload();
            String actionUrl = stage.getAction().getSourceCode();

            stage.setStatus("Running");
            stageXRepository.save(stage);
            pipelineX.setCurrentStageX(stage);
            pipelineXRepository.save(pipelineX);
            stageXRepository.flush();
            pipelineXRepository.flush();
            logger.info("▶️ Starting stage: sxid={}, userStageId={}, actionUrl={}", sxid, userStageId, actionUrl);
            StringBuilder stageLog = new StringBuilder();
            Map<String, Object> result = executionService.executeStageActionWithResponse(
                    sxid, actionUrl, payload, stageLog
            );

            int responseCode = (int) result.get("responseCode");
            if (responseCode >= 200 && responseCode < 300) {
                String outcome = (String) result.get("outcome");
                stage.setStatus("COMPLETED");
                completedStages.add(sxid);
                stageOutcomes.put(sxid, outcome);
                progressMap.put(userStageId, true);
                logger.info("✅ Stage sxid={} completed with outcome: {}", sxid, outcome);
            } else {
                progressMap.put(userStageId, false);
                stage.setStatus("FAILED");
                logger.error("❌ Stage sxid={} failed with response code {}", sxid, responseCode);
                logger.error("❌ Stage sxid={} failed with response code {}", sxid, responseCode);

                // Set the pipeline failure flag
                pipelineFailed.set(true);
                //throw new RuntimeException("Stage " + sxid + " failed.");
            }

            stageXRepository.save(stage);
//            synchronized (logBuilder) {
//                logBuilder.append(stageLog);
//                pipelineX.setLogInfo(logBuilder.toString());
//                try {
//                    // Save updated pipelineProgress also
//
//                } catch (Exception e) {
//                    e.printStackTrace(); // Optional: handle serialization error properly
//                }
//
//
//
//            }
            String logFilePath = pipelineX.getLogInfo(); // Assuming you have loaded PipelineX

// Append the log to the file
            if (logFilePath != null && !logFilePath.isEmpty()) {
                try (FileWriter fileWriter = new FileWriter(logFilePath, true)) { // true = append mode
                    fileWriter.write(stageLog.toString());
                } catch (IOException e) {
                    logger.error("Failed to write logs to file: {}", logFilePath, e);
                    // Optionally rethrow or handle silently
                }
            }
            pipelineX.setPipelineProgress(new ObjectMapper().writeValueAsString(progressMap));
            pipelineXRepository.save(pipelineX);
            if (!pipelineFailed.get()) {
                checkAndSubmitNextStages(sxid, jwtToken, pipelineX, progressMap, dependencyMap, dependsOnMap,
                        sxidToStageX, executor, completedStages, stageOutcomes, logBuilder, submittedStages, pipelineFailed);
            }
        } catch (Exception e) {
            String errorMsg = "❗ Exception in Stage "  + ": " + e.getMessage();
            logger.error(errorMsg, e);
          //  logBuilder.append("Stage ").append(stage.getSxId()).append(" error: ").append(e.getMessage()).append("\n");
        }
    });
}




    private void checkAndSubmitNextStages(Long completedSxid,
                                          String jwtToken,
                                          PipelineX pipelineX,
                                          Map<String, Boolean> progressMap,
                                          Map<Long, List<StageXDependency>> dependencyMap,
                                          Map<Long, List<StageXDependency>> dependsOnMap,
                                          Map<Long, StageX> sxidToStageX,
                                          ExecutorService executor,
                                          Set<Long> completedStages,
                                          Map<Long, String> stageOutcomes,
                                          StringBuilder logBuilder, Set<Long> submittedStages,
                                          AtomicBoolean pipelineFailed) {

        List<StageXDependency> dependentStages = dependsOnMap.getOrDefault(completedSxid, new ArrayList<>());
        for (StageXDependency dep : dependentStages) {
            StageX dependentStage = dep.getStageX();
            Long nextSxid = dependentStage.getSxId();
            logger.info("🔄 Checking if stage {} can run (triggered by completion of stage {})", nextSxid, completedSxid);

            logger.info("➡️  Evaluating dependent stage sxid={} (depends on sxid={}, requiredOutcome={})",
                    nextSxid, dep.getDependsOn().getSxId(), dep.getStageOutcome());


            if (completedStages.contains(nextSxid)) {
                logger.debug("Stage {} already completed, skipping", nextSxid);
                continue;
            }

            if (canRunStage(nextSxid, stageOutcomes, dependencyMap,dependsOnMap, completedStages)) {
                logger.info("✅ Submitting stage {} as all dependencies are satisfied", nextSxid);
                if(submittedStages.contains(nextSxid)) {continue;}
                submittedStages.add(nextSxid);
                submitStage(dependentStage, jwtToken, pipelineX, progressMap, dependencyMap,dependsOnMap,
                        sxidToStageX, executor, completedStages, stageOutcomes, logBuilder,submittedStages,pipelineFailed);
            } else {
                // If not runnable yet, remove from set so it can be retried later
                logger.info("⛔ Dependencies not satisfied for stage {}, will check again later", nextSxid);
                //completedStages.remove(nextSxid);
            }



        }
    }

}



//    if (isConditional) {
//        logger.info(anySatisfied ? "✅ At least one conditional dependency satisfied for stage {}" :
//                "⛔ No conditional dependencies satisfied yet for stage {}", sxId);
//        return anySatisfied;
//    } else {
//        logger.info("✅ All non-conditional dependencies satisfied for stage {}", sxId);
//        logger.info("✅ need to check further dependencies {}", sxId);
//        return false;
//    }
//}


//    public void submitStage(StageX stage,
//                            String jwtToken,
//                            PipelineX pipelineX,
//                            Map<String, Boolean> progressMap,
//                            Map<Long, List<StageXDependency>> dependencyMap,
//                            Map<Long, StageX> sxidToStageX,
//                            ExecutorService executor,
//                            Set<Long> completedStages,
//                            Map<Long, String> stageOutcomes,
//                            StringBuilder logBuilder) {
//        if (completedStages.contains(stage.getSxId())) {
//            logger.debug("Stage {} already completed, skipping execution", stage.getSxId());
//            return;
//        }
//
//        executor.submit(() -> {
//            try {
//                Long sxid = stage.getSxId();
//                String userStageId = String.valueOf(stage.getUserStageId());
//
//                Action action = stage.getAction();
//                if (action == null || action.getSourceCode() == null) {
//                    logBuilder.append("Action missing for stage ").append(sxid).append("\n");
//                    return;
//                }
//
//                String actionUrl = action.getSourceCode();
//                String payload = "custom-payload"; // Replace with real payload
//
//                stage.setStatus("Running");
//                stageXRepository.save(stage);
//                pipelineX.setCurrentStageX(stage);
//                pipelineXRepository.save(pipelineX);
//                stageXRepository.flush();
//                pipelineXRepository.flush();
////                progressMap.put(userStageId, true);
//                // Synchronously call the action and get outcome + logs
//                StringBuilder stageLog = new StringBuilder();
//                Map<String, Object> result  = executionService.executeStageActionWithResponse(sxid, stage.getStageName(), payload, jwtToken, actionUrl, stageLog);
//
//
//                int responseCode = (int) result.get("responseCode");
//                if (responseCode >= 200 && responseCode < 300) {
//                    logger.debug("Stage {} run successfully", sxid);
//                    String outcome = (String) result.get("outcome");
//                    stage.setStatus("COMPLETED");
//                    logger.info("Stage {} status set to: {}", sxid, stage.getStatus());
//                   completedStages.add(sxid);
//                    stageOutcomes.put(sxid, outcome);
//                    progressMap.put(userStageId, true);
//                    logger.debug("Updated progressMap {} for stage: {}", progressMap, sxid);
//
//                } else {
//                    // Don't mark completed
//                    progressMap.put(userStageId, false);
//                    stage.setStatus("FAILED"); // optional
//                    logBuilder.append("Failed to execute stage ").append(sxid).append(": ").append(result.toString()).append("\n");
//                    throw new RuntimeException("Stage " + sxid + " failed. Aborting pipeline.");
//                }
//
////                // Save status to DB
////                stage.setStatus("COMPLETED");
//                stageXRepository.save(stage);
//
//                // Update state
////                progressMap.put(userStageId, true);
////                completedStages.add(sxid);
////                stageOutcomes.put(sxid, outcome);
//
//                synchronized (logBuilder) {
//                    logBuilder.append(stageLog);
//                    pipelineX.setLogInfo(logBuilder.toString());
//                    pipelineXRepository.save(pipelineX);
//                }
//                stageXRepository.flush();
//                pipelineRepository.flush();
//
//
//                // Check if this completion allows more stages to run
//                checkAndSubmitNextStages(sxid, jwtToken, pipelineX, progressMap, dependencyMap,
//                        sxidToStageX, executor, completedStages, stageOutcomes, logBuilder);
//
//            } catch (Exception e) {
//                logBuilder.append("Error executing stage ").append(stage.getSxId()).append(": ").append(e.getMessage()).append("\n");
//            }
//        });
//    }




//    private boolean canRunStage(Long sxId,

//                                Map<Long, String> stageOutcomes,
//                                Map<Long, List<StageXDependency>> dependencyMap
//                                ) {
//
//        List<StageXDependency> dependencies = dependencyMap.values().stream()
//                .flatMap(List::stream)
//                .filter(dep -> dep.getStageX().getSxId().equals(sxId))
//                .collect(Collectors.toList());
//
//        if (dependencies.isEmpty()) return true;
//
//        boolean isConditional = dependencies.get(0).getDependencyType() == DependencyType.CONDITIONAL;
//
//
//        for (StageXDependency dep : dependencies) {
//            Long dependsOnId = dep.getDependsOn().getSxId();
//            String requiredOutcome = String.valueOf(dep.getStageOutcome()); // "positive" or "negative"
//            String actualOutcome = stageOutcomes.get(dependsOnId);
//
//            boolean satisfied = actualOutcome != null && actualOutcome.equalsIgnoreCase(requiredOutcome);
//
//            if (isConditional && satisfied) return true;
//            if (!isConditional && !satisfied) return false;
//        }
//
//        return !isConditional; // return true only if all satisfied in non-conditional
//    }

//    private boolean canRunStage(Long sxId,
//                                Map<Long, String> stageOutcomes,
//                                Map<Long, List<StageXDependency>> dependencyMap,
//                                Set<Long> completedStages) {
//
//        List<StageXDependency> dependencies = dependencyMap.values().stream()
//                .flatMap(List::stream)
//                .filter(dep -> dep.getStageX().getSxId().equals(sxId))
//                .collect(Collectors.toList());
//
//        if (dependencies.isEmpty()) {
//            logger.info("📦 Stage {} has no dependencies, ready to run", sxId);
//            return true;
//        }
//
//        boolean isConditional = dependencies.get(0).getDependencyType() == DependencyType.CONDITIONAL;
//
//        logger.info("🔍 Evaluating canRunStage for sxid={} (isConditional={}) with {} dependencies",
//                sxId, isConditional, dependencies.size());
//        for (StageXDependency dep : dependencies) {
//            Long dependsOnId = dep.getDependsOn().getSxId();
//            String requiredOutcome = String.valueOf(dep.getStageOutcome());
//            String actualOutcome = stageOutcomes.get(dependsOnId);
//            logger.info("➡️ Dependency: stage {} depends on stage {} with requiredOutcome='{}', actualOutcome='{}'",
//                    sxId, dependsOnId, requiredOutcome, actualOutcome);
//
////            if (completedStages.contains(sxId)) {
////                logger.info("Stage {} already completed, skipping duplicate result", sxId);
////                return false;
////            }
//            // ✅ Check if the stage is completed
//            if (!completedStages.contains(dependsOnId)) {
//                logger.info("⛔ Dependency stage {} not completed yet. Stage {} cannot run.", dependsOnId, sxId);
//                return false;
//            }
//
//            boolean satisfied = actualOutcome != null && actualOutcome.equalsIgnoreCase(requiredOutcome);
//            if (isConditional && satisfied) {
//                logger.info("✅ Conditional dependency satisfied for stage {}", sxId);
//                return true;
//            }
//
//            if (!isConditional && !satisfied) {
//                logger.info("❌ Non-conditional dependency not satisfied for stage {}", sxId);
//                return false;
//            }
//        }
//        logger.info("✅ All non-conditional dependencies satisfied for stage {}", sxId);
//        return !isConditional; // all must be satisfied in non-conditional
//    }
