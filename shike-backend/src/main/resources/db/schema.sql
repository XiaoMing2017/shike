-- Create Database if not exists
CREATE DATABASE IF NOT EXISTS `db_shike` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE `db_shike`;

-- 1. 用户基础信息表
CREATE TABLE IF NOT EXISTS `tb_user` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
  `openid` VARCHAR(64) NOT NULL UNIQUE COMMENT '微信OpenID',
  `nickname` VARCHAR(50) DEFAULT NULL COMMENT '微信昵称',
  `avatar_url` VARCHAR(256) DEFAULT NULL COMMENT '头像URL',
  `gender` TINYINT DEFAULT 0 COMMENT '性别: 0-未知, 1-男, 2-女',
  `age` INT DEFAULT NULL COMMENT '年龄',
  `height` DECIMAL(5,2) DEFAULT NULL COMMENT '身高(cm)',
  `weight` DECIMAL(5,2) DEFAULT NULL COMMENT '体重(kg)',
  `activity_level` VARCHAR(20) DEFAULT 'SEDENTARY' COMMENT '运动等级: SEDENTARY(久坐), LIGHT(轻度), MODERATE(中度), ACTIVE(高强度)',
  `goal` VARCHAR(20) DEFAULT 'MAINTAIN' COMMENT '目标: LOSE_WEIGHT(减脂), MAINTAIN(维持), GAIN_MUSCLE(增肌)',
  `bmr` DECIMAL(6,1) DEFAULT NULL COMMENT '基础代谢率(kcal)',
  `tdee` DECIMAL(6,1) DEFAULT NULL COMMENT '每日总能消耗(kcal)',
  `target_calories` DECIMAL(6,1) DEFAULT NULL COMMENT '每日目标摄入热量(kcal)',
  `points` INT DEFAULT 1000 COMMENT '用户当前持有契约积分',
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  INDEX `idx_openid` (`openid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 2. 饮食记录明细表
CREATE TABLE IF NOT EXISTS `tb_diet_record` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `record_date` DATE NOT NULL COMMENT '记录日期',
  `meal_type` VARCHAR(10) NOT NULL COMMENT '餐次类型: BREAKFAST, LUNCH, DINNER, SNACK',
  `image_url` VARCHAR(256) DEFAULT NULL COMMENT '上传食物图片地址',
  `food_items` JSON NOT NULL COMMENT '识别的食物列表JSON: [{"name":"苹果", "weight":150}]',
  `total_calories` DECIMAL(6,1) NOT NULL COMMENT '该餐总热量(kcal)',
  `total_protein` DECIMAL(5,1) DEFAULT 0.0 COMMENT '总蛋白质(g)',
  `total_fat` DECIMAL(5,1) DEFAULT 0.0 COMMENT '总脂肪(g)',
  `total_carbs` DECIMAL(5,1) DEFAULT 0.0 COMMENT '总碳水(g)',
  `oil_level` VARCHAR(10) DEFAULT 'MODERATE' COMMENT '油量估算: LIGHT, MODERATE, HEAVY',
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  INDEX `idx_user_date` (`user_id`, `record_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='饮食记录明细表';

-- 3. 对赌打卡小队表
CREATE TABLE IF NOT EXISTS `tb_team` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
  `team_name` VARCHAR(50) NOT NULL COMMENT '小队名称',
  `creator_id` BIGINT NOT NULL COMMENT '创建者ID',
  `invite_code` VARCHAR(10) NOT NULL UNIQUE COMMENT '加入邀请码',
  `target_days` INT DEFAULT 7 COMMENT '打卡目标天数',
  `deposit_points` INT DEFAULT 100 COMMENT '小队契约金/筹码',
  `status` VARCHAR(20) DEFAULT 'ACTIVE' COMMENT '状态: ACTIVE(进行中), SUCCESS(完成), FAILED(失败)',
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='小队表';

-- 4. 小队成员关联表
CREATE TABLE IF NOT EXISTS `tb_team_member` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
  `team_id` BIGINT NOT NULL COMMENT '小队ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `joined_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '加入时间',
  UNIQUE KEY `uk_team_user` (`team_id`, `user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='小队成员表';

-- 5. 每日对赌打卡明细表
CREATE TABLE IF NOT EXISTS `tb_team_checkin` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
  `team_id` BIGINT NOT NULL COMMENT '小队ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `checkin_date` DATE NOT NULL COMMENT '打卡日期',
  `is_success` TINYINT(1) DEFAULT 0 COMMENT '今日是否达标: 0-未打卡/超标, 1-成功达标',
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '打卡时间',
  UNIQUE KEY `uk_team_user_date` (`team_id`, `user_id`, `checkin_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='每日对赌打卡明细表';
