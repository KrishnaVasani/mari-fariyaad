package com.gvp.marifariyaad.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Handles unauthenticated access to protected resources.
 * - REST API calls (/api/**) receive a 401 JSON response.
 * - Regular page requests are redirected to the login page.
 */
@Component
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                          AuthenticationException authException) throws IOException, ServletException {
        String uri = request.getRequestURI();
        boolean isApiRequest = uri.startsWith("/api/");
        boolean wantsJson = "XMLHttpRequest".equalsIgnoreCase(request.getHeader("X-Requested-With"))
                || (request.getHeader("Accept") != null && request.getHeader("Accept").contains(MediaType.APPLICATION_JSON_VALUE));

        if (isApiRequest || wantsJson) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("success", false);
            body.put("message", "Login required. Please sign in to continue.");
            response.getWriter().write(objectMapper.writeValueAsString(body));
        } else {
            response.sendRedirect("/login.html");
        }
    }
}
