package com.lab.reservation.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class Blacklist {
    private Long id;
    private Long userId;
    private String username;
    private String realName;
    private String reason;
    private Long operatorId;
    /** 封禁到期时间，null 表示永久 */
    private LocalDateTime expireTime;
    private LocalDateTime createTime;
}
