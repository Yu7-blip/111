-- =============================================
-- 创建 feedback 表（评价申诉/客服反馈功能依赖此表）
-- 请在 MySQL 中执行此脚本
-- =============================================

USE delivery_platform;

-- 1. 创建 feedback 表
CREATE TABLE IF NOT EXISTS `feedback` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '反馈ID',
    `user_id`     BIGINT       NOT NULL COMMENT '用户/骑手/商家ID',
    `role`        VARCHAR(20)  NOT NULL COMMENT '角色: user-用户, delivery-骑手, merchant-商家',
    `type`        VARCHAR(30)  NOT NULL DEFAULT 'other' COMMENT '类型: support-客服, complaint-投诉, feedback-反馈, appeal-申诉, other-其他',
    `content`     TEXT         NOT NULL COMMENT '反馈内容',
    `status`      TINYINT      NOT NULL DEFAULT 0 COMMENT '状态: 0-待处理, 1-已回复',
    `reply`       TEXT         DEFAULT NULL COMMENT '平台回复',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户反馈/投诉/申诉表';

-- 2. 验证
SELECT * FROM feedback LIMIT 1;
