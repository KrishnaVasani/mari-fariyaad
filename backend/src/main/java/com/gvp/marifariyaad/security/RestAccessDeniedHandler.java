package com.gvp.marifariyaad.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Handles requests from authenticated users who lack the required role
 * (e.g. a normal USER hitting an ADMIN-only endpoint or page).
 */
@Component
public class RestAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                        AccessDeniedException accessDeniedException) throws IOException, ServletException {
        String uri = request.getRequestURI();
        boolean isApiRequest = uri.startsWith("/api/");

        if (isApiRequest) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("success", false);
            body.put("message", "You are not authorized to access this resource.");
            response.getWriter().write(objectMapper.writeValueAsString(body));
        } else {
            response.sendRedirect("/login.html");
        }
    }
}
