package com.lab.reservation.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ReservationRequest {
    @NotNull(message = "实验室ID不能为空")
    private Long labId;

    @NotBlank(message = "预约标题不能为空")
    @Size(max = 200)
    private String title;

    @NotNull(message = "开始时间不能为空")
    @Future(message = "开始时间必须是未来时间")
    private LocalDateTime startTime;

    @NotNull(message = "结束时间不能为空")
    private LocalDateTime endTime;

    @Min(value = 1, message = "参与人数至少1人")
    private Integer attendees = 1;

    private String remark;
}
