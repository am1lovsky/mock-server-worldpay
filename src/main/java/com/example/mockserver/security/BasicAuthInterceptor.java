package com.example.mockserver.security;

import com.example.mockserver.dto.response.ErrorResponse;
import tools.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Enforces the documented HTTP Basic auth on {@code /accountVerifications}.
 * Implemented as a plain interceptor (rather than pulling in Spring Security)
 * so the 401 body can match the documented {@code {errorName, message}} shape exactly.
 */
@Component
public class BasicAuthInterceptor implements HandlerInterceptor {

    private final String expectedUsername;
    private final String expectedPassword;
    private final ObjectMapper objectMapper;

    public BasicAuthInterceptor(
            @Value("${mock.security.username}") String expectedUsername,
            @Value("${mock.security.password}") String expectedPassword,
            ObjectMapper objectMapper) {
        this.expectedUsername = expectedUsername;
        this.expectedPassword = expectedPassword;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull Object handler) throws Exception {
        String header = request.getHeader("Authorization");
        if (!StringUtils.hasText(header) || !header.startsWith("Basic ")) {
            reject(response, "unauthorized", "An Authorization header with HTTP Basic credentials is required");
            return false;
        }

        String[] credentials = decode(header.substring("Basic ".length()));
        if (credentials == null || !expectedUsername.equals(credentials[0]) || !expectedPassword.equals(credentials[1])) {
            reject(response, "invalidCredentials", "The supplied credentials are invalid");
            return false;
        }

        return true;
    }

    private String[] decode(String base64Credentials) {
        try {
            String decoded = new String(Base64.getDecoder().decode(base64Credentials), StandardCharsets.UTF_8);
            int separator = decoded.indexOf(':');
            if (separator < 0) {
                return null;
            }
            return new String[]{decoded.substring(0, separator), decoded.substring(separator + 1)};
        } catch (IllegalArgumentException malformedBase64) {
            return null;
        }
    }

    private void reject(HttpServletResponse response, String errorName, String message) throws java.io.IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(objectMapper.writeValueAsString(new ErrorResponse(errorName, message)));
    }
}
