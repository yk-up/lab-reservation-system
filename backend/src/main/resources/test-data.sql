-- 实验室预约系统测试数据初始化脚本
-- 说明：仅插入测试数据，不创建表结构
-- 使用前请确保已经先执行 schema.sql

USE lab_reservation_system;

SET FOREIGN_KEY_CHECKS = 0;

-- 按外键依赖顺序清空旧测试数据
DELETE FROM notice;
DELETE FROM statistics_log;
DELETE FROM blacklist;
DELETE FROM lab_admin;
DELETE FROM reservation;
DELETE FROM time_slot;
DELETE FROM lab;
DELETE FROM user;

ALTER TABLE user AUTO_INCREMENT = 1;
ALTER TABLE lab AUTO_INCREMENT = 1;
ALTER TABLE time_slot AUTO_INCREMENT = 1;
ALTER TABLE reservation AUTO_INCREMENT = 1;
ALTER TABLE notice AUTO_INCREMENT = 1;
ALTER TABLE lab_admin AUTO_INCREMENT = 1;
ALTER TABLE blacklist AUTO_INCREMENT = 1;
ALTER TABLE statistics_log AUTO_INCREMENT = 1;

SET FOREIGN_KEY_CHECKS = 1;

-- 默认密码均为 123456（BCrypt 加密）
INSERT INTO user (id, username, password, real_name, email, phone, role, status) VALUES
(1, 'admin',      '$2a$10$jQT7P9TkAWn0AYqMZ9MwneSsk7SEuaxqfjwQsXyH4M1KbgRnn4Rzm', '系统管理员', 'admin@lab.com',       '13800000001', 1, 1),
(2, 'student001', '$2a$10$jQT7P9TkAWn0AYqMZ9MwneSsk7SEuaxqfjwQsXyH4M1KbgRnn4Rzm', '张三',       'student001@lab.com', '13800000003', 0, 1),
(3, 'student002', '$2a$10$jQT7P9TkAWn0AYqMZ9MwneSsk7SEuaxqfjwQsXyH4M1KbgRnn4Rzm', '李四',       'student002@lab.com', '13800000004', 0, 1),
(4, 'student003', '$2a$10$jQT7P9TkAWn0AYqMZ9MwneSsk7SEuaxqfjwQsXyH4M1KbgRnn4Rzm', '王五',       'student003@lab.com', '13800000005', 0, 1),
(5, 'student004', '$2a$10$jQT7P9TkAWn0AYqMZ9MwneSsk7SEuaxqfjwQsXyH4M1KbgRnn4Rzm', '赵六',       'student004@lab.com', '13800000006', 0, 0);

-- 实验室数据
INSERT INTO lab (id, name, location, capacity, description, status, admin_id) VALUES
(1, '计算机实验室A', '教学楼3楼301', 40, '配备高性能工作站，适合编程实践', 1, 1),
(2, '网络技术实验室', '教学楼3楼302', 30, '配备交换机、路由器和网络实验设备', 1, 1),
(3, '软件工程实验室', '教学楼4楼401', 35, '用于软件开发、测试与项目实践', 1, 1),
(4, '嵌入式实验室',   '教学楼4楼402', 25, '用于单片机、物联网和硬件开发实验', 0, 1);

-- 实验室管理员关联
INSERT INTO lab_admin (id, lab_id, user_id) VALUES
(1, 1, 1),
(2, 2, 1),
(3, 3, 1),
(4, 4, 1);

-- 时间段模板
INSERT INTO time_slot (id, lab_id, week_day, start_time, end_time, max_duration, status) VALUES
(1, 1, 1, '08:00:00', '12:00:00', 120, 1),
(2, 1, 1, '14:00:00', '18:00:00', 120, 1),
(3, 1, 2, '08:00:00', '12:00:00', 120, 1),
(4, 1, 2, '14:00:00', '18:00:00', 120, 1),
(5, 1, 3, '08:00:00', '12:00:00', 120, 1),
(6, 1, 3, '14:00:00', '18:00:00', 120, 1),
(7, 2, 1, '08:00:00', '17:00:00', 180, 1),
(8, 2, 2, '08:00:00', '17:00:00', 180, 1),
(9, 2, 3, '08:00:00', '17:00:00', 180, 1),
(10, 3, 1, '09:00:00', '12:00:00', 120, 1),
(11, 3, 1, '13:30:00', '17:30:00', 180, 1),
(12, 3, 4, '09:00:00', '12:00:00', 120, 1),
(13, 3, 4, '13:30:00', '17:30:00', 180, 1),
(14, 4, 5, '08:30:00', '11:30:00', 120, 0),
(15, 4, 5, '14:00:00', '17:00:00', 120, 0);

-- 预约记录
INSERT INTO reservation (id, user_id, lab_id, title, start_time, end_time, attendees, remark, status, reject_reason, cancel_time, create_time, update_time) VALUES
(1, 3, 1, 'Java 课程项目讨论', '2026-04-16 09:00:00', '2026-04-16 11:00:00', 4, '需要投影设备', 1, NULL, NULL, '2026-04-15 10:00:00', '2026-04-15 10:30:00'),
(2, 4, 2, '网络拓扑实验',     '2026-04-16 14:00:00', '2026-04-16 16:00:00', 6, '课程实验使用', 0, NULL, NULL, '2026-04-15 11:00:00', '2026-04-15 11:00:00'),
(3, 5, 3, '软件测试答辩排练', '2026-04-17 09:30:00', '2026-04-17 11:30:00', 5, '需要安装测试工具', 2, '该时段已预留给学院活动', NULL, '2026-04-15 09:00:00', '2026-04-15 09:20:00'),
(4, 3, 3, '毕业设计讨论',     '2026-04-18 14:00:00', '2026-04-18 16:30:00', 3, '小组讨论', 3, NULL, '2026-04-15 12:00:00', '2026-04-15 08:30:00', '2026-04-15 12:00:00'),
(5, 4, 1, '算法训练营',       '2026-04-19 08:30:00', '2026-04-19 11:30:00', 8, '周末训练活动', 4, NULL, NULL, '2026-04-14 15:00:00', '2026-04-19 12:00:00');

-- 站内消息通知
INSERT INTO notice (id, user_id, type, title, content, reservation_id, is_read, create_time) VALUES
(1, 3, 1, '预约审核通过', '您的“Java 课程项目讨论”预约已通过审核，请按时使用实验室。', 1, 0, '2026-04-15 10:35:00'),
(2, 5, 2, '预约审核未通过', '您的“软件测试答辩排练”预约未通过，原因：该时段已预留给学院活动。', 3, 1, '2026-04-15 09:25:00'),
(3, 3, 4, '系统公告', '本周五晚间将进行实验室设备巡检，请合理安排预约时间。', NULL, 0, '2026-04-15 08:00:00');

-- 黑名单数据
INSERT INTO blacklist (id, user_id, reason, operator_id, expire_time, create_time) VALUES
(1, 6, '多次预约后无故缺席', 1, '2026-06-30 23:59:59', '2026-04-10 09:00:00');

-- 统计日志
INSERT INTO statistics_log (id, lab_id, stat_date, total_reservations, approved_count, cancelled_count, usage_minutes) VALUES
(1, 1, '2026-04-14', 3, 2, 0, 240),
(2, 2, '2026-04-14', 2, 1, 0, 120),
(3, 3, '2026-04-14', 4, 2, 1, 300),
(4, 1, '2026-04-15', 2, 1, 0, 120),
(5, 3, '2026-04-15', 3, 1, 1, 180);

-- 校验查询
SELECT 'user' AS table_name, COUNT(*) AS total FROM user
UNION ALL
SELECT 'lab', COUNT(*) FROM lab
UNION ALL
SELECT 'time_slot', COUNT(*) FROM time_slot
UNION ALL
SELECT 'reservation', COUNT(*) FROM reservation
UNION ALL
SELECT 'notice', COUNT(*) FROM notice
UNION ALL
SELECT 'lab_admin', COUNT(*) FROM lab_admin
UNION ALL
SELECT 'blacklist', COUNT(*) FROM blacklist
UNION ALL
SELECT 'statistics_log', COUNT(*) FROM statistics_log;
