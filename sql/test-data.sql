-- ============================================================
-- 自习室预约系统 — 测试数据
-- 请先执行 init.sql 建表
-- ============================================================

USE seat_reservation;

-- -----------------------------------------------------------
-- 用户
-- -----------------------------------------------------------
INSERT INTO `user` (id, username, password, role, name, email, reservation_count, study_seconds, check_in_count, credit_score, banned) VALUES
('user-admin',    'admin',    'admin123', 'ADMIN',  '系统管理员', 'admin@example.com',    0, 0,  0, 100, FALSE),
('user-zhangsan', 'zhangsan', 'pass123',  'STUDENT','张三',      'zhangsan@example.com', 0, 0,  0, 100, FALSE),
('user-lisi',     'lisi',     'pass123',  'STUDENT','李四',      'lisi@example.com',     0, 0,  0, 100, FALSE),
('user-wangwu',   'wangwu',   'pass123',  'STUDENT','王五',      'wangwu@example.com',   0, 0,  0, 100, FALSE),
('user-zhaoliu',  'zhaoliu',  'pass123',  'STUDENT','赵六',      'zhaoliu@example.com',  0, 0,  0, 100, FALSE);

-- -----------------------------------------------------------
-- 自习室
-- -----------------------------------------------------------
INSERT INTO study_room (id, name, location, layout, status) VALUES
('room-1', '自习室A', '图书馆一楼', 'SMALL',  'OPEN'),
('room-2', '自习室B', '图书馆二楼', 'SMALL',  'OPEN'),
('room-3', '自习室C', '教学楼三楼', 'MEDIUM', 'MAINTENANCE'),
('room-4', '自习室D', '图书馆三楼', 'MEDIUM', 'CLOSED'),
('room-5', '自习室E', '综合楼一楼', 'LARGE',  'OPEN');

-- -----------------------------------------------------------
-- 座位（SMALL=8, MEDIUM未预置, LARGE未预置）
-- -----------------------------------------------------------
-- room-1: 8 seats
INSERT INTO seat (id, room_id, status) VALUES
(1, 'room-1', 'AVAILABLE'), (2, 'room-1', 'AVAILABLE'),
(3, 'room-1', 'AVAILABLE'), (4, 'room-1', 'AVAILABLE'),
(5, 'room-1', 'AVAILABLE'), (6, 'room-1', 'AVAILABLE'),
(7, 'room-1', 'AVAILABLE'), (8, 'room-1', 'AVAILABLE');

-- room-2: 4 seats
INSERT INTO seat (id, room_id, status) VALUES
(1, 'room-2', 'AVAILABLE'), (2, 'room-2', 'AVAILABLE'),
(3, 'room-2', 'AVAILABLE'), (4, 'room-2', 'AVAILABLE');

-- room-5: 2 example seats
INSERT INTO seat (id, room_id, status) VALUES
(1, 'room-5', 'AVAILABLE'), (2, 'room-5', 'AVAILABLE');

-- -----------------------------------------------------------
-- 时段
-- -----------------------------------------------------------
INSERT INTO time_slot (id, start_time, end_time, label) VALUES
('ts-1', '08:00', '12:00', '上午 08:00-12:00'),
('ts-2', '13:00', '17:00', '下午 13:00-17:00'),
('ts-3', '18:00', '22:00', '晚上 18:00-22:00');

-- -----------------------------------------------------------
-- 预约
-- -----------------------------------------------------------
INSERT INTO reservation (id, user_id, room_id, seat_id, time_slot_id, date, status, created_at) VALUES
('res-1', 'user-zhangsan', 'room-1', 1, 'ts-2', CURDATE(), 'RESERVED',    NOW()),
('res-2', 'user-lisi',     'room-1', 3, 'ts-2', CURDATE(), 'RESERVED',    NOW()),
('res-3', 'user-wangwu',   'room-1', 5, 'ts-3', CURDATE(), 'RESERVED',    NOW()),
('res-4', 'user-zhangsan', 'room-1', 2, 'ts-1', CURDATE(), 'CHECKED_IN',  NOW()),
('res-5', 'user-zhaoliu',  'room-1', 4, 'ts-1', CURDATE(), 'CHECKED_IN',  NOW()),
('res-6', 'user-lisi',     'room-2', 1, 'ts-3', DATE_SUB(CURDATE(), INTERVAL 1 DAY), 'CANCELLED',   NOW()),
('res-7', 'user-wangwu',   'room-2', 2, 'ts-1', DATE_SUB(CURDATE(), INTERVAL 1 DAY), 'CHECKED_OUT',  NOW()),
('res-8', 'user-zhaoliu',  'room-1', 6, 'ts-2', DATE_SUB(CURDATE(), INTERVAL 2 DAY), 'EXPIRED',      NOW());

-- r7 check-in/out timestamps
UPDATE reservation SET check_in_at = DATE_SUB(NOW(), INTERVAL 2 HOUR), check_out_at = NOW()
WHERE id = 'res-7';

-- r4/r5 check-in timestamps
UPDATE reservation SET check_in_at = NOW() WHERE status = 'CHECKED_IN';
