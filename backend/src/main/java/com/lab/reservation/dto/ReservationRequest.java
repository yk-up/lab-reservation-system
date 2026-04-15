package com.lab.reservation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "提交预约请求参数")
public class ReservationRequest {
    @Schema(description = "实验室 ID", example = "1")
    @NotNull(message = "实验室ID不能为空")
    private Long labId;

    @Schema(description = "预约标题", example = "Java课程项目讨论")
    @NotBlank(message = "预约标题不能为空")
    @Size(max = 200)
    private String title;

    @Schema(description = "开始时间", example = "2026-04-20T09:00:00")
    @NotNull(message = "开始时间不能为空")
    @Future(message = "开始时间必须是未来时间")
    private LocalDateTime startTime;

    @Schema(description = "结束时间", example = "2026-04-20T11:00:00")
    @NotNull(message = "结束时间不能为空")
    private LocalDateTime endTime;

    @Schema(description = "参与人数", example = "4", defaultValue = "1")
    @Min(value = 1, message = "参与人数至少1人")
    private Integer attendees = 1;

    @Schema(description = "备注说明", example = "需要投影设备")
    private String remark;
}
