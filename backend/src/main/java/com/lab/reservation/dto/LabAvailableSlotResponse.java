package com.lab.reservation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "实验室空闲时段响应项")
public class LabAvailableSlotResponse {
    @Schema(description = "时段开始时间", example = "2026-04-16T08:00:00")
    private String start;

    @Schema(description = "时段结束时间", example = "2026-04-16T08:30:00")
    private String end;

    @Schema(description = "是否已被占用，true=占用，false=空闲", example = "false")
    private Boolean occupied;
}
