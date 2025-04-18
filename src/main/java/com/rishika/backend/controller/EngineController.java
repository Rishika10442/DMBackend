package com.rishika.backend.controller;


import com.rishika.backend.filter.JWTFilter;
import com.rishika.backend.service.EngineService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/execute")
@RequiredArgsConstructor
public class EngineController {


    private final EngineService engineService;
    private static final Logger logger = LoggerFactory.getLogger(JWTFilter.class);

    @PostMapping("/create/pipelineX")
    public ResponseEntity<Map<String, Object>> createPipelineX(@RequestBody Map<String, String> request, HttpServletRequest httpRequest) {
        try {
            logger.debug("Entered controller createPipelineX()");

            String pIDParam = request.get("pid");

            if (pIDParam == null || pIDParam.trim().isEmpty()) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("status", "error");
                errorResponse.put("message", "Missing or empty pid in request");
                return ResponseEntity.badRequest().body(errorResponse);
            }

            // Extract JWT token from Authorization header
            String authHeader = httpRequest.getHeader("Authorization");
            String jwtToken = null;

            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                jwtToken = authHeader.substring(7); // Remove the "Bearer " prefix
            }

            if (jwtToken == null) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("status", "error");
                errorResponse.put("message", "Missing or invalid Authorization token");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
            }

            // Delegate to service and expect a ResponseEntity
            return engineService.createPipelineX(pIDParam,jwtToken);

        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", "EngineController failed at createPipelineX. Exception occurred: " + e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }


    @PostMapping("/run/pipelineX")
    public ResponseEntity<Map<String, Object>> runPipelineX(@RequestBody Map<String, String> request,HttpServletRequest httpRequest) {
        try {
            Long pxid = Long.parseLong(request.get("pxid").toString());
            logger.debug("Entered controller RunPipelineX() ,{}", pxid);

            Map<String, Object> response = new HashMap<>();

            // Extract JWT token from Authorization header
            String authHeader = httpRequest.getHeader("Authorization");
            String jwtToken = null;

            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                jwtToken = authHeader.substring(7); // Remove the "Bearer " prefix
            }

            if (jwtToken == null) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("status", "error");
                errorResponse.put("message", "Missing or invalid Authorization token");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
            }

//            response.put("status", "success");
//            response.put("message", "EngineController running a" +
//                    " t pipelineX run");
            // ✅ Trigger pipeline asynchronously
            engineService.runPipelineX(pxid, jwtToken);

            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "PipelineX execution triggered in background"
            ));

        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", "EngineController failed at createPipelineX. Exception occurred: " + e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
}
