package com.rishika.backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.*;
public record UserRequest (

        @JsonProperty("first_name")
        @NotBlank(message = "First name is required")
        @Size(max = 50, message = "First name must not exceed 50 characters")
        String firstName,

        @JsonProperty("last_name")
        @NotBlank(message = "Last name is required")
        @Size(max = 50, message = "Last name must not exceed 50 characters")
        String lastName,

        @JsonProperty("email")
        @NotBlank(message = "Email is required")
        @Email(message = "Email must be valid")
        @Size(max = 50, message = "Email must not exceed 50 characters")
        String email,


        @JsonProperty("password")
        @NotBlank(message = "Password is required")
        @Size(max = 100, message = "Password must not exceed 100 characters")
        String password,

        @JsonProperty("user_info")
        String userInfo
){
}
