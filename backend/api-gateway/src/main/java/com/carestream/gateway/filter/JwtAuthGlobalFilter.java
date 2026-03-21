package com.carestream.gateway.filter;

import com.carestream.gateway.security.JwtService;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.*;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Global JWT authentication filter.
 *
 * Applied to every request BEFORE routing.
 * 1. Skip public paths (login, health, actuator)
 * 2. Extract and validate JWT from Authorization header
 * 3. Inject X-User-Id, X-User-Role headers for downstream services
 * 4. Reject with 401 if token is missing or invalid
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthGlobalFilter implements GlobalFilter, Ordered {

    private static final List<String> PUBLIC_PATHS = List.of(
            "/api/v1/auth/login",
            "/api/v1/auth/refresh",
            "/api/v1/auth/health",
            "/actuator"
    );

    private final JwtService jwtService;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getPath().value();

        if (isPublicPath(path)) {
            return chain.filter(exchange);
        }

        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("[GATEWAY] Missing Authorization header for path={}", path);
            return unauthorized(exchange, "Authorization header missing or malformed");
        }

        String token = authHeader.substring(7);
        if (!jwtService.isValid(token)) {
            log.warn("[GATEWAY] Invalid JWT for path={}", path);
            return unauthorized(exchange, "Invalid or expired token");
        }

        try {
            Claims claims  = jwtService.extractAllClaims(token);
            String userId  = claims.getSubject();
            String role    = claims.get("role", String.class);
            String jti     = claims.getId();

            log.debug("[GATEWAY] Authenticated user={} role={} path={}", userId, role, path);

            // Mutate request — add user context headers for downstream services
            ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                    .header("X-User-Id",   userId)
                    .header("X-User-Role", role)
                    .header("X-Token-Jti", jti)
                    .build();

            return chain.filter(exchange.mutate().request(mutatedRequest).build());

        } catch (Exception e) {
            log.error("[GATEWAY] Error processing JWT: {}", e.getMessage());
            return unauthorized(exchange, "Token processing error");
        }
    }

    @Override
    public int getOrder() {
        return -100;    // Run before all other filters
    }

    private boolean isPublicPath(String path) {
        return PUBLIC_PATHS.stream().anyMatch(path::startsWith);
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        String body = """
                {"timestamp":"%s","status":401,"error":"Unauthorized","message":"%s"}
                """.formatted(java.time.Instant.now(), message).strip();

        org.springframework.core.io.buffer.DataBuffer buffer =
                response.bufferFactory().wrap(body.getBytes());
        return response.writeWith(Mono.just(buffer));
    }
}
