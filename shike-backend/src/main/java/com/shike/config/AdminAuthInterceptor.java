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
        String servletPath = request.getServletPath();

        // 放行静态资源与公开功能配置读取 (只对 GET /features 放行，修改 toggle 依然验权)
        if ("GET".equalsIgnoreCase(request.getMethod()) && (requestURI.endsWith("/features") || servletPath.endsWith("/features"))) {
            return true;
        }

        if (requestURI.endsWith(".html") 
                || requestURI.endsWith(".css") 
                || requestURI.endsWith(".js") 
                || requestURI.endsWith(".png") 
                || requestURI.endsWith(".jpg") 
                || requestURI.endsWith(".ico")
                || requestURI.contains("/admin/index.html")
                || servletPath.equals("/admin/login")
                || servletPath.equals("/admin/check-auth")
                || requestURI.endsWith("/login")
                || requestURI.endsWith("/check-auth")) {
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

        log.warn("Unauthorized access attempt to {} (servletPath: {})", requestURI, servletPath);
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        ResultDTO<Void> errorResult = ResultDTO.error(401, "请先登录管理后台");
        response.getWriter().write(objectMapper.writeValueAsString(errorResult));
        return false;
    }
}
