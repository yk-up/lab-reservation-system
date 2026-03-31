package com.lab.reservation.service;

import com.lab.reservation.dto.ReservationRequest;
import com.lab.reservation.entity.*;
import com.lab.reservation.mapper.*;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReservationService {

    private final ReservationMapper reservationMapper;
    private final NoticeMapper noticeMapper;
    private final BlacklistMapper blacklistMapper;
    private final UserMapper userMapper;

    @Transactional
    public Reservation create(Long userId, ReservationRequest req) {
        // 1. 黑名单检查
        if (blacklistMapper.findActiveByUserId(userId) != null) {
            throw new IllegalArgumentException("您已被列入黑名单，无法进行预约");
        }

        // 2. 时间合理性校验
        if (!req.getEndTime().isAfter(req.getStartTime())) {
            throw new IllegalArgumentException("结束时间必须晚于开始时间");
        }
        long minutes = java.time.Duration.between(req.getStartTime(), req.getEndTime()).toMinutes();
        if (minutes < 30) {
            throw new IllegalArgumentException("预约时长不能少于30分钟");
        }
        if (minutes > 480) {
            throw new IllegalArgumentException("单次预约不能超过8小时");
        }

        // 3. 冲突检测（核心：利用区间重叠 SQL 查询）
        int conflictCount = reservationMapper.countConflict(
                req.getLabId(), req.getStartTime(), req.getEndTime(), null);
        if (conflictCount > 0) {
            throw new IllegalArgumentException("该时间段已被预约，请选择其他时段");
        }

        // 4. 创建预约
        Reservation reservation = new Reservation();
        reservation.setUserId(userId);
        reservation.setLabId(req.getLabId());
        reservation.setTitle(req.getTitle());
        reservation.setStartTime(req.getStartTime());
        reservation.setEndTime(req.getEndTime());
        reservation.setAttendees(req.getAttendees());
        reservation.setRemark(req.getRemark());
        reservation.setStatus(0);
        reservationMapper.insert(reservation);

        return reservation;
    }

    public List<Reservation> listMyReservations(Long userId) {
        return reservationMapper.findByUserId(userId);
    }

    @Transactional
    public void cancel(Long reservationId, Long userId) {
        Reservation r = reservationMapper.findById(reservationId);
        if (r == null) throw new IllegalArgumentException("预约不存在");
        if (!r.getUserId().equals(userId)) throw new IllegalArgumentException("无权操作此预约");
        if (r.getStatus() == 3) throw new IllegalArgumentException("预约已取消");
        if (r.getStatus() == 4) throw new IllegalArgumentException("预约已完成，无法取消");

        reservationMapper.cancel(reservationId, LocalDateTime.now());
    }

    /** 管理员查看所有待审核预约 */
    public List<Reservation> listPending() {
        return reservationMapper.findPendingList();
    }

    /** 管理员审核预约 */
    @Transactional
    public void audit(Long reservationId, Integer status, String rejectReason) {
        Reservation r = reservationMapper.findById(reservationId);
        if (r == null) throw new IllegalArgumentException("预约不存在");
        if (r.getStatus() != 0) throw new IllegalArgumentException("该预约已审核，请勿重复操作");

        // 通过时再次检查冲突（防止并发场景下两个预约同时通过）
        if (status == 1) {
            int conflict = reservationMapper.countConflict(
                    r.getLabId(), r.getStartTime(), r.getEndTime(), reservationId);
            if (conflict > 0) {
                throw new IllegalArgumentException("该时段已有其他预约通过，请拒绝此申请");
            }
        }

        reservationMapper.updateStatus(reservationId, status, rejectReason);

        // 发送通知
        User user = userMapper.findById(r.getUserId());
        Notice notice = new Notice();
        notice.setUserId(r.getUserId());
        notice.setReservationId(reservationId);

        if (status == 1) {
            notice.setType(1);
            notice.setTitle("预约审核通过");
            notice.setContent(String.format("您的预约【%s】已通过审核，请按时使用实验室。", r.getTitle()));
        } else {
            notice.setType(2);
            notice.setTitle("预约审核未通过");
            notice.setContent(String.format("您的预约【%s】未通过审核。原因：%s",
                    r.getTitle(), rejectReason != null ? rejectReason : "暂无说明"));
        }
        noticeMapper.insert(notice);
    }

    /**
     * 定时任务：每5分钟检查是否有预约即将开始（提前1小时发送站内信提醒）
     */
    @Scheduled(fixedRate = 5 * 60 * 1000)
    public void sendReminderNotices() {
        List<Reservation> reservations = reservationMapper.findNeedReminderReservations();
        for (Reservation r : reservations) {
            Notice notice = new Notice();
            notice.setUserId(r.getUserId());
            notice.setReservationId(r.getId());
            notice.setType(3);
            notice.setTitle("预约即将开始提醒");
            notice.setContent(String.format("您的预约【%s】将在1小时后开始，请提前做好准备。", r.getTitle()));
            noticeMapper.insert(notice);
        }
    }
}
