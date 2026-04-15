package com.lab.reservation.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "通知实体")
public class Notice {
    @Schema(description = "通知 ID", example = "1")
    private Long id;

    @Schema(description = "接收用户 ID", example = "3")
    private Long userId;

    /** 1-审核通过，2-审核拒绝，3-预约提醒，4-系统公告 */
    @Schema(description = "通知类型，1=审核通过，2=审核拒绝，3=预约提醒，4=系统公告", example = "1")
    private Integer type;

    @Schema(description = "通知标题", example = "预约审核通过")
    private String title;

    @Schema(description = "通知内容", example = "您的预约已通过审核，请按时使用实验室。")
    private String content;

    @Schema(description = "关联预约 ID", example = "1")
    private Long reservationId;

    /** 0-未读，1-已读 */
    @Schema(description = "是否已读，0=未读，1=已读", example = "0")
    private Integer isRead;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
