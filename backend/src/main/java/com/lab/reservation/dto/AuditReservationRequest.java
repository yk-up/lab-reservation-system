package com.lab.reservation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "预约审核请求参数")
public class AuditReservationRequest {

    /** 1-通过，2-拒绝 */
    @Schema(description = "审核结果，1=通过，2=拒绝", example = "1")
    @NotNull(message = "审核状态不能为空")
    private Integer status;

    @Schema(description = "拒绝原因，当 status=2 时建议填写", example = "该时段已预留给学院活动")
    private String rejectReason;
}
