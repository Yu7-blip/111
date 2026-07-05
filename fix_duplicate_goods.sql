-- =============================================
-- 清理 goods 表重复数据 & 添加唯一约束
-- 请在 MySQL 中执行此脚本
-- =============================================

USE delivery_platform;

-- 1. 查看当前重复情况
SELECT shop_id, name, COUNT(*) AS cnt
FROM goods
GROUP BY shop_id, name
HAVING cnt > 1;

-- 2. 删除重复商品（保留最早创建的那条，即 id 最小的）
DELETE g1 FROM goods g1
INNER JOIN goods g2
WHERE g1.shop_id = g2.shop_id
  AND g1.name = g2.name
  AND g1.id > g2.id;

-- 3. 同样处理 goods_category 表
DELETE gc1 FROM goods_category gc1
INNER JOIN goods_category gc2
WHERE gc1.shop_id = gc2.shop_id
  AND gc1.name = gc2.name
  AND gc1.id > gc2.id;

-- 4. 添加唯一约束，防止将来再次重复
ALTER TABLE goods ADD UNIQUE KEY uk_shop_goods_name (shop_id, name);
ALTER TABLE goods_category ADD UNIQUE KEY uk_shop_category_name (shop_id, name);

-- 5. 验证
SELECT shop_id, COUNT(*) AS total FROM goods GROUP BY shop_id;
