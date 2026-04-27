package com.lab.reservation.vo;

import lombok.Data;

/** 管理员检索用户（不含敏感字段） */
@Data
public class UserSearchItem {
    private Long id;
    private String username;
    private String realName;
}
