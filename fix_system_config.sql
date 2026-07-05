-- =============================================
-- 系统配置表
-- =============================================
USE delivery_platform;

CREATE TABLE IF NOT EXISTS `system_config` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '配置ID',
    `config_key`  VARCHAR(100) NOT NULL COMMENT '配置键',
    `config_value` VARCHAR(500) NOT NULL COMMENT '配置值',
    `description` VARCHAR(255) DEFAULT NULL COMMENT '描述',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_config_key` (`config_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统配置表';

-- 种子数据
INSERT IGNORE INTO `system_config` (`config_key`, `config_value`, `description`) VALUES
('site.name', '外卖配送平台', '站点名称'),
('site.logo', '', '站点Logo URL'),
('delivery.default_fee', '5.00', '默认配送费(元)'),
('delivery.max_distance', '10.0', '最大配送距离(km)'),
('order.auto_cancel_minutes', '30', '未支付订单自动取消时间(分钟)'),
('order.auto_confirm_days', '7', '订单自动确认收货天数'),
('payment.sandbox.success_rate', '90', '支付沙箱成功率(%)，取值范围 0-100');
