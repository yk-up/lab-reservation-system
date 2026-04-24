package com.lab.reservation.controller;

import com.lab.reservation.entity.Blacklist;
import com.lab.reservation.mapper.BlacklistMapper;
import com.lab.reservation.mapper.LabMapper;
import com.lab.reservation.mapper.ReservationMapper;
import com.lab.reservation.util.UserContext;
import com.lab.reservation.vo.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
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

    /** 预约趋势统计 */
    @GetMapping("/reservation-trend")
    public Result<?> reservationTrend(@RequestParam(defaultValue = "7") Integer days) {
        if (!UserContext.isAdmin()) return Result.forbidden();

        int safeDays = Math.min(Math.max(days, 7), 30);
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(safeDays - 1L);
        LocalDateTime startTime = startDate.atStartOfDay();
        LocalDateTime endTime = endDate.plusDays(1L).atStartOfDay().minusNanos(1);

        List<Map<String, Object>> rows = reservationMapper.findDailyTrend(startTime, endTime);
        Map<String, Map<String, Object>> trendMap = new HashMap<>();
        for (Map<String, Object> row : rows) {
            trendMap.put(String.valueOf(row.get("stat_date")), row);
        }

        List<Map<String, Object>> series = new java.util.ArrayList<>();
        long totalCount = 0L;
        long approvedCount = 0L;
        long rejectedCount = 0L;
        Map<String, Object> peakDay = null;

        for (int i = 0; i < safeDays; i++) {
            LocalDate current = startDate.plusDays(i);
            Map<String, Object> raw = trendMap.get(current.toString());
            long dayTotal = raw == null ? 0L : ((Number) raw.getOrDefault("total_count", 0L)).longValue();
            long dayApproved = raw == null ? 0L : ((Number) raw.getOrDefault("approved_count", 0L)).longValue();
            long dayRejected = raw == null ? 0L : ((Number) raw.getOrDefault("rejected_count", 0L)).longValue();

            Map<String, Object> item = new LinkedHashMap<>();
            item.put("date", current.toString());
            item.put("label", String.format("%02d-%02d", current.getMonthValue(), current.getDayOfMonth()));
            item.put("totalCount", dayTotal);
            item.put("approvedCount", dayApproved);
            item.put("rejectedCount", dayRejected);
            series.add(item);

            totalCount += dayTotal;
            approvedCount += dayApproved;
            rejectedCount += dayRejected;

            if (peakDay == null || dayTotal > ((Number) peakDay.get("totalCount")).longValue()) {
                peakDay = item;
            }
        }

        double passRate = totalCount == 0 ? 0D : (approvedCount * 100.0 / totalCount);
        double avgDaily = safeDays == 0 ? 0D : (totalCount * 1.0 / safeDays);

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("days", safeDays);
        summary.put("totalCount", totalCount);
        summary.put("approvedCount", approvedCount);
        summary.put("rejectedCount", rejectedCount);
        summary.put("passRate", Math.round(passRate * 10) / 10.0);
        summary.put("avgDaily", Math.round(avgDaily * 10) / 10.0);
        summary.put("peakDate", peakDay == null ? null : peakDay.get("date"));
        summary.put("peakLabel", peakDay == null ? null : peakDay.get("label"));
        summary.put("peakCount", peakDay == null ? 0L : peakDay.get("totalCount"));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("series", series);
        result.put("summary", summary);
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
