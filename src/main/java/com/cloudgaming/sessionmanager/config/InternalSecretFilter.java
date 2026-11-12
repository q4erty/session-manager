package com.cloudgaming.sessionmanager.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.UriUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@RequiredArgsConstructor
public class InternalSecretFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-Internal-Secret";

    private final InternalSecurityProperties properties;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        if (!resolvePath(request).startsWith("/internal/")) {
            chain.doFilter(request, response);
            return;
        }

        String provided = request.getHeader(HEADER);
        String expected = properties.internalSecret();

        if (provided == null || expected == null || expected.isBlank()
                || !constantTimeEquals(provided, expected)) {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.getWriter().write(
                    "{\"error\":\"INTERNAL_SECRET_INVALID\",\"message\":\"Неверный или отсутствующий внутренний секрет\"}");
            return;
        }

        chain.doFilter(request, response);
    }

    private static String resolvePath(HttpServletRequest request) {
        String uri = request.getRequestURI();
        if (uri == null) {
            return "";
        }
        String contextPath = request.getContextPath();
        if (contextPath != null && !contextPath.isEmpty() && uri.startsWith(contextPath)) {
            uri = uri.substring(contextPath.length());
        }
        return UriUtils.decode(uri, StandardCharsets.UTF_8);
    }

    private static boolean constantTimeEquals(String a, String b) {
        return MessageDigest.isEqual(a.getBytes(StandardCharsets.UTF_8), b.getBytes(StandardCharsets.UTF_8));
    }
}
