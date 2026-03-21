package com.carestream.auth.dto;

import jakarta.validation.constraints.*;

public record CreateUserRequest(
        @NotBlank @Size(min = 3, max = 100) String username,
        @NotBlank @Email String email,
        @NotBlank @Size(min = 12, message = "Password must be at least 12 characters") String password,
        @NotBlank @Pattern(regexp = "ADMIN|DOCTOR|SERVICE", message = "Role must be ADMIN, DOCTOR, or SERVICE") String role
) {}
