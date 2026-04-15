package com.lab.reservation.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "实验室实体")
public class Lab {
    @Schema(description = "实验室 ID", example = "1")
    private Long id;

    @Schema(description = "实验室名称", example = "计算机实验室A")
    private String name;

    @Schema(description = "实验室位置", example = "教学楼3楼301")
    private String location;

    @Schema(description = "实验室容量", example = "40")
    private Integer capacity;

    @Schema(description = "实验室简介", example = "配备高性能工作站，适合编程实践")
    private String description;

    /** 0-停用，1-开放 */
    @Schema(description = "实验室状态，0=停用，1=开放", example = "1")
    private Integer status;

    @Schema(description = "负责管理员 ID", example = "1")
    private Long adminId;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
