package com.secure_monitor.security;

import com.secure_monitor.domain.UserEntity;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JwtAuthenticationFilterTest {

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void authenticatesAValidBearerTokenFromTheDatabaseUser() throws Exception {
        JwtService jwtService = new JwtService(new JwtProperties("test-only-jwt-secret-key-32-bytes!!", 3600));
        CustomUserDetailsService userDetailsService = mock(CustomUserDetailsService.class);
        when(userDetailsService.loadUserByUsername("pratik")).thenReturn(
                new MonitorUserDetails(new UserEntity("pratik", "hash", Role.USER, true))
        );
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtService, userDetailsService);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + jwtService.generateToken("pratik", Role.ADMIN));
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, new MockHttpServletResponse(), chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication().getName()).isEqualTo("pratik");
        assertThat(SecurityContextHolder.getContext().getAuthentication().getAuthorities())
                .extracting("authority")
                .containsExactly("ROLE_USER");
    }

    @Test
    void leavesTheRequestAnonymousWhenTheTokenIsExpired() throws Exception {
        JwtService jwtService = new JwtService(new JwtProperties("test-only-jwt-secret-key-32-bytes!!", -60));
        CustomUserDetailsService userDetailsService = mock(CustomUserDetailsService.class);
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtService, userDetailsService);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + jwtService.generateToken("pratik", Role.USER));

        filter.doFilter(request, new MockHttpServletResponse(), mock(FilterChain.class));

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void leavesTheRequestAnonymousWhenTheUserIsDisabled() throws Exception {
        JwtService jwtService = new JwtService(new JwtProperties("test-only-jwt-secret-key-32-bytes!!", 3600));
        CustomUserDetailsService userDetailsService = mock(CustomUserDetailsService.class);
        when(userDetailsService.loadUserByUsername("pratik")).thenReturn(
                new MonitorUserDetails(new UserEntity("pratik", "hash", Role.ADMIN, false))
        );
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtService, userDetailsService);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + jwtService.generateToken("pratik", Role.ADMIN));

        filter.doFilter(request, new MockHttpServletResponse(), mock(FilterChain.class));

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }
}
