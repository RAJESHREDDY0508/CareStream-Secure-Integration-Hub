package com.carestream.auth.service;

import com.carestream.auth.dto.LoginRequest;
import com.carestream.auth.dto.TokenResponse;
import com.carestream.auth.entity.RefreshToken;
import com.carestream.auth.entity.TokenBlacklist;
import com.carestream.auth.entity.User;
import com.carestream.auth.repository.RefreshTokenRepository;
import com.carestream.auth.repository.TokenBlacklistRepository;
import com.carestream.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final TokenBlacklistRepository tokenBlacklistRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${jwt.expiration-ms:900000}")
    private long accessTokenExpirationMs;

    @Value("${jwt.refresh-expiration-ms:604800000}")
    private long refreshTokenExpirationMs;

    @Transactional
    public TokenResponse login(LoginRequest request) {
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password())
        );

        User user = userRepository.findByUsername(request.username())
                .orElseThrow(() -> new BadCredentialsException("User not found"));

        String accessToken  = jwtService.generateAccessToken(user.getUsername(), user.getRole().name());
        String refreshToken = jwtService.generateRefreshToken();

        // Persist hashed refresh token
        RefreshToken rt = RefreshToken.builder()
                .user(user)
                .tokenHash(hash(refreshToken))
                .expiresAt(Instant.now().plusMillis(refreshTokenExpirationMs))
                .build();
        refreshTokenRepository.save(rt);

        log.info("[AUTH] Login success user={} role={}", user.getUsername(), user.getRole());
        return TokenResponse.of(accessToken, refreshToken, accessTokenExpirationMs / 1000,
                user.getRole().name(), user.getUsername());
    }

    @Transactional
    public TokenResponse refresh(String rawRefreshToken) {
        String tokenHash = hash(rawRefreshToken);
        RefreshToken rt = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new BadCredentialsException("Invalid refresh token"));

        if (rt.isRevoked() || rt.isExpired()) {
            throw new BadCredentialsException("Refresh token expired or revoked");
        }

        User user = rt.getUser();
        String newAccessToken = jwtService.generateAccessToken(user.getUsername(), user.getRole().name());

        log.info("[AUTH] Token refreshed user={}", user.getUsername());
        return TokenResponse.of(newAccessToken, rawRefreshToken, accessTokenExpirationMs / 1000,
                user.getRole().name(), user.getUsername());
    }

    @Transactional
    public void logout(String bearerToken) {
        if (bearerToken == null || !bearerToken.startsWith("Bearer ")) return;
        String token = bearerToken.substring(7);

        try {
            String jti       = jwtService.extractJti(token);
            Instant expiresAt = jwtService.extractExpiration(token).toInstant();

            TokenBlacklist blacklisted = TokenBlacklist.builder()
                    .jti(jti)
                    .expiresAt(expiresAt)
                    .build();
            tokenBlacklistRepository.save(blacklisted);

            String username = jwtService.extractUsername(token);
            userRepository.findByUsername(username).ifPresent(u ->
                    refreshTokenRepository.revokeAllByUserId(u.getId())
            );
            log.info("[AUTH] Logout — jti={} blacklisted", jti);
        } catch (Exception e) {
            log.warn("[AUTH] Logout with invalid token: {}", e.getMessage());
        }
    }

    public boolean isTokenBlacklisted(String jti) {
        return tokenBlacklistRepository.existsByJti(jti);
    }

    private String hash(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }
}
