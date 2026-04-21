-- 实验室预约系统测试数据脚本
-- 说明：请先执行 schema.sql，再执行本脚本

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;
USE `lab_reservation_system`;

-- 清空测试数据（按外键依赖顺序）
DELETE FROM `statistics_log`;
DELETE FROM `blacklist`;
DELETE FROM `notice`;
DELETE FROM `reservation`;
DELETE FROM `lab_admin`;
DELETE FROM `time_slot`;
DELETE FROM `lab`;
DELETE FROM `user`;

ALTER TABLE `statistics_log` AUTO_INCREMENT = 1;
ALTER TABLE `blacklist` AUTO_INCREMENT = 1;
ALTER TABLE `notice` AUTO_INCREMENT = 1;
ALTER TABLE `reservation` AUTO_INCREMENT = 1;
ALTER TABLE `lab_admin` AUTO_INCREMENT = 1;
ALTER TABLE `time_slot` AUTO_INCREMENT = 1;
ALTER TABLE `lab` AUTO_INCREMENT = 1;
ALTER TABLE `user` AUTO_INCREMENT = 1;

-- 默认密码均为 123456（BCrypt 加密）
INSERT INTO `user` (`id`, `username`, `password`, `real_name`, `email`, `phone`, `role`, `status`) VALUES
(1, 'admin',      '$2a$10$jQT7P9TkAWn0AYqMZ9MwneSsk7SEuaxqfjwQsXyH4M1KbgRnn4Rzm', '系统管理员', 'admin@lab.com',      '13800000001', 1, 1),
(2, 'teacher001', '$2a$10$jQT7P9TkAWn0AYqMZ9MwneSsk7SEuaxqfjwQsXyH4M1KbgRnn4Rzm', '王老师',     'teacher1@lab.com',   '13800000002', 1, 1),
(3, 'student001', '$2a$10$jQT7P9TkAWn0AYqMZ9MwneSsk7SEuaxqfjwQsXyH4M1KbgRnn4Rzm', '张三',       'student001@lab.com', '13800000003', 0, 1),
(4, 'student002', '$2a$10$jQT7P9TkAWn0AYqMZ9MwneSsk7SEuaxqfjwQsXyH4M1KbgRnn4Rzm', '李四',       'student002@lab.com', '13800000004', 0, 1),
(5, 'student003', '$2a$10$jQT7P9TkAWn0AYqMZ9MwneSsk7SEuaxqfjwQsXyH4M1KbgRnn4Rzm', '王五',       'student003@lab.com', '13800000005', 0, 1),
(6, 'student004', '$2a$10$jQT7P9TkAWn0AYqMZ9MwneSsk7SEuaxqfjwQsXyH4M1KbgRnn4Rzm', '赵六',       'student004@lab.com', '13800000006', 0, 0);

INSERT INTO `lab` (`id`, `name`, `location`, `capacity`, `description`, `status`, `admin_id`) VALUES
(1, '计算机实验室A', '教学楼3楼301', 40, '配备高性能工作站，适合编程实践课程与项目开发。', 1, 1),
(2, '网络技术实验室', '教学楼3楼302', 30, '配备交换机、路由器与网络调试设备，适合网络实验。', 1, 2),
(3, '软件工程实验室', '教学楼4楼401', 35, '用于软件开发、测试与课程设计答辩。', 1, 1),
(4, '嵌入式实验室', '实训楼2楼201', 24, '配备单片机与开发板，适合嵌入式系统实验。', 0, 2);

INSERT INTO `lab_admin` (`lab_id`, `user_id`) VALUES
(1, 1),
(2, 2),
(3, 1),
(4, 2);

INSERT INTO `time_slot` (`lab_id`, `week_day`, `start_time`, `end_time`, `max_duration`, `status`) VALUES
(1, 1, '08:00:00', '12:00:00', 120, 1),
(1, 1, '14:00:00', '18:00:00', 120, 1),
(1, 2, '08:00:00', '12:00:00', 120, 1),
(1, 2, '14:00:00', '18:00:00', 120, 1),
(1, 3, '08:00:00', '12:00:00', 120, 1),
(1, 3, '14:00:00', '18:00:00', 120, 1),
(1, 4, '08:00:00', '12:00:00', 120, 1),
(1, 4, '14:00:00', '18:00:00', 120, 1),
(1, 5, '08:00:00', '12:00:00', 120, 1),
(1, 5, '14:00:00', '18:00:00', 120, 1),

(2, 1, '08:00:00', '17:00:00', 180, 1),
(2, 2, '08:00:00', '17:00:00', 180, 1),
(2, 3, '08:00:00', '17:00:00', 180, 1),
(2, 4, '08:00:00', '17:00:00', 180, 1),
(2, 5, '08:00:00', '17:00:00', 180, 1),

(3, 1, '09:00:00', '12:00:00', 120, 1),
(3, 1, '13:30:00', '17:30:00', 180, 1),
(3, 2, '09:00:00', '12:00:00', 120, 1),
(3, 2, '13:30:00', '17:30:00', 180, 1),
(3, 3, '09:00:00', '12:00:00', 120, 1),
(3, 3, '13:30:00', '17:30:00', 180, 1),
(3, 4, '09:00:00', '12:00:00', 120, 1),
(3, 4, '13:30:00', '17:30:00', 180, 1),
(3, 5, '09:00:00', '12:00:00', 120, 1),
(3, 5, '13:30:00', '17:30:00', 180, 1),

(4, 1, '08:30:00', '11:30:00', 120, 0),
(4, 3, '14:00:00', '17:00:00', 120, 0);

INSERT INTO `reservation` (`id`, `user_id`, `lab_id`, `title`, `start_time`, `end_time`, `attendees`, `remark`, `status`, `reject_reason`, `cancel_time`, `create_time`, `update_time`) VALUES
(1, 3, 1, 'Java课程项目讨论', '2026-04-22 09:00:00', '2026-04-22 11:00:00', 4, '需要投影设备', 1, NULL, NULL, '2026-04-20 10:00:00', '2026-04-20 15:00:00'),
(2, 4, 2, '网络实验课程上机', '2026-04-23 14:00:00', '2026-04-23 16:00:00', 18, '需要开放交换机实验台', 0, NULL, NULL, '2026-04-21 09:30:00', '2026-04-21 09:30:00'),
(3, 5, 3, '软件测试课程答辩演练', '2026-04-24 09:00:00', '2026-04-24 10:30:00', 6, '需要连接音响设备', 2, '该时段已优先安排学院活动', NULL, '2026-04-20 12:00:00', '2026-04-20 13:00:00'),
(4, 3, 1, '算法竞赛训练', '2026-04-18 14:00:00', '2026-04-18 17:00:00', 8, '小组训练使用', 4, NULL, NULL, '2026-04-15 08:00:00', '2026-04-18 17:10:00'),
(5, 4, 3, '课程设计中期检查', '2026-04-19 10:00:00', '2026-04-19 12:00:00', 5, '导师现场指导', 3, NULL, '2026-04-18 20:00:00', '2026-04-17 16:20:00', '2026-04-18 20:00:00');

INSERT INTO `notice` (`id`, `user_id`, `type`, `title`, `content`, `reservation_id`, `is_read`, `create_time`) VALUES
(1, 3, 1, '预约审核通过', '您的“Java课程项目讨论”预约已审核通过，请按时使用实验室。', 1, 0, '2026-04-20 15:05:00'),
(2, 4, 3, '预约待开始提醒', '您预约的“网络实验课程上机”将在明天 14:00 开始，请提前到场。', 2, 0, '2026-04-22 18:00:00'),
(3, 5, 2, '预约审核拒绝', '您的“软件测试课程答辩演练”预约未通过，原因：该时段已优先安排学院活动。', 3, 1, '2026-04-20 13:05:00'),
(4, 3, 4, '系统公告', '五一假期期间实验室预约系统将于 4 月 30 日晚进行维护。', NULL, 0, '2026-04-21 08:30:00'),
(5, 4, 4, '系统公告', '请预约成功的同学至少提前 10 分钟到达实验室签到。', NULL, 1, '2026-04-21 08:30:00');

INSERT INTO `blacklist` (`id`, `user_id`, `reason`, `operator_id`, `expire_time`, `create_time`) VALUES
(1, 6, '多次预约后无故缺席，暂停预约资格 30 天', 1, '2026-05-20 23:59:59', '2026-04-20 09:00:00');

INSERT INTO `statistics_log` (`lab_id`, `stat_date`, `total_reservations`, `approved_count`, `cancelled_count`, `usage_minutes`) VALUES
(1, '2026-04-18', 1, 1, 0, 180),
(1, '2026-04-19', 1, 0, 1, 0),
(1, '2026-04-22', 1, 1, 0, 120),
(2, '2026-04-23', 1, 0, 0, 0),
(3, '2026-04-24', 1, 0, 0, 0);

SET FOREIGN_KEY_CHECKS = 1;
