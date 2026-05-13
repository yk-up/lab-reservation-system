package com.lab.reservation.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * MVC 拦截器与跨域；由 Spring Boot 自动注册，IDE「未使用」类提示可忽略。
 */
@SuppressWarnings("unused")
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final JwtInterceptor jwtInterceptor;
    private final String allowedOrigins;

    public WebMvcConfig(JwtInterceptor jwtInterceptor,
                        @Value("${cors.allowed-origins}") String allowedOrigins) {
        this.jwtInterceptor = jwtInterceptor;
        this.allowedOrigins = allowedOrigins;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(jwtInterceptor)
                .addPathPatterns("/api/**")
                // 登录、注册接口不需要鉴权
                .excludePathPatterns(
                        "/api/auth/login",
                        "/api/auth/register",
                        // 首页聚合（公开）；实验室开放列表 POST 仍走控制器内 UserContext 校验
                        "/api/home/**",
                        "/api/labs",
                        "/api/labs/usage",
                        "/api/labs/*/slots",
                        "/v3/api-docs/**",
                        "/swagger-ui/**",
                        "/swagger-ui.html"
                );
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns(allowedOrigins.split(","))
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }
}
