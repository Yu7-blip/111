-- =============================================
-- 给 shop 表添加 shop_no 列 + 回填已有数据
-- 请在 MySQL 中执行此脚本
-- =============================================

USE delivery_platform;

-- 1. 添加 shop_no 列
ALTER TABLE shop
ADD COLUMN `shop_no` VARCHAR(20) COMMENT '店铺编号，如 SHOP1、SHOP2'
AFTER `id`;

-- 2. 为已有店铺生成 shop_no（按 id 顺序依次生成 SHOP+id）
UPDATE shop SET shop_no = CONCAT('SHOP', id) WHERE shop_no IS NULL;

-- 3. 添加唯一索引（可选，防止编号重复）
ALTER TABLE shop ADD UNIQUE INDEX idx_shop_no (shop_no);

-- 4. 验证
SELECT id, shop_no, name, username FROM shop ORDER BY id;
