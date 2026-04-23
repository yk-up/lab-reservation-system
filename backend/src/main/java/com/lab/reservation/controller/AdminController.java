package com.lab.reservation.controller;

import com.lab.reservation.entity.Blacklist;
import com.lab.reservation.mapper.BlacklistMapper;
import com.lab.reservation.mapper.LabMapper;
import com.lab.reservation.mapper.ReservationMapper;
import com.lab.reservation.util.UserContext;
import com.lab.reservation.vo.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final BlacklistMapper blacklistMapper;
    private final ReservationMapper reservationMapper;
    private final LabMapper labMapper;

    /** 数据看板 */
    @GetMapping("/dashboard")
    public Result<?> dashboard() {
        if (!UserContext.isAdmin()) return Result.forbidden();
        Map<String, Object> data = new HashMap<>();
        data.put("totalLabs", labMapper.findAll(null).size());
        data.put("openLabs", labMapper.findAll(1).size());
        data.put("pendingCount", reservationMapper.findPendingList().size());
        data.put("blacklistCount", blacklistMapper.findAll().size());
        return Result.success(data);
    }

    /** 实验室使用率统计 */
    @GetMapping("/lab-usage")
    public Result<?> labUsage() {
        if (!UserContext.isAdmin()) return Result.forbidden();
        List<Map<String, Object>> rows = labMapper.findUsageStats();
        long totalReservations = 0L;
        for (Map<String, Object> row : rows) {
            totalReservations += ((Number) row.getOrDefault("reservationCount", 0L)).longValue();
        }

        for (int i = 0; i < rows.size(); i++) {
            Map<String, Object> row = rows.get(i);
            long count = ((Number) row.getOrDefault("reservationCount", 0L)).longValue();
            double usageRate = totalReservations == 0 ? 0D : (count * 100.0 / totalReservations);
            row.put("rank", i + 1);
            row.put("usageRate", Math.round(usageRate * 10) / 10.0);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("totalReservations", totalReservations);
        result.put("ranking", rows);
        return Result.success(result);
    }

    /** 黑名单列表 */
    @GetMapping("/blacklist")
    public Result<?> blacklist() {
        if (!UserContext.isAdmin()) return Result.forbidden();
        return Result.success(blacklistMapper.findAll());
    }

    /** 加入黑名单 */
    @PostMapping("/blacklist")
    public Result<?> addBlacklist(@RequestBody Blacklist blacklist) {
        if (!UserContext.isAdmin()) return Result.forbidden();
        blacklist.setOperatorId(UserContext.getUserId());
        blacklistMapper.insert(blacklist);
        return Result.success("已加入黑名单");
    }

    /** 移出黑名单 */
    @DeleteMapping("/blacklist/{id}")
    public Result<?> removeBlacklist(@PathVariable Long id) {
        if (!UserContext.isAdmin()) return Result.forbidden();
        blacklistMapper.deleteById(id);
        return Result.success("已移出黑名单");
    }
}
