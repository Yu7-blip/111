-- =============================================
-- 给 evaluation 表添加 status 列（修复评价申诉功能）
-- 请在 MySQL 中执行此脚本
-- =============================================

USE delivery_platform;

-- 1. 添加 status 列（如果已存在会报错但可忽略，说明已经添加过）
ALTER TABLE evaluation
ADD COLUMN `status` TINYINT NOT NULL DEFAULT 0 COMMENT '状态: 0-正常, 1-已撤销'
AFTER `content`;

-- 2. 验证
SELECT id, order_id, rating, status, create_time FROM evaluation ORDER BY id DESC LIMIT 5;
