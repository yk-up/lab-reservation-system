package com.lab.reservation.service;

import com.lab.reservation.entity.Lab;
import com.lab.reservation.entity.TimeSlot;
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

    public List<Lab> listOpenLabs() {
        return labMapper.findAll(1);
    }

    public List<Lab> listAllLabs() {
        return labMapper.findAll(null);
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

    /**
     * 查询某天某实验室的空闲时段（用于日历视图展示）
     * 将当天已有的审核通过/待审核预约从时间轴中剔除，返回剩余空闲区间
     */
    public List<Map<String, Object>> getAvailableSlots(Long labId, LocalDate date) {
        LocalDateTime dayStart = date.atStartOfDay();
        LocalDateTime dayEnd = date.atTime(LocalTime.MAX);

        var booked = reservationMapper.findByLabAndTimeRange(labId, dayStart, dayEnd);

        // 简单示例：以30分钟为粒度枚举该天8:00-22:00所有格子，标记哪些已被占用
        List<Map<String, Object>> slots = new ArrayList<>();
        LocalDateTime cursor = date.atTime(8, 0);
        LocalDateTime endOfDay = date.atTime(22, 0);

        while (cursor.isBefore(endOfDay)) {
            LocalDateTime slotEnd = cursor.plusMinutes(30);
            boolean occupied = false;
            for (var r : booked) {
                // 区间重叠判断
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
