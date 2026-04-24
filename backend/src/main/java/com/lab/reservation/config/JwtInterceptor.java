package com.lab.reservation.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lab.reservation.util.JwtUtil;
import com.lab.reservation.util.UserContext;
import com.lab.reservation.vo.Result;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.method.HandlerMethod;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        boolean isPublicApi = isPublicApi(handler);
        String header = request.getHeader("Authorization");
        // 默认所有接口都要求登录，只有标注 @PublicApi 的接口可匿名访问
        if (!StringUtils.hasText(header) || !header.startsWith("Bearer ")) {
            if (isPublicApi) {
                return true;
            }
            writeUnauthorized(response);
            return false;
        }

        String token = header.substring(7);
        try {
            if (jwtUtil.isTokenExpired(token)) {
                writeUnauthorized(response);
                return false;
            }
            Long userId = jwtUtil.getUserId(token);
            Integer role = jwtUtil.getRole(token);
            log.debug("JWT解析 - userId={}, role={}", userId, role);
            UserContext.setUserId(userId);
            UserContext.setRole(role);
            return true;
        } catch (JwtException e) {
            writeUnauthorized(response);
            return false;
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        UserContext.clear();
    }

    private void writeUnauthorized(HttpServletResponse response) throws Exception {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(Result.unauthorized()));
    }

    private boolean isPublicApi(Object handler) {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return false;
        }
        return handlerMethod.hasMethodAnnotation(PublicApi.class)
                || handlerMethod.getBeanType().isAnnotationPresent(PublicApi.class);
    }
}
