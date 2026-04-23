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
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
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
        data.put("pendingCount", reservationMapper.countPending());
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

    /** 预约趋势分析（支持按天/按每7天分组） */
    @GetMapping("/reservation-trend")
    public Result<?> reservationTrend(
            @RequestParam(defaultValue = "7") Integer days,
            @RequestParam(defaultValue = "weekly") String groupBy) {
        if (!UserContext.isAdmin()) return Result.forbidden();
        int safeDays = (days == null || days < 1) ? 7 : Math.min(days, 90);
        boolean dailyMode = "daily".equalsIgnoreCase(groupBy);

        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(safeDays - 1L);
        LocalDateTime startTime = startDate.atStartOfDay();
        LocalDateTime endTime = endDate.atTime(LocalTime.MAX);

        List<Map<String, Object>> raw = reservationMapper.findDailyTrend(startTime, endTime);
        Map<String, Map<String, Object>> byDate = new HashMap<>();
        for (Map<String, Object> row : raw) {
            byDate.put(String.valueOf(row.get("stat_date")), row);
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM-dd");
        List<Map<String, Object>> points = new ArrayList<>();
        long total = 0L;
        long approvedTotal = 0L;
        long rejectedTotal = 0L;
        long peakCount = 0L;
        String peakPeriod = null;
        final int bucketSize = dailyMode ? 1 : 7;

        for (int offset = 0; offset < safeDays; offset += bucketSize) {
            LocalDate bucketStart = startDate.plusDays(offset);
            LocalDate bucketEnd = startDate.plusDays(Math.min(offset + bucketSize - 1L, safeDays - 1L));
            long totalCount = 0L;
            long approvedCount = 0L;
            long rejectedCount = 0L;

            for (LocalDate d = bucketStart; !d.isAfter(bucketEnd); d = d.plusDays(1)) {
                Map<String, Object> row = byDate.get(d.toString());
                totalCount += row == null ? 0L : ((Number) row.getOrDefault("total_count", 0)).longValue();
                approvedCount += row == null ? 0L : ((Number) row.getOrDefault("approved_count", 0)).longValue();
                rejectedCount += row == null ? 0L : ((Number) row.getOrDefault("rejected_count", 0)).longValue();
            }

            Map<String, Object> point = new HashMap<>();
            point.put("date", bucketStart.toString());
            point.put("startDate", bucketStart.toString());
            point.put("endDate", bucketEnd.toString());
            point.put("label", dailyMode
                    ? bucketStart.format(formatter)
                    : bucketStart.format(formatter) + "~" + bucketEnd.format(formatter));
            point.put("totalCount", totalCount);
            point.put("approvedCount", approvedCount);
            point.put("rejectedCount", rejectedCount);
            points.add(point);

            total += totalCount;
            approvedTotal += approvedCount;
            rejectedTotal += rejectedCount;
            if (totalCount >= peakCount) {
                peakCount = totalCount;
                peakPeriod = bucketStart.toString() + " ~ " + bucketEnd.toString();
            }
        }

        long previous = points.size() > 1
                ? ((Number) points.get(points.size() - 2).getOrDefault("totalCount", 0)).longValue()
                : 0L;
        long latest = points.isEmpty()
                ? 0L
                : ((Number) points.get(points.size() - 1).getOrDefault("totalCount", 0)).longValue();
        double weekOverWeek = previous == 0 ? (latest > 0 ? 100D : 0D) : ((latest - previous) * 100.0 / previous);

        Map<String, Object> result = new HashMap<>();
        result.put("days", safeDays);
        result.put("bucketSize", bucketSize);
        result.put("groupBy", dailyMode ? "daily" : "weekly");
        result.put("totalReservations", total);
        result.put("approvedReservations", approvedTotal);
        result.put("rejectedReservations", rejectedTotal);
        result.put("peakDate", peakPeriod);
        result.put("peakPeriod", peakPeriod);
        result.put("peakCount", peakCount);
        result.put("dayOverDay", Math.round(weekOverWeek * 10) / 10.0);
        result.put("weekOverWeek", Math.round(weekOverWeek * 10) / 10.0);
        result.put("points", points);
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
