package com.lab.reservation.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "用户实体")
public class User {
    @Schema(description = "用户 ID", example = "1")
    private Long id;

    @Schema(description = "用户名", example = "admin")
    private String username;

    @Schema(description = "密码（加密后存储）", example = "$2a$10$xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx")
    private String password;

    @Schema(description = "真实姓名", example = "系统管理员")
    private String realName;

    @Schema(description = "邮箱", example = "admin@lab.com")
    private String email;

    @Schema(description = "手机号", example = "13800000001")
    private String phone;

    /** 0-普通用户，1-管理员 */
    @Schema(description = "角色，0=普通用户，1=管理员", example = "1")
    private Integer role;

    /** 0-禁用，1-正常 */
    @Schema(description = "状态，0=禁用，1=正常", example = "1")
    private Integer status;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
