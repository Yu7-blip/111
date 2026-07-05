-- =============================================
-- 审计日志表
-- =============================================
USE delivery_platform;

CREATE TABLE IF NOT EXISTS `audit_log` (
    `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '日志ID',
    `admin_id`      BIGINT       NOT NULL COMMENT '操作管理员ID',
    `admin_name`    VARCHAR(50)  DEFAULT NULL COMMENT '操作管理员名称',
    `action`        VARCHAR(50)  NOT NULL COMMENT '操作类型: CREATE/UPDATE/DELETE/STATUS_CHANGE',
    `target_type`   VARCHAR(50)  NOT NULL COMMENT '操作对象: ADMIN/MERCHANT/ORDER/GOODS/CONFIG',
    `target_id`     BIGINT       DEFAULT NULL COMMENT '操作对象ID',
    `detail`        VARCHAR(1000) DEFAULT NULL COMMENT '操作详情',
    `ip`            VARCHAR(50)  DEFAULT NULL COMMENT '操作IP',
    `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_admin_id` (`admin_id`),
    KEY `idx_action` (`action`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审计日志表';
