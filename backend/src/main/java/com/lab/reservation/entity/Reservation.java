package com.lab.reservation.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class Reservation {
    private Long id;
    private Long userId;
    private Long labId;
    private String labName;
    private String username;
    private String realName;
    private String title;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer attendees;
    private String remark;
    /**
     * 预约状态：
     * 0-待审核，1-已通过，2-已拒绝，3-已取消，4-已完成
     */
    private Integer status;
    private String rejectReason;
    /** 用于统计用户取消行为 */
    private LocalDateTime cancelTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
