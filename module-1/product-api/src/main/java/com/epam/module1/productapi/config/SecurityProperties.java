package com.epam.module1.productapi.config;

import com.epam.module1.productapi.entity.Role;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * @param seedUsers users to create on startup when they do not exist yet
 */
@ConfigurationProperties("app.security")
public record SecurityProperties(@DefaultValue List<SeedUser> seedUsers) {

    public record SeedUser(String username, String password, Role role) {
    }
}
