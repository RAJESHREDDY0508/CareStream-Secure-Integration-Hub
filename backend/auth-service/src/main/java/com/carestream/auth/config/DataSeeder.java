package com.carestream.auth.config;

import com.carestream.auth.entity.Role;
import com.carestream.auth.entity.User;
import com.carestream.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Seeds default users on first startup.
 * In production, remove this and provision users via the API.
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class DataSeeder {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Bean
    public CommandLineRunner seedUsers() {
        return args -> {
            createIfAbsent("admin",    "admin@carestream.com",    "Admin@CareStream1!",   Role.ADMIN);
            createIfAbsent("dr.smith", "dr.smith@carestream.com", "Doctor@CareStream1!",  Role.DOCTOR);
            createIfAbsent("svc-user", "svc@carestream.com",      "Service@CareStream1!", Role.SERVICE);
        };
    }

    private void createIfAbsent(String username, String email, String rawPassword, Role role) {
        if (!userRepository.existsByUsername(username)) {
            User user = User.builder()
                    .username(username)
                    .email(email)
                    .password(passwordEncoder.encode(rawPassword))
                    .role(role)
                    .enabled(true)
                    .build();
            userRepository.save(user);
            log.info("[SEEDER] Created default user: {} ({})", username, role);
        }
    }
}
