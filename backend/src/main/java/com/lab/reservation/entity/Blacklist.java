package com.lab.reservation.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "黑名单记录实体")
public class Blacklist {
    @Schema(description = "黑名单记录 ID", example = "1")
    private Long id;

    @Schema(description = "被封禁用户 ID", example = "5")
    private Long userId;

    @Schema(description = "封禁原因", example = "多次预约后无故缺席")
    private String reason;

    @Schema(description = "操作管理员 ID", example = "1")
    private Long operatorId;

    /** 封禁到期时间，null 表示永久 */
    @Schema(description = "封禁到期时间，null 表示永久封禁", example = "2026-06-30T23:59:59")
    private LocalDateTime expireTime;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
