package com.lab.reservation.entity;

import lombok.Data;
import java.time.LocalTime;

@Data
public class TimeSlot {
    private Long id;
    private Long labId;
    /** 星期：1-7，1=周一 */
    private Integer weekDay;
    private LocalTime startTime;
    private LocalTime endTime;
    /** 单次最长预约分钟数 */
    private Integer maxDuration;
    /** 0-关闭，1-开放 */
    private Integer status;
}
