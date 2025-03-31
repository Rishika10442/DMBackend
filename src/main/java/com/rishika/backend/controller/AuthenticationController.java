package com.rishika.backend.controller;
import com.rishika.backend.dto.LoginRequest;
import com.rishika.backend.dto.UserRequest;
import com.rishika.backend.entity.User;
import com.rishika.backend.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthenticationController {
    private final UserService userService;

    @GetMapping("/health")
    public String checkHealth() {
        return "Server is up and running!";
    }

    @PostMapping("/signup")
    public ResponseEntity<Map<String, Object>> signup(@Valid @RequestBody UserRequest userRequest) {
        Map<String, Object> response = userService.createUser(userRequest);
        return ResponseEntity.ok(response);
    }


    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestBody @Valid LoginRequest request) {
        return ResponseEntity.ok(userService.login(request));
    }

//    @GetMapping("/check-token")
//    public String checkTokenValidity(@RequestHeader("Authorization") String token, @AuthenticationPrincipal User principal) {
//        if (principal != null) {
//            // Token is valid and user is authenticated
//            return "Token is valid. User: " + principal.getFirstName();
//        } else {
//            // Invalid or expired token
//            return "Invalid or expired token.";
//        }
//    }

}

