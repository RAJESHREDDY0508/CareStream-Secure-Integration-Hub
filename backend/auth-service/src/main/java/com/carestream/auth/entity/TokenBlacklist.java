package com.carestream.auth.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * Revoked access tokens (before their natural expiry).
 * Indexed by jti (JWT ID claim).
 * Expired entries can be purged by a scheduler.
 */
@Entity
@Table(name = "token_blacklist",
       indexes = {
           @Index(name = "idx_blacklist_jti",     columnList = "jti"),
           @Index(name = "idx_blacklist_expires",  columnList = "expires_at")
       })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TokenBlacklist {

    @Id
    @Column(name = "jti", nullable = false, length = 255)
    private String jti;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "revoked_at", updatable = false)
    private Instant revokedAt;

    @PrePersist
    void prePersist() {
        revokedAt = Instant.now();
    }
}
