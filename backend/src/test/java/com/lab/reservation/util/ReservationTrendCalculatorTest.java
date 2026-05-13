package com.lab.reservation.util;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ReservationTrendCalculatorTest {

    @Test
    void shouldFillMissingDaysWithZeros() {
        LocalDate start = LocalDate.of(2026, 5, 1);
        int days = 7;
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("stat_date", "2026-05-03");
        row.put("total_count", 4L);
        row.put("approved_count", 3L);
        row.put("rejected_count", 1L);

        Map<String, Object> trend = ReservationTrendCalculator.build(start, days, List.of(row));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> series = (List<Map<String, Object>>) trend.get("series");
        assertNotNull(series);
        assertEquals(7, series.size());
        Map<String, Object> day3 = series.stream()
                .filter(m -> "2026-05-03".equals(m.get("date")))
                .findFirst()
                .orElseThrow();
        assertEquals(4L, ((Number) day3.get("totalCount")).longValue());
        assertEquals(3L, ((Number) day3.get("approvedCount")).longValue());
        assertEquals(1L, ((Number) day3.get("rejectedCount")).longValue());

        Map<String, Object> day1 = series.get(0);
        assertEquals("2026-05-01", day1.get("date"));
        assertEquals(0L, ((Number) day1.get("totalCount")).longValue());

        @SuppressWarnings("unchecked")
        Map<String, Object> summary = (Map<String, Object>) trend.get("summary");
        assertEquals(7, summary.get("days"));
        assertEquals(4L, ((Number) summary.get("totalCount")).longValue());
        assertEquals(75.0, ((Number) summary.get("passRate")).doubleValue(), 0.001);
        assertEquals("2026-05-03", summary.get("peakDate"));
        assertEquals(4L, ((Number) summary.get("peakCount")).longValue());
    }

    @Test
    void shouldHandleEmptyRows() {
        LocalDate start = LocalDate.of(2026, 5, 10);
        Map<String, Object> trend = ReservationTrendCalculator.build(start, 7, List.of());
        @SuppressWarnings("unchecked")
        Map<String, Object> summary = (Map<String, Object>) trend.get("summary");
        assertEquals(0L, ((Number) summary.get("totalCount")).longValue());
        assertEquals(0.0, ((Number) summary.get("passRate")).doubleValue(), 0.001);
    }
}
