package com.lab.reservation.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "预约记录实体")
public class Reservation {
    @Schema(description = "预约 ID", example = "1")
    private Long id;

    @Schema(description = "预约用户 ID", example = "3")
    private Long userId;

    @Schema(description = "实验室 ID", example = "1")
    private Long labId;

    @Schema(description = "预约标题", example = "Java课程项目讨论")
    private String title;

    @Schema(description = "预约开始时间", example = "2026-04-20T09:00:00")
    private LocalDateTime startTime;

    @Schema(description = "预约结束时间", example = "2026-04-20T11:00:00")
    private LocalDateTime endTime;

    @Schema(description = "参与人数", example = "4")
    private Integer attendees;

    @Schema(description = "预约备注", example = "需要投影设备")
    private String remark;

    /**
     * 预约状态：
     * 0-待审核，1-已通过，2-已拒绝，3-已取消，4-已完成
     */
    @Schema(description = "预约状态，0=待审核，1=已通过，2=已拒绝，3=已取消，4=已完成", example = "0")
    private Integer status;

    @Schema(description = "拒绝原因", example = "该时段已预留给学院活动")
    private String rejectReason;

    /** 用于统计用户取消行为 */
    @Schema(description = "取消时间")
    private LocalDateTime cancelTime;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
