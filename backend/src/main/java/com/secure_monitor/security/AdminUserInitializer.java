package com.secure_monitor.security;

import com.secure_monitor.domain.UserEntity;
import com.secure_monitor.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class AdminUserInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminUserInitializer.class);

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final BootstrapProperties properties;

    public AdminUserInitializer(UserRepository users, PasswordEncoder passwordEncoder, BootstrapProperties properties) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.properties = properties;
    }

    @Override
    public void run(ApplicationArguments args) {
        String username = properties.username();
        String password = properties.password();
        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            log.info("Bootstrap admin password is not set; skipping initial admin user");
            return;
        }
        if (users.existsByUsername(username)) {
            return;
        }
        users.save(new UserEntity(username, passwordEncoder.encode(password), Role.ADMIN, true));
        log.info("Created bootstrap admin user {}", username);
    }
}
