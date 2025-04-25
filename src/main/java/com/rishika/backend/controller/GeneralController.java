package com.rishika.backend.controller;

import com.rishika.backend.filter.JWTFilter;
import com.rishika.backend.service.GeneralService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class GeneralController {

    private final GeneralService generalService;
    private static final Logger logger = LoggerFactory.getLogger(JWTFilter.class);

    @GetMapping("/fetch/pipelineX")
    public Map<String, Object> getPipelineX(@RequestParam Long pxId) {
     //   logger.info("AT getPipelineX controller,{}", pxId);
        return generalService.getPipelineX(pxId);  // You pass pxId to service
    }

    @GetMapping("/fetch/pipeline")
    public Map<String, Object> getPipeline(@RequestParam Long pId) {
        //   logger.info("AT getPipelineX controller,{}", pxId);
        return generalService.getPipeline(pId);  // You pass pxId to service
    }

    @GetMapping("/delete/pipeline")
    public Map<String, Object> deletePipeline(@RequestParam Long pId) {
        //   logger.info("AT getPipelineX controller,{}", pxId);
        return generalService.deletePipeline(pId);  // You pass pxId to service
    }

    @PostMapping("/update-stages")
    public ResponseEntity<Map<String, Object>> updatePipelineStages(@RequestBody Map<String, Object> requestData) {
        Map<String, Object> response = generalService.updatePipelineStages(requestData);
        if ((boolean) response.getOrDefault("success", false)) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }

    @GetMapping("/polling")
    public ResponseEntity<?> pollPipelineX(@RequestParam Long pxId) {
        return generalService.getPollingInfo(pxId);
    }

    @GetMapping("/actions/all")
    public ResponseEntity<Map<String, Object>> getAllActions() {
        return generalService.getAllActions();
    }

}
