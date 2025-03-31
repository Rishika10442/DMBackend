package com.rishika.backend.filter;
import com.rishika.backend.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import com.rishika.backend.helper.JWTHelper;
import com.rishika.backend.helper.CustomUserDetails;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@Component
public class JWTFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(JWTFilter.class);

    @Autowired
    private JWTHelper jwtHelper;

    @Autowired
    private UserService userService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");
        final String jwt;
        final String username;

        // Log the Authorization header
        logger.debug("Authorization header: {}", authHeader);

        // Check for JWT in Authorization header
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            logger.debug("No JWT token found in request, continuing with filter chain");
            filterChain.doFilter(request, response);  // Continue with filter chain if no JWT token
            return;
        }

        jwt = authHeader.substring(7); // Remove "Bearer " prefix
        username = jwtHelper.extractUsername(jwt);

        // Log the extracted username from the JWT
        logger.debug("Extracted username from JWT: {}", username);

        // If username is found and SecurityContext is not already set
        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                var userDetails = userService.loadUserByUsername(username);  // Load user details
                logger.debug("User details loaded for username: {}", username);

                if (jwtHelper.validateToken(jwt, userDetails.getUsername())) {
                    logger.debug("JWT is valid for user: {}", username);

                    // Create authentication token
                    UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    // Set the authentication in SecurityContext
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                    logger.debug("Authentication token set for user: {}", username);
                } else {
                    logger.warn("JWT validation failed for user: {}", username);
                }
            } catch (Exception e) {
                logger.error("Error loading user details for username: {}", username, e);
            }
        }

        // Continue with the filter chain
        filterChain.doFilter(request, response);
    }
}