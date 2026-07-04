-- ============================================================
-- 自习室预约系统 — 数据库初始化脚本
-- ============================================================

CREATE DATABASE IF NOT EXISTS seat_reservation
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE seat_reservation;

-- -----------------------------------------------------------
-- 用户表
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS `user` (
    id          VARCHAR(36)  NOT NULL PRIMARY KEY,
    username    VARCHAR(50)  NOT NULL UNIQUE,
    password    VARCHAR(255) NOT NULL,
    role        VARCHAR(10)  NOT NULL COMMENT 'STUDENT / ADMIN',
    name        VARCHAR(100) NOT NULL,
    email       VARCHAR(100) NOT NULL,
    reservation_count INT   NOT NULL DEFAULT 0,
    study_hours       INT   NOT NULL DEFAULT 0,
    check_in_count    INT   NOT NULL DEFAULT 0,
    credit_score      INT   NOT NULL DEFAULT 100,
    banned            BOOLEAN NOT NULL DEFAULT FALSE
) ENGINE=InnoDB;

-- -----------------------------------------------------------
-- 自习室表
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS study_room (
    id       VARCHAR(36)  NOT NULL PRIMARY KEY,
    name     VARCHAR(100) NOT NULL UNIQUE,
    location VARCHAR(200) NOT NULL,
    layout   VARCHAR(20)  NOT NULL COMMENT 'SMALL / MEDIUM / LARGE',
    status   VARCHAR(20)  NOT NULL DEFAULT 'OPEN' COMMENT 'OPEN / CLOSED / MAINTENANCE'
) ENGINE=InnoDB;

-- -----------------------------------------------------------
-- 座位表（复合主键：room_id + id）
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS seat (
    id      INT         NOT NULL,
    room_id VARCHAR(36) NOT NULL,
    status  VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE' COMMENT 'AVAILABLE / RESERVED / OCCUPIED / MAINTENANCE / REMOVED',
    PRIMARY KEY (room_id, id),
    FOREIGN KEY (room_id) REFERENCES study_room(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- -----------------------------------------------------------
-- 时段表
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS time_slot (
    id         VARCHAR(10) NOT NULL PRIMARY KEY,
    start_time VARCHAR(5)  NOT NULL COMMENT 'HH:MM',
    end_time   VARCHAR(5)  NOT NULL COMMENT 'HH:MM',
    label      VARCHAR(50) NOT NULL
) ENGINE=InnoDB;

-- -----------------------------------------------------------
-- 预约表
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS reservation (
    id           VARCHAR(36)  NOT NULL PRIMARY KEY,
    user_id      VARCHAR(36)  NOT NULL,
    room_id      VARCHAR(36)  NOT NULL,
    seat_id      INT          NOT NULL,
    time_slot_id VARCHAR(10)  NOT NULL,
    date         DATE         NOT NULL,
    status       VARCHAR(20)  NOT NULL DEFAULT 'RESERVED' COMMENT 'RESERVED / CHECKED_IN / CHECKED_OUT / CANCELLED / EXPIRED',
    created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    check_in_at  DATETIME     NULL,
    check_out_at DATETIME     NULL,
    FOREIGN KEY (user_id)      REFERENCES `user`(id)       ON DELETE CASCADE,
    FOREIGN KEY (room_id)      REFERENCES study_room(id)   ON DELETE CASCADE,
    FOREIGN KEY (time_slot_id) REFERENCES time_slot(id)
) ENGINE=InnoDB;

-- 索引
CREATE INDEX idx_reservation_user   ON reservation(user_id);
CREATE INDEX idx_reservation_room   ON reservation(room_id);
CREATE INDEX idx_reservation_date   ON reservation(date);
CREATE INDEX idx_reservation_status ON reservation(status);
CREATE INDEX idx_reservation_seat   ON reservation(room_id, seat_id, date, time_slot_id);
