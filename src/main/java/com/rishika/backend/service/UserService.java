package com.rishika.backend.service;


import com.rishika.backend.dto.LoginRequest;
import com.rishika.backend.dto.UserRequest;
import com.rishika.backend.entity.User;
import com.rishika.backend.filter.JWTFilter;
import com.rishika.backend.helper.CustomUserDetails;
import com.rishika.backend.helper.JWTHelper;
import com.rishika.backend.mapper.UserMapper;
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

import java.util.Map;

@Service
@RequiredArgsConstructor
public class UserService implements UserDetailsService {
    private final UserRepo repo;
    private final UserMapper mapper;
    @Autowired
    private final BCryptPasswordEncoder passwordEncoder;
    private static final Logger logger = LoggerFactory.getLogger(JWTFilter.class);
    private final JWTHelper jwtHelper;

    public Map<String, Object> createUser(UserRequest request) {
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

        // Return JSON response with success flag and token
        return Map.of(
                "success", true,
                "token", token,
                "message", "User created and logged in successfully."
        );
    }

    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        logger.debug("Loading user details for username: {}", username);
        User user = repo.findByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + username));
        return new CustomUserDetails(user); // Wrapping Customer in CustomUserDetails
    }

    public String login(LoginRequest request) {
        User user =repo.findByEmail(request.email())
                .orElseThrow(() -> new UsernameNotFoundException("Employee not found with email: " + request.email()));
        boolean matches = passwordEncoder.matches(request.password(), user.getEncryptedpassword());

        if(!matches){
            return "Wrong Password or Email";
        }
        String token ="";


            token=jwtHelper.generateToken(request.email());
            return token;


        // return token;

    }
}


