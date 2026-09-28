package com.secure_monitor.config;

import jakarta.servlet.ServletException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.filter.CorsFilter;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CorsConfigTest {

    @Test
    void allowsGetFromTheReactDevelopmentOrigin() throws Exception {
        MockHttpServletResponse response = filter("GET", "http://localhost:5173");

        assertThat(response.getStatus()).isEqualTo(HttpStatus.OK.value());
        assertThat(response.getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN))
                .isEqualTo("http://localhost:5173");
    }

    @Test
    void allowsGetWhenViteUsesTheNextPort() throws Exception {
        MockHttpServletResponse response = filter("GET", "http://localhost:5174");

        assertThat(response.getStatus()).isEqualTo(HttpStatus.OK.value());
        assertThat(response.getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN))
                .isEqualTo("http://localhost:5174");
    }

    @Test
    void allowsPreflightFromTheReactDevelopmentOrigin() throws Exception {
        MockHttpServletRequest request = request("OPTIONS", "http://localhost:5173");
        request.addHeader(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter().doFilter(request, response, (req, res) -> {
            throw new AssertionError("preflight should be handled by the CORS filter");
        });

        assertThat(response.getStatus()).isEqualTo(HttpStatus.OK.value());
        assertThat(response.getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN))
                .isEqualTo("http://localhost:5173");
        assertThat(response.getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS)).contains("GET");
    }

    @Test
    void rejectsAnUnknownOrigin() throws Exception {
        MockHttpServletResponse response = filter("GET", "http://evil.example");

        assertThat(response.getStatus()).isEqualTo(HttpStatus.FORBIDDEN.value());
        assertThat(response.getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN)).isNull();
    }

    private static MockHttpServletResponse filter(String method, String origin) throws ServletException, IOException {
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter().doFilter(request(method, origin), response, (req, res) ->
                ((MockHttpServletResponse) res).setStatus(HttpStatus.OK.value()));
        return response;
    }

    private static MockHttpServletRequest request(String method, String origin) {
        MockHttpServletRequest request = new MockHttpServletRequest(method, "/api/alerts");
        request.addHeader(HttpHeaders.ORIGIN, origin);
        return request;
    }

    private static CorsFilter filter() {
        return new CorsConfig()
                .apiCorsFilter(new CorsProperties(List.of("http://localhost:*", "http://127.0.0.1:*")))
                .getFilter();
    }
}
