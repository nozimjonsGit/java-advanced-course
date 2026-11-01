package com.epam.module1.productapi.config;

import com.epam.module1.productapi.config.SecurityProperties.SeedUser;
import com.epam.module1.productapi.entity.AppUser;
import com.epam.module1.productapi.repository.AppUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Creates the configured users on startup. Existing users are left unchanged.
 */
@Component
public class SeedUsersRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(SeedUsersRunner.class);

    private final SecurityProperties properties;
    private final AppUserRepository repository;
    private final PasswordEncoder passwordEncoder;

    public SeedUsersRunner(
            SecurityProperties properties, AppUserRepository repository, PasswordEncoder passwordEncoder) {
        this.properties = properties;
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        for (SeedUser seedUser : properties.seedUsers()) {
            if (!repository.existsByUsername(seedUser.username())) {
                repository.save(new AppUser(
                        seedUser.username(), passwordEncoder.encode(seedUser.password()), seedUser.role()));
                log.info("Created user '{}' with role {}", seedUser.username(), seedUser.role());
            }
        }
    }
}
