package com.lab.reservation.service;

import com.lab.reservation.entity.Lab;
import com.lab.reservation.mapper.LabMapper;
import com.lab.reservation.mapper.ReservationMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class LabService {

    private final LabMapper labMapper;
    private final ReservationMapper reservationMapper;

    private record UsageStats(long totalReservations, List<Map<String, Object>> rows) {}

    public List<Lab> listOpenLabs() {
        return labMapper.findAll(1);
    }

    /**
     * 开放实验室列表；无筛选条件时等价于 {@link #listOpenLabs()}。
     */
    public List<Lab> listOpenLabsFiltered(String keyword, Integer minCapacity, Integer maxCapacity) {
        boolean hasKeyword = keyword != null && !keyword.trim().isEmpty();
        boolean hasCap = minCapacity != null || maxCapacity != null;
        if (!hasKeyword && !hasCap) {
            return listOpenLabs();
        }
        return labMapper.findOpenFiltered(
                hasKeyword ? keyword.trim() : null,
                minCapacity,
                maxCapacity);
    }

    public List<Lab> listAllLabs() {
        return labMapper.findAll(null);
    }

    public List<Map<String, Object>> usageStats() {
        return computeUsageStats().rows();
    }

    public Map<String, Object> usageStatsSummary() {
        UsageStats stats = computeUsageStats();
        Map<String, Object> result = new HashMap<>();
        result.put("totalReservations", stats.totalReservations());
        result.put("ranking", stats.rows());
        return result;
    }

    private UsageStats computeUsageStats() {
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
        return new UsageStats(totalReservations, rows);
    }

    public Lab getById(Long id) {
        Lab lab = labMapper.findById(id);
        if (lab == null) throw new IllegalArgumentException("实验室不存在");
        return lab;
    }

    public void addLab(Lab lab) {
        lab.setStatus(1);
        labMapper.insert(lab);
    }

    public void updateLab(Lab lab) {
        labMapper.update(lab);
    }

    public void toggleStatus(Long id, Integer status) {
        labMapper.updateStatus(id, status);
    }

    public List<Map<String, Object>> getAvailableSlots(Long labId, LocalDate date) {
        LocalDateTime dayStart = date.atStartOfDay();
        LocalDateTime dayEnd = date.atTime(LocalTime.MAX);

        var booked = reservationMapper.findByLabAndTimeRange(labId, dayStart, dayEnd);

        List<Map<String, Object>> slots = new ArrayList<>();
        LocalDateTime cursor = date.atTime(8, 0);
        LocalDateTime endOfDay = date.atTime(22, 0);

        while (cursor.isBefore(endOfDay)) {
            LocalDateTime slotEnd = cursor.plusMinutes(30);
            boolean occupied = false;
            for (var r : booked) {
                if (r.getStartTime().isBefore(slotEnd) && r.getEndTime().isAfter(cursor)) {
                    occupied = true;
                    break;
                }
            }
            Map<String, Object> slot = new HashMap<>();
            slot.put("start", cursor.toString());
            slot.put("end", slotEnd.toString());
            slot.put("occupied", occupied);
            slots.add(slot);
            cursor = slotEnd;
        }
        return slots;
    }
}
