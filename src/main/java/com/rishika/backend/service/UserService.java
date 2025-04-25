package com.rishika.backend.service;


import com.rishika.backend.dto.*;
import com.rishika.backend.entity.Pipeline;
import com.rishika.backend.entity.PipelineX;
import com.rishika.backend.entity.User;
import com.rishika.backend.filter.JWTFilter;
import com.rishika.backend.helper.CustomUserDetails;
import com.rishika.backend.helper.JWTHelper;
import com.rishika.backend.mapper.UserMapper;
import com.rishika.backend.repo.PipelineRepo;
import com.rishika.backend.repo.PipelineXRepo;
import com.rishika.backend.repo.UserRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import static java.lang.String.format;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class UserService implements UserDetailsService {
    private final UserRepo repo;
    private final UserMapper mapper;
    private final PipelineRepo pipelineRepository;
    private final PipelineXRepo pipelineXRepository;

    @Autowired
    private final BCryptPasswordEncoder passwordEncoder;
    private static final Logger logger = LoggerFactory.getLogger(JWTFilter.class);
    private final JWTHelper jwtHelper;


    public Map<String, Object> createUser(UserRequest request) {
        try {
            // Check if user already exists
            if (repo.findByEmail(request.email()).isPresent()) {
                return Map.of(
                        "success", false,
                        "message", "User already registered. Please log in."
                );
            }

            // Encrypt the password
            String encryptedPassword = passwordEncoder.encode(request.password());

            // Create and save the user
            User user = mapper.toEntity(request, encryptedPassword);
            repo.save(user);

            // Generate JWT token after signup
            String token = jwtHelper.generateToken(request.email());

            // Fetch pipelines and pipelineX for the new user
            List<Pipeline> pipelines = pipelineRepository.findByUser(user);
            List<PipelineX> pipelineXList = pipelineXRepository.findByUser(user);

            // Safely handle nulls
            List<PipelineSummaryDTO> pipelineSummaries = (pipelines != null) ? pipelines.stream()
                    .sorted(Comparator.comparing(
                            Pipeline::getCreatedAt,
                            Comparator.nullsLast(Comparator.naturalOrder())
                    ).reversed())
                    .map(p -> new PipelineSummaryDTO(p.getPid(), p.getPName()))
                    .toList() : new ArrayList<>();

            List<PipelineXSummaryDTO> pipelineXSummaries = (pipelineXList != null) ? pipelineXList.stream()
                    .sorted(Comparator.comparing(
                            PipelineX::getCreatedAt,
                            Comparator.nullsLast(Comparator.naturalOrder())
                    ).reversed())
                    .map(px -> new PipelineXSummaryDTO(px.getPxId(), px.getName(), px.getStatus()))
                    .toList() : new ArrayList<>();

            UserSummaryDTO userSummary = new UserSummaryDTO(
                    user.getUserId(),
                    user.getFirstName(),
                    user.getLastName(),
                    user.getEmail()
            );

            // Build the response
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("token", token);
            response.put("user", userSummary);  // full user details (later we can replace this with a safe UserDTO if needed)
            response.put("pipelines", pipelineSummaries);
            response.put("pipelinesX", pipelineXSummaries);

            return response;

        } catch (Exception e) {
            // Catch any unexpected error and respond
            return Map.of(
                    "success", false,
                    "message", "Something went wrong while creating user.",
                    "error", e.getMessage()
            );
        }
    }

//    public Map<String, Object> createUser(UserRequest request) {
//        // Check if user already exists
//        if (repo.findByEmail(request.email()).isPresent()) {
//            return Map.of(
//                    "success", false,
//                    "message", "User already registered. Please log in."
//            );
//        }
//
//        // Encrypt the password
//        String encryptedPassword = passwordEncoder.encode(request.password());
//
//        // Create and save the user
//        User user = mapper.toEntity(request, encryptedPassword);
//        repo.save(user);
//
//        // Generate JWT token after signup
//        String token = jwtHelper.generateToken(request.email());
//
//
//        List<Pipeline> pipelines = pipelineRepository.findByUser(user);
//        List<PipelineX> pipelineXList = pipelineXRepository.findByUser(user);
//
//        // Safely handle nulls
//        List<PipelineSummaryDTO> pipelineSummaries = (pipelines != null) ? pipelines.stream()
//                .map(p -> new PipelineSummaryDTO(p.getPid(), p.getPName()))
//                .toList() : new ArrayList<>();
//
//        List<PipelineXSummaryDTO> pipelineXSummaries = (pipelineXList != null) ? pipelineXList.stream()
//                .map(px -> new PipelineXSummaryDTO(px.getPxId(), px.getName(), px.getStatus()))
//                .toList() : new ArrayList<>();
//
//
//        // Build the response
//        Map<String, Object> response = new HashMap<>();
//        response.put("success", true);
//        response.put("user", user);  // full user details
//        response.put("pipelines", pipelineSummaries);
//        response.put("pipelinesX", pipelineXSummaries);
//
//        return response;
//
//        // Return JSON response with success flag and token
////        return Map.of(
////                "success", true,
////                "token", token,
////                "message", "User created and logged in successfully."
////        );
//    }

    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        logger.debug("Loading user details for username: {}", username);
        User user = repo.findByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + username));
        return new CustomUserDetails(user); // Wrapping Customer in CustomUserDetails
    }

    public Map<String, Object> login(LoginRequest request) {
        try {
            // Find user by email
            User user = repo.findByEmail(request.email())
                    .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + request.email()));

            // Check if password matches
            boolean matches = passwordEncoder.matches(request.password(), user.getEncryptedpassword());

            if (!matches) {
                return Map.of(
                        "success", false,
                        "message", "Wrong password or email."
                );
            }

            // Generate JWT token
            String token = jwtHelper.generateToken(request.email());

            // Fetch pipelines and pipelineX for the user
            List<Pipeline> pipelines = pipelineRepository.findByUser(user);
            List<PipelineX> pipelineXList = pipelineXRepository.findByUser(user);

            // Safely handle nulls
            List<PipelineSummaryDTO> pipelineSummaries = (pipelines != null) ? pipelines.stream()
                    .sorted(Comparator.comparing(
                            Pipeline::getCreatedAt,
                            Comparator.nullsLast(Comparator.naturalOrder())
                    ).reversed())
                    .map(p -> new PipelineSummaryDTO(p.getPid(), p.getPName()))
                    .toList() : new ArrayList<>();

            List<PipelineXSummaryDTO> pipelineXSummaries = (pipelineXList != null) ? pipelineXList.stream()
                    .sorted(Comparator.comparing(
                            PipelineX::getCreatedAt,
                            Comparator.nullsLast(Comparator.naturalOrder())
                    ).reversed())                    .map(px -> new PipelineXSummaryDTO(px.getPxId(), px.getName(), px.getStatus()))
                    .toList() : new ArrayList<>();

            UserSummaryDTO userSummary = new UserSummaryDTO(
                    user.getUserId(),
                    user.getFirstName(),
                    user.getLastName(),
                    user.getEmail()
            );

            // Build the response
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("token", token);
            response.put("user", userSummary);  // full user details
            response.put("pipelines", pipelineSummaries);
            response.put("pipelinesX", pipelineXSummaries);

            return response;

        } catch (Exception e) {
            // Catch any unexpected error and respond
            return Map.of(
                    "success", false,
                    "message", "Something went wrong while logging in.",
                    "error", e.getMessage()
            );
        }
    }

}


