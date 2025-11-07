-- 创建数据库
CREATE DATABASE IF NOT EXISTS `ai_travel_planner_db` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 使用数据库
USE `ai_travel_planner_db`;

-- 用户表
DROP TABLE IF EXISTS `user`;
CREATE TABLE `user` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `username` VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
    `password` VARCHAR(255) NOT NULL COMMENT '密码（加密后）',
    `email` VARCHAR(100) NOT NULL UNIQUE COMMENT '邮箱',
    `registration_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '注册时间',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- 行程计划表
DROP TABLE IF EXISTS `travel_plan`;
CREATE TABLE `travel_plan` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `plan_name` VARCHAR(255) NOT NULL COMMENT '行程名称',
    `destination` VARCHAR(255) NOT NULL COMMENT '目的地',
    `start_date` DATE NOT NULL COMMENT '开始日期',
    `end_date` DATE NOT NULL COMMENT '结束日期',
    `budget` DECIMAL(10, 2) NOT NULL DEFAULT 0.00 COMMENT '预算',
    `travelers` INT NOT NULL DEFAULT 1 COMMENT '同行人数',
    `preferences` TEXT COMMENT '旅行偏好（JSON或逗号分隔）',
    `details` JSON COMMENT '详细行程（JSON格式，包含交通、住宿、景点、餐厅等）',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='行程计划表';

-- 费用表
DROP TABLE IF EXISTS `expense`;
CREATE TABLE `expense` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `travel_plan_id` BIGINT NOT NULL COMMENT '行程计划ID',
    `amount` DECIMAL(10, 2) NOT NULL COMMENT '金额',
    `description` VARCHAR(255) COMMENT '费用描述',
    `details` text comment '详情,json',
    `expense_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发生时间',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_travel_plan_id` (`travel_plan_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='费用表';