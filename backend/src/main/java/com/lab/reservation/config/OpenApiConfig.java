package com.lab.reservation.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI labReservationOpenAPI() {
        final String bearer = "bearerAuth";
        return new OpenAPI()
                .info(new Info()
                        .title("实验室预约系统 API")
                        .version("1.0.0")
                        .description("""
                                认证：除标注公开的接口外，请求头需携带 `Authorization: Bearer <JWT>`。
                                管理端统计：看板 `GET /admin/dashboard`、使用率 `GET /admin/lab-usage`、趋势 `GET /admin/reservation-trend`；
                                数据大屏可优先使用聚合接口 `GET /admin/stats/screen`。
                                审批中心聚合：`GET /admin/stats/approval-center`；公告数据来自 `notice` 表 type=4 系统公告，列表与详情见 `GET /admin/announcements` 与 `GET /admin/announcements/{id}`。
                                """))
                .addSecurityItem(new SecurityRequirement().addList(bearer))
                .components(new Components()
                        .addSecuritySchemes(bearer, new SecurityScheme()
                                .name(bearer)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("登录接口返回的 accessToken")));
    }
}
