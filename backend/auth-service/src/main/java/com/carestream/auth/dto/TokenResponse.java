package com.carestream.auth.dto;

public record TokenResponse(
        String accessToken,
        String refreshToken,
        long expiresIn,
        String tokenType,
        String role,
        String username
) {
    public static TokenResponse of(String accessToken, String refreshToken, long expiresIn, String role, String username) {
        return new TokenResponse(accessToken, refreshToken, expiresIn, "Bearer", role, username);
    }
}
