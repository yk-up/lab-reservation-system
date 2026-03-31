package com.lab.reservation.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class Notice {
    private Long id;
    private Long userId;
    /** 1-审核通过，2-审核拒绝，3-预约提醒，4-系统公告 */
    private Integer type;
    private String title;
    private String content;
    private Long reservationId;
    /** 0-未读，1-已读 */
    private Integer isRead;
    private LocalDateTime createTime;
}
