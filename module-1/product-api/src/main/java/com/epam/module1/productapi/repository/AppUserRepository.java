package com.epam.module1.productapi.repository;

import com.epam.module1.productapi.entity.AppUser;
import com.epam.module1.productapi.entity.Role;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {

    Optional<AppUser> findByUsername(String username);

    boolean existsByUsername(String username);

    boolean existsByRole(Role role);
}
