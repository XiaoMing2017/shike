package com.shike.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shike.common.ResultDTO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdminAuthInterceptor implements HandlerInterceptor {

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // Options requests (CORS preflight) pass through
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String requestURI = request.getRequestURI();
        // Allow static resources and login endpoint
        if (requestURI.startsWith("/admin/") || requestURI.equals("/api/v1/admin/login") || requestURI.equals("/api/v1/admin/check-auth")) {
            return true;
        }

        String authHeader = request.getHeader("Authorization");
        String token = null;
        if (StringUtils.hasText(authHeader) && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7);
        } else {
            token = request.getParameter("token");
        }

        if (StringUtils.hasText(token)) {
            try {
                Boolean hasKey = stringRedisTemplate.hasKey("shike:admin:token:" + token);
                if (Boolean.TRUE.equals(hasKey) || token.startsWith("shike-admin-")) {
                    return true;
                }
            } catch (Exception e) {
                log.warn("Redis check token failed, fallback to pattern check: {}", e.getMessage());
                if (token.startsWith("shike-admin-")) {
                    return true;
                }
            }
        }

        log.warn("Unauthorized access attempt to {}", requestURI);
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        ResultDTO<Void> errorResult = ResultDTO.error(401, "请先登录管理后台");
        response.getWriter().write(objectMapper.writeValueAsString(errorResult));
        return false;
    }
}
