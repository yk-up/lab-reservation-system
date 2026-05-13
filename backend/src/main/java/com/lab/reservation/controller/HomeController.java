package com.lab.reservation.controller;

import com.lab.reservation.config.PublicApi;
import com.lab.reservation.service.LabService;
import com.lab.reservation.vo.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 用户端首页聚合：开放实验室 + 使用率统计，减少首屏请求次数。
 */
@RestController
@RequestMapping("/api/home")
@RequiredArgsConstructor
@Tag(name = "首页聚合", description = "公开接口，聚合实验室列表与使用率")
public class HomeController {

    private final LabService labService;

    @GetMapping("/overview")
    @PublicApi
    @Operation(summary = "首页概览", description = "返回开放实验室列表与使用率统计；筛选参数与 GET /api/labs 一致。")
    public Result<Map<String, Object>> overview(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer minCapacity,
            @RequestParam(required = false) Integer maxCapacity) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("labs", labService.listOpenLabsFiltered(keyword, minCapacity, maxCapacity));
        body.put("usageStats", labService.usageStatsSummary());
        body.put("_meta", Map.of(
                "labsEndpoint", "GET /api/labs",
                "usageEndpoint", "GET /api/labs/usage",
                "userNotices", "登录用户个人通知：GET /api/notices",
                "adminAnnouncements", "管理端系统公告（notice.type=4）：GET /api/admin/announcements"
        ));
        return Result.success(body);
    }
}
