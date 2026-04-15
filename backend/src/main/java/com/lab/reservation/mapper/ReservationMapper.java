package com.lab.reservation.mapper;

import com.lab.reservation.entity.Reservation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface ReservationMapper {

    int insert(Reservation reservation);

    Reservation findById(@Param("id") Long id);

    List<Reservation> findByUserId(@Param("userId") Long userId);

    List<Reservation> findPendingList();

    List<Reservation> findByLabAndTimeRange(
            @Param("labId") Long labId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );

    /**
     * 预约冲突检测：查询指定实验室在时间段内是否存在已通过/待审核的预约
     * 利用区间重叠判断：a.start < b.end AND a.end > b.start
     */
    int countConflict(
            @Param("labId") Long labId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            @Param("excludeId") Long excludeId
    );

    int updateStatus(
            @Param("id") Long id,
            @Param("status") Integer status,
            @Param("rejectReason") String rejectReason
    );

    int cancel(@Param("id") Long id, @Param("cancelTime") LocalDateTime cancelTime);

    /** 查询需要发送提醒的预约（开始前1小时，且尚未提醒） */
    List<Reservation> findNeedReminderReservations();
}
