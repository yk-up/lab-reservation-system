-- 实验室预约系统数据库初始化脚本
-- 数据库：MySQL 8.0+

CREATE DATABASE IF NOT EXISTS lab_reservation DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE lab_reservation;

-- 1. 用户表
CREATE TABLE IF NOT EXISTS `user` (
    `id`          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '用户ID',
    `username`    VARCHAR(50) NOT NULL COMMENT '用户名（学号/工号）',
    `password`    VARCHAR(100) NOT NULL COMMENT '密码（BCrypt加密）',
    `real_name`   VARCHAR(50) NOT NULL COMMENT '真实姓名',
    `email`       VARCHAR(100) COMMENT '邮箱',
    `phone`       VARCHAR(20) COMMENT '手机号',
    `role`        TINYINT NOT NULL DEFAULT 0 COMMENT '角色：0-普通用户，1-管理员',
    `status`      TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0-禁用，1-正常',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 2. 实验室表
CREATE TABLE IF NOT EXISTS `lab` (
    `id`          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '实验室ID',
    `name`        VARCHAR(100) NOT NULL COMMENT '实验室名称',
    `location`    VARCHAR(200) COMMENT '地理位置（楼栋+房间号）',
    `capacity`    INT NOT NULL DEFAULT 1 COMMENT '最大容纳人数',
    `description` TEXT COMMENT '实验室简介',
    `status`      TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0-停用，1-开放',
    `admin_id`    BIGINT COMMENT '负责管理员ID',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='实验室表';

-- 3. 时间段模板表（定义每个实验室每天可预约的时间段）
CREATE TABLE IF NOT EXISTS `time_slot` (
    `id`           BIGINT   NOT NULL AUTO_INCREMENT COMMENT '时间段ID',
    `lab_id`       BIGINT   NOT NULL COMMENT '所属实验室ID',
    `week_day`     TINYINT  NOT NULL COMMENT '星期（1-7，1=周一）',
    `start_time`   TIME     NOT NULL COMMENT '开始时间',
    `end_time`     TIME     NOT NULL COMMENT '结束时间',
    `max_duration` INT      NOT NULL DEFAULT 120 COMMENT '单次最长预约分钟数',
    `status`       TINYINT  NOT NULL DEFAULT 1 COMMENT '状态：0-关闭，1-开放',
    PRIMARY KEY (`id`),
    KEY `idx_lab_id` (`lab_id`),
    CONSTRAINT `fk_time_slot_lab` FOREIGN KEY (`lab_id`) REFERENCES `lab` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='时间段模板表';

-- 4. 预约记录表（核心业务表）
CREATE TABLE IF NOT EXISTS `reservation` (
    `id`           BIGINT      NOT NULL AUTO_INCREMENT COMMENT '预约ID',
    `user_id`      BIGINT      NOT NULL COMMENT '预约用户ID',
    `lab_id`       BIGINT      NOT NULL COMMENT '实验室ID',
    `title`        VARCHAR(200) NOT NULL COMMENT '预约标题/用途',
    `start_time`   DATETIME    NOT NULL COMMENT '预约开始时间',
    `end_time`     DATETIME    NOT NULL COMMENT '预约结束时间',
    `attendees`    INT         NOT NULL DEFAULT 1 COMMENT '参与人数',
    `remark`       TEXT COMMENT '备注说明',
    `status`       TINYINT     NOT NULL DEFAULT 0 COMMENT '状态：0-待审核，1-已通过，2-已拒绝，3-已取消，4-已完成',
    `reject_reason` VARCHAR(500) COMMENT '拒绝原因',
    `cancel_time`  DATETIME    COMMENT '取消时间（用于统计取消行为）',
    `create_time`  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_lab_time` (`lab_id`, `start_time`, `end_time`),  -- 冲突检测核心索引
    KEY `idx_lab_status` (`lab_id`, `status`),
    KEY `idx_status` (`status`),
    KEY `idx_create_time` (`create_time`),
    CONSTRAINT `fk_reservation_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`),
    CONSTRAINT `fk_reservation_lab` FOREIGN KEY (`lab_id`) REFERENCES `lab` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='预约记录表';

-- 5. 站内消息通知表
CREATE TABLE IF NOT EXISTS `notice` (
    `id`          BIGINT   NOT NULL AUTO_INCREMENT COMMENT '通知ID',
    `user_id`     BIGINT   NOT NULL COMMENT '接收用户ID',
    `type`        TINYINT  NOT NULL COMMENT '通知类型：1-审核通过，2-审核拒绝，3-预约提醒，4-系统公告',
    `title`       VARCHAR(200) NOT NULL COMMENT '通知标题',
    `content`     TEXT NOT NULL COMMENT '通知内容',
    `reservation_id` BIGINT COMMENT '关联预约ID（可为空）',
    `is_read`     TINYINT  NOT NULL DEFAULT 0 COMMENT '是否已读：0-未读，1-已读',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_user_unread` (`user_id`, `is_read`),
    KEY `idx_notice_res_type` (`reservation_id`, `type`),
    CONSTRAINT `fk_notice_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='站内消息通知表';

-- 6. 实验室管理员关联表（支持一个实验室多个管理员）
CREATE TABLE IF NOT EXISTS `lab_admin` (
    `id`      BIGINT NOT NULL AUTO_INCREMENT,
    `lab_id`  BIGINT NOT NULL COMMENT '实验室ID',
    `user_id` BIGINT NOT NULL COMMENT '管理员用户ID',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_lab_user` (`lab_id`, `user_id`),
    CONSTRAINT `fk_lab_admin_lab` FOREIGN KEY (`lab_id`) REFERENCES `lab` (`id`),
    CONSTRAINT `fk_lab_admin_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='实验室管理员关联表';

-- 7. 黑名单表（违规用户限制预约）
CREATE TABLE IF NOT EXISTS `blacklist` (
    `id`          BIGINT   NOT NULL AUTO_INCREMENT COMMENT '黑名单ID',
    `user_id`     BIGINT   NOT NULL COMMENT '被封禁用户ID',
    `reason`      VARCHAR(500) NOT NULL COMMENT '封禁原因',
    `operator_id` BIGINT   NOT NULL COMMENT '操作管理员ID',
    `expire_time` DATETIME COMMENT '封禁到期时间（NULL表示永久）',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    CONSTRAINT `fk_blacklist_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='黑名单表';

-- 8. 统计日志表（记录每日使用数据，轻量级设计）
CREATE TABLE IF NOT EXISTS `statistics_log` (
    `id`              BIGINT   NOT NULL AUTO_INCREMENT,
    `lab_id`          BIGINT   NOT NULL COMMENT '实验室ID',
    `stat_date`       DATE     NOT NULL COMMENT '统计日期',
    `total_reservations` INT   NOT NULL DEFAULT 0 COMMENT '当日预约总数',
    `approved_count`  INT      NOT NULL DEFAULT 0 COMMENT '审核通过数',
    `cancelled_count` INT      NOT NULL DEFAULT 0 COMMENT '取消数',
    `usage_minutes`   INT      NOT NULL DEFAULT 0 COMMENT '使用总分钟数',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_lab_date` (`lab_id`, `stat_date`),
    CONSTRAINT `fk_stats_lab` FOREIGN KEY (`lab_id`) REFERENCES `lab` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='统计日志表';

-- 初始化测试数据
INSERT INTO `user` (`username`, `password`, `real_name`, `role`) VALUES
('admin', '$2a$10$jQT7P9TkAWn0AYqMZ9MwneSsk7SEuaxqfjwQsXyH4M1KbgRnn4Rzm', '系统管理员', 1),
('student001', '$2a$10$jQT7P9TkAWn0AYqMZ9MwneSsk7SEuaxqfjwQsXyH4M1KbgRnn4Rzm', '张三', 0),
('student002', '$2a$10$jQT7P9TkAWn0AYqMZ9MwneSsk7SEuaxqfjwQsXyH4M1KbgRnn4Rzm', '李四', 0);
-- 默认密码均为 123456（BCrypt加密）

INSERT INTO `lab` (`name`, `location`, `capacity`, `description`, `admin_id`) VALUES
('计算机实验室A', '教学楼3楼301', 40, '配备高性能工作站，适合编程实践', 1),
('网络技术实验室', '教学楼3楼302', 30, '配备网络设备，适合网络实验', 1),
('软件工程实验室', '教学楼4楼4 01', 35, '用于软件开发与测试', 1);

INSERT INTO `time_slot` (`lab_id`, `week_day`, `start_time`, `end_time`) VALUES
(1, 1, '08:00:00', '12:00:00'), (1, 1, '14:00:00', '18:00:00'),
(1, 2, '08:00:00', '12:00:00'), (1, 2, '14:00:00', '18:00:00'),
(1, 3, '08:00:00', '12:00:00'), (1, 3, '14:00:00', '18:00:00'),
(1, 4, '08:00:00', '12:00:00'), (1, 4, '14:00:00', '18:00:00'),
(1, 5, '08:00:00', '12:00:00'), (1, 5, '14:00:00', '18:00:00'),
(2, 1, '08:00:00', '17:00:00'), (2, 2, '08:00:00', '17:00:00'),
(2, 3, '08:00:00', '17:00:00'), (2, 4, '08:00:00', '17:00:00'),
(2, 5, '08:00:00', '17:00:00');
