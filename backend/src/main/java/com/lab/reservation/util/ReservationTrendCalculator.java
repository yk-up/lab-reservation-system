package com.lab.reservation.util;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 将 {@code findDailyTrend} 查询结果填充为连续日期序列，并计算汇总指标（供管理端趋势图与聚合接口复用）。
 */
public final class ReservationTrendCalculator {

    private ReservationTrendCalculator() {
    }

    /**
     * @param startDate 统计区间首日（含）
     * @param safeDays  区间天数（7–30）
     * @param rows      键 {@code stat_date} 为 {@link LocalDate#toString()} 形式的每日聚合行
     */
    public static Map<String, Object> build(LocalDate startDate, int safeDays, List<Map<String, Object>> rows) {
        Map<String, Map<String, Object>> trendMap = new HashMap<>();
        if (rows != null) {
            for (Map<String, Object> row : rows) {
                trendMap.put(String.valueOf(row.get("stat_date")), row);
            }
        }

        List<Map<String, Object>> series = new ArrayList<>();
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
        return result;
    }
}
