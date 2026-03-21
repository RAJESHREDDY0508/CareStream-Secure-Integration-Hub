package com.carestream.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Auth Service — Phase 1 Skeleton.
 *
 * Phase 1: application starts, DB connection wired, basic health endpoint available.
 * Phase 2: JWT generation, login endpoint, RBAC, token blacklist.
 */
@SpringBootApplication
public class AuthServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(AuthServiceApplication.class, args);
    }
}
