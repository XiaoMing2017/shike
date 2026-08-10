# 食刻 (ShiKe) — 数据库设计与 API 接口规范
## 研发文档 (v1.0)

---

### 一、 数据库设计 (MySQL DDL)

为保障高并发与高可用，表设计均包含合理的索引、默认值及更新时间字段。

```sql
-- 1. 用户基础信息表
CREATE TABLE `tb_user` (
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
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  INDEX `idx_openid` (`openid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 2. 饮食记录明细表
CREATE TABLE `tb_diet_record` (
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
CREATE TABLE `tb_team` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
  `team_name` VARCHAR(50) NOT NULL COMMENT '小队名称',
  `creator_id` BIGINT NOT NULL COMMENT '创建者ID',
  `invite_code` VARCHAR(10) NOT NULL UNIQUE COMMENT '加入邀请码',
  `target_days` INT DEFAULT 7 COMMENT '打卡目标天数',
  `status` VARCHAR(20) DEFAULT 'ACTIVE' COMMENT '状态: ACTIVE(进行中), SUCCESS(完成), FAILED(失败)',
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='小队表';

-- 4. 小队成员关联表
CREATE TABLE `tb_team_member` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
  `team_id` BIGINT NOT NULL COMMENT '小队ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `joined_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '加入时间',
  UNIQUE KEY `uk_team_user` (`team_id`, `user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='小队成员表';

-- 5. 每日对赌打卡明细表
CREATE TABLE `tb_team_checkin` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
  `team_id` BIGINT NOT NULL COMMENT '小队ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `checkin_date` DATE NOT NULL COMMENT '打卡日期',
  `is_success` TINYINT(1) DEFAULT 0 COMMENT '今日是否达标: 0-未打卡/超标, 1-成功达标',
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '打卡时间',
  UNIQUE KEY `uk_team_user_date` (`team_id`, `user_id`, `checkin_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='每日对赌打卡明细表';
```

---

### 二、 Redis 缓存设计

*   **热量看板高速缓存**：
    *   *Key*: `shike:user:dashboard:{userId}:{date}`
    *   *Value*: 今日摄入总热量、三大营养素累计值（Hash结构）。
    *   *过期时间*: 24小时，或每日24:00自动失效。
*   **分布式锁（对赌小队每日结算）**：
    *   *Key*: `shike:lock:team:settle:{teamId}:{date}`
    *   *说明*: 防止定时批处理任务集群执行时，对同一个小队的重复结算。

---

### 三、 核心 API 接口定义 (Restful)

#### 1. 用户模块

*   **微信登录与初始化**
    *   `POST /api/v1/user/login`
    *   *请求体*：`{ "code": "wx_login_code" }`
    *   *响应体*：`{ "token": "jwt_token", "isNewUser": true }`
*   **更新健康档案**
    *   `POST /api/v1/user/profile`
    *   *请求体*：
        ```json
        {
          "age": 25,
          "gender": 1,
          "height": 175.5,
          "weight": 70.0,
          "activityLevel": "MODERATE",
          "goal": "LOSE_WEIGHT"
        }
        ```
    *   *响应*：返回计算好的 `bmr`, `tdee`, `targetCalories`。

#### 2. AI 拍照识别与记录模块

*   **拍照解析饮食（多模态 API 转发）**
    *   `POST /api/v1/diet/recognize`
    *   *请求*：Multipart-File (图片流)
    *   *响应体*：
        ```json
        {
          "tempImageId": "oss_temp_url",
          "foodItems": [
            { "name": "西红柿炒鸡蛋", "weight": 200 }
          ],
          "calories": 350,
          "protein": 15,
          "fat": 18,
          "carbs": 25
        }
        ```
*   **确认并保存记录**
    *   `POST /api/v1/diet/record`
    *   *请求体*：
        ```json
        {
          "tempImageId": "oss_temp_url",
          "mealType": "LUNCH",
          "foodItems": [{ "name": "西红柿炒鸡蛋", "weight": 200 }],
          "oilLevel": "MODERATE"
        }
        ```

#### 3. 对赌小队模块

*   **创建对赌小队**
    *   `POST /api/v1/team/create`
    *   *请求体*：`{ "teamName": "打卡互助组", "targetDays": 7 }`
    *   *响应体*：`{ "teamId": 10023, "inviteCode": "A8X2B" }`
*   **加入对赌小队**
    *   `POST /api/v1/team/join`
    *   *请求体*：`{ "inviteCode": "A8X2B" }`
