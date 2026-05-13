package com.lab.reservation.controller;

import com.lab.reservation.config.PublicApi;
import com.lab.reservation.entity.Lab;
import com.lab.reservation.service.LabService;
import com.lab.reservation.util.UserContext;
import com.lab.reservation.vo.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/labs")
@RequiredArgsConstructor
@Tag(name = "实验室", description = "实验室信息、使用率与时段查询")
public class LabController {

    private final LabService labService;

    /**
     * 获取开放实验室列表（不需要登录）。
     * 可选：{@code keyword} 匹配名称或位置；{@code minCapacity}/{@code maxCapacity} 按容量区间筛选。
     */
    @GetMapping
    @PublicApi
    @Operation(summary = "开放实验室列表", description = "支持 keyword（名称/位置）、容量区间筛选；无参数时返回全部开放实验室。")
    public Result<?> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer minCapacity,
            @RequestParam(required = false) Integer maxCapacity) {
        return Result.success(labService.listOpenLabsFiltered(keyword, minCapacity, maxCapacity));
    }

    /** 实验室使用率统计（用户端展示） */
    @GetMapping("/usage")
    @PublicApi
    @Operation(summary = "实验室使用率统计", description = "按预约次数汇总排行，匿名可访问。")
    public Result<?> usage() {
        return Result.success(labService.usageStatsSummary());
    }

    /** 获取指定实验室某天的空闲时段（日历视图） */
    @GetMapping("/{id}/slots")
    @PublicApi
    public Result<?> availableSlots(
            @PathVariable Long id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return Result.success(labService.getAvailableSlots(id, date));
    }

    /** 管理员：获取所有实验室（含停用） */
    @GetMapping("/all")
    public Result<?> listAll() {
        if (!UserContext.isAdmin()) return Result.forbidden();
        return Result.success(labService.listAllLabs());
    }

    /** 管理员：新增实验室 */
    @PostMapping
    public Result<?> add(@RequestBody Lab lab) {
        if (!UserContext.isAdmin()) return Result.forbidden();
        labService.addLab(lab);
        return Result.success("实验室添加成功");
    }

    /** 管理员：编辑实验室信息 */
    @PutMapping("/{id}")
    public Result<?> update(@PathVariable Long id, @RequestBody Lab lab) {
        if (!UserContext.isAdmin()) return Result.forbidden();
        lab.setId(id);
        labService.updateLab(lab);
        return Result.success("更新成功");
    }

    /** 管理员：启用/停用实验室 */
    @PutMapping("/{id}/status")
    public Result<?> toggleStatus(@PathVariable Long id, @RequestParam Integer status) {
        if (!UserContext.isAdmin()) return Result.forbidden();
        labService.toggleStatus(id, status);
        return Result.success("操作成功");
    }
}
