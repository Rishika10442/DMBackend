package com.rishika.backend.controller;


import com.rishika.backend.dto.PipelineRequest;
import com.rishika.backend.service.DesignerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/design")
@RequiredArgsConstructor
public class DesignerController {
    private final DesignerService designerService;


    @PostMapping("/create")
    public ResponseEntity<?> createPipeline(@RequestBody PipelineRequest request) {
        try {
            Map<String, Object> response = designerService.createPipeline(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            // In case the controller itself fails (binding, etc.)
            return ResponseEntity.status(500).body(Map.of(
                    "message", "DesignerController at createPipeline failed to process request",
                    "error", e.getMessage(),
                    "status", "controller_error"
            ));
        }
    }
}
