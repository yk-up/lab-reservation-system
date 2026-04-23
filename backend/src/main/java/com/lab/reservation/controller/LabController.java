package com.lab.reservation.controller;

import com.lab.reservation.entity.Lab;
import com.lab.reservation.service.LabService;
import com.lab.reservation.util.UserContext;
import com.lab.reservation.vo.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/labs")
@RequiredArgsConstructor
public class LabController {

    private final LabService labService;

    /** 获取所有开放实验室（不需要登录） */
    @GetMapping
    public Result<?> list() {
        return Result.success(labService.listOpenLabs());
    }

    /** 实验室使用率统计（用户端展示） */
    @GetMapping("/usage")
    public Result<?> usage() {
        List<Map<String, Object>> rows = labService.usageStats();
        long totalReservations = 0L;
        for (Map<String, Object> row : rows) {
            totalReservations += ((Number) row.getOrDefault("reservationCount", 0L)).longValue();
        }
        Map<String, Object> result = new HashMap<>();
        result.put("totalReservations", totalReservations);
        result.put("ranking", rows);
        return Result.success(result);
    }

    /** 获取指定实验室某天的空闲时段（日历视图） */
    @GetMapping("/{id}/slots")
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
