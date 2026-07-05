-- =============================================
-- 店铺图片 & 商品图片 批量更新
-- Spring Boot 静态资源路径: http://localhost:8080/uploads/xxx.png
-- 访问URL: http://localhost:8080/uploads/xxx.png
-- =============================================

-- 1. 更新店铺 LOGO
UPDATE `shop` SET `logo` = 'http://localhost:8080/uploads/shops/shop1_logo.png' WHERE `id` = 1;
UPDATE `shop` SET `logo` = 'http://localhost:8080/uploads/shops/shop2_logo.png' WHERE `id` = 2;
UPDATE `shop` SET `logo` = 'http://localhost:8080/uploads/shops/shop3_logo.png' WHERE `id` = 3;

-- 2. 新增老凯里酸汤牛肉 (shop 5)
INSERT INTO `shop` (`name`, `logo`, `description`, `phone`, `email`, `address`, `latitude`, `longitude`, `open_time`, `close_time`, `min_price`, `delivery_fee`, `rating`, `sales`, `notice`, `status`, `username`, `password`) VALUES
('老凯里酸汤牛肉', 'http://localhost:8080/uploads/shops/shop5_logo.png', '正宗凯里苗家酸汤，非遗传承美味', '0851-88880005', 'suantang@shop.com', '贵阳市云岩区苗岭路18号', 26.6580, 106.6380, '10:00', '22:00', 25.00, 4.00, 5.0, 0, '苗家古法秘制酸汤，每天限量100份', 1, 'shop5', '123456')
ON DUPLICATE KEY UPDATE `logo` = VALUES(`logo`);

-- 3. 添加老凯里酸汤牛肉的分类
INSERT IGNORE INTO `goods_category` (`shop_id`, `name`, `sort`) VALUES
(5, '招牌酸汤', 1),
(5, '精品荤菜', 2),
(5, '时蔬素菜', 3),
(5, '主食', 4);

-- 4. 添加老凯里酸汤牛肉的商品
INSERT IGNORE INTO `goods` (`shop_id`, `category_id`, `name`, `description`, `price`, `stock`, `sales`, `status`, `image`) VALUES
(5, (SELECT id FROM goods_category WHERE shop_id=5 AND sort=1), '招牌酸汤牛肉',   '苗家古法酸汤，精选黄牛肉', 68.00, 100, 0, 1, 'http://localhost:8080/uploads/goods/g5_牛肉.png'),
(5, (SELECT id FROM goods_category WHERE shop_id=5 AND sort=1), '酸汤江团鱼',     '鲜活江团，酸汤慢炖',       88.00, 50,  0, 1, 'http://localhost:8080/uploads/goods/g5_江团.png'),
(5, (SELECT id FROM goods_category WHERE shop_id=5 AND sort=2), '秘制卤牛肉',     '苗家独门卤料，鲜香入味',   38.00, 80,  0, 1, 'http://localhost:8080/uploads/goods/g5_牛肉.png'),
(5, (SELECT id FROM goods_category WHERE shop_id=5 AND sort=3), '灰豆腐',         '凯里特产灰碱豆腐',         12.00, 200, 0, 1, 'http://localhost:8080/uploads/shops/shop5_logo.png'),
(5, (SELECT id FROM goods_category WHERE shop_id=5 AND sort=3), '时蔬拼盘',       '当季新鲜时蔬',             18.00, 150, 0, 1, 'http://localhost:8080/uploads/shops/shop5_logo.png'),
(5, (SELECT id FROM goods_category WHERE shop_id=5 AND sort=4), '米饭',           '香喷喷白米饭',              3.00, 999, 0, 1, 'http://localhost:8080/uploads/shops/shop5_logo.png');

-- =============================================
-- 5. 更新已有商品图片
-- =============================================

-- === 老北京烤鸭 (shop_id=1) ===
-- 1=店铺logo  2=黄瓜  3=豆腐  其余用1

UPDATE `goods` SET `image` = 'http://localhost:8080/uploads/goods/g3_黄瓜.png'  WHERE `shop_id` = 1 AND `name` = '凉拌黄瓜';
UPDATE `goods` SET `image` = 'http://localhost:8080/uploads/goods/g4_豆腐.png'  WHERE `shop_id` = 1 AND `name` = '皮蛋豆腐';
-- 其余4个商品统一用烤鸭图
UPDATE `goods` SET `image` = 'http://localhost:8080/uploads/goods/g1_烤鸭.png'  WHERE `shop_id` = 1 AND `image` IS NULL;

-- === 老王麻辣烫 (shop_id=2) ===
-- 1=店铺logo+麻辣烫  2=牛肉  3=土豆片  其余用1

UPDATE `goods` SET `image` = 'http://localhost:8080/uploads/goods/g7_麻辣烫.png' WHERE `shop_id` = 2 AND `name` = '经典麻辣烫';
UPDATE `goods` SET `image` = 'http://localhost:8080/uploads/goods/g7_麻辣烫.png' WHERE `shop_id` = 2 AND `name` = '麻辣拌';
UPDATE `goods` SET `image` = 'http://localhost:8080/uploads/goods/g9_牛肉.png'   WHERE `shop_id` = 2 AND `name` = '肥牛卷';
UPDATE `goods` SET `image` = 'http://localhost:8080/uploads/goods/g10_土豆片.png' WHERE `shop_id` = 2 AND `name` = '土豆片';
-- 米饭也用麻辣烫图
UPDATE `goods` SET `image` = 'http://localhost:8080/uploads/goods/g7_麻辣烫.png' WHERE `shop_id` = 2 AND `image` IS NULL;

-- === 阿强面馆 (shop_id=3) ===
-- 1=店铺  2=牛肉面  3=西红柿鸡蛋面  其余用1

UPDATE `goods` SET `image` = 'http://localhost:8080/uploads/goods/g12_牛肉面.png'        WHERE `shop_id` = 3 AND `name` = '红烧牛肉面';
UPDATE `goods` SET `image` = 'http://localhost:8080/uploads/goods/g13_西红柿鸡蛋面.png'  WHERE `shop_id` = 3 AND `name` = '西红柿鸡蛋面';
-- 其余(兰州拉面+卤蛋+酱牛肉)用牛肉面图
UPDATE `goods` SET `image` = 'http://localhost:8080/uploads/goods/g12_牛肉面.png'        WHERE `shop_id` = 3 AND `image` IS NULL;

-- =============================================
-- 6. 验证结果
-- =============================================
SELECT '=== 店铺Logo ===' AS '';
SELECT id, name, logo FROM shop WHERE id IN (1,2,3,5);

SELECT '=== 商品图片 ===' AS '';
SELECT id, shop_id, name, image FROM goods WHERE shop_id IN (1,2,3,5) ORDER BY shop_id, id;
