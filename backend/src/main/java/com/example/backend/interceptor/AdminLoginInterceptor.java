package com.example.backend.interceptor;

import com.example.backend.utils.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.List;

@Component
@RequiredArgsConstructor
public class AdminLoginInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;

    private static final List<String> ADMIN_ONLY_PATTERNS = List.of(
            "/api/admin/delivery",
            "/api/admin/activities",
            "/api/admin/withdraw"
    );

    private static final List<String> ADMIN_ONLY_ACTIONS = List.of(
            "/audit",
            "/refund/approve",
            "/refund/reject"
    );

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String token = extractToken(request);
        if (token == null || !jwtUtil.validateToken(token)) {
            response.setStatus(401);
            return false;
        }
        String role = jwtUtil.getRole(token);
        if (!"admin".equals(role) && !"operator".equals(role)) {
            response.setStatus(403);
            return false;
        }

        if ("operator".equals(role) && !isAllowedForOperator(request)) {
            response.setStatus(403);
            return false;
        }

        request.setAttribute("userId", jwtUtil.getUserId(token));
        request.setAttribute("role", role);
        return true;
    }

    private boolean isAllowedForOperator(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String method = request.getMethod();

        // Operator cannot create/update/delete on merchants
        if (uri.contains("/api/admin/merchants") && !"GET".equalsIgnoreCase(method)) {
            return false;
        }

        // Operator cannot access delivery, activities, withdraw
        for (String pattern : ADMIN_ONLY_PATTERNS) {
            if (uri.startsWith(pattern)) {
                return false;
            }
        }

        // Operator cannot perform admin-only actions (audit, refund approve/reject)
        for (String action : ADMIN_ONLY_ACTIONS) {
            if (uri.contains(action)) {
                return false;
            }
        }

        return true;
    }

    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }
}
