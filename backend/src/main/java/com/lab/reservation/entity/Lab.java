package com.lab.reservation.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class Lab {
    private Long id;
    private String name;
    private String location;
    private Integer capacity;
    private String description;
    /** 0-停用，1-开放 */
    private Integer status;
    private Long adminId;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
