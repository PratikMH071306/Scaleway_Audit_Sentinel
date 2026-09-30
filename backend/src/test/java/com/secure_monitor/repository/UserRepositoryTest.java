package com.secure_monitor.repository;

import com.secure_monitor.domain.UserEntity;
import com.secure_monitor.security.Role;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

class UserRepositoryTest extends AbstractPostgresRepositoryTest {

    @Autowired
    private UserRepository repository;

    @Test
    void findsAUserByUniqueUsername() {
        repository.saveAndFlush(new UserEntity("pratik", "hash", Role.ADMIN, true));

        assertThat(repository.existsByUsername("pratik")).isTrue();
        UserEntity stored = repository.findByUsername("pratik").orElseThrow();
        assertThat(stored.getRole()).isEqualTo(Role.ADMIN);
        assertThat(stored.isEnabled()).isTrue();
        assertThat(stored.getCreatedAt()).isNotNull();
    }
}
