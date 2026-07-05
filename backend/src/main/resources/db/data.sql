-- =============================================
-- 初始数据 (使用 INSERT IGNORE 避免重复执行报错)
-- =============================================

-- 平台管理员 (密码: 123456)
INSERT IGNORE INTO `admin` (`username`, `password`, `name`, `role`, `phone`, `status`) VALUES
('admin',    '123456', '超级管理员', 'admin',    '13800000001', 1),
('operator', '123456', '运营人员',   'operator', '13800000002', 1);

-- C端用户
INSERT IGNORE INTO `user` (`phone`, `nickname`, `password`, `role`, `status`) VALUES
('13900000001', '外卖吃货',    '123456', 'user',     1),
('13900000002', '美食达人',    '123456', 'user',     1),
('13900000003', '金牌骑手',    '123456', 'delivery', 1),
('13900000004', '极速配送',    '123456', 'delivery', 1),
('13900000005', '张三',        '123456', 'user',     0);

-- 骑手信息
INSERT IGNORE INTO `delivery` (`user_id`, `name`, `phone`, `id_card`, `vehicle`, `status`, `balance`) VALUES
(3, '金牌骑手', '13900000003', '110101199001011234', '电动车', 1, 1580.50),
(4, '极速配送', '13900000004', '110101199505052345', '摩托车', 0, 890.00);

-- 商家/店铺 (密码: 123456)
INSERT IGNORE INTO `shop` (`name`, `logo`, `description`, `phone`, `email`, `address`, `latitude`, `longitude`, `open_time`, `close_time`, `min_price`, `delivery_fee`, `rating`, `sales`, `notice`, `status`, `username`, `password`) VALUES
('老北京烤鸭（高新店）', NULL, '正宗老北京烤鸭，百年传承手艺', '0851-88880001', 'kaoya@shop.com', '贵阳市观山湖区高新路88号', 26.6470, 106.6300, '09:00', '22:00', 20.00, 5.00, 5.0, 0, NULL, 1, 'shop1', '123456'),
('老王麻辣烫',             NULL, '地道重庆麻辣烫，鲜香麻辣',     '0851-88880002', 'malatang@shop.com', '贵阳市云岩区科技路12号',  26.6520, 106.6450, '10:00', '23:00', 15.00, 3.00, 5.0, 0,  NULL, 1, 'shop2', '123456'),
('阿强面馆',               NULL, '手工拉面，劲道十足',           '0851-88880003', 'mian@shop.com',    '贵阳市南明区创新大道56号', 26.6350, 106.6180, '08:00', '21:00', 12.00, 2.00, 5.0, 0, NULL, 1, 'shop3', '123456'),
('待审核商家',              NULL, '这是一家待审核的商家',         '0851-88880004', 'new@shop.com',     '贵阳市花溪区中关村1号',    26.6100, 106.6000, '09:00', '21:00', 20.00, 4.00, 0,    0,    NULL, 0, 'shop4', '123456');

-- 商品分类
INSERT IGNORE INTO `goods_category` (`shop_id`, `name`, `sort`) VALUES
(1, '热销推荐', 1),
(1, '精美凉菜', 2),
(1, '烤鸭套餐', 3),
(1, '饮品',     4),
(2, '热销推荐', 1),
(2, '荤菜',     2),
(2, '素菜',     3),
(2, '主食',     4),
(3, '热销推荐', 1),
(3, '拉面',     2),
(3, '小食',     3);

-- 商品
INSERT IGNORE INTO `goods` (`shop_id`, `category_id`, `name`, `description`, `price`, `stock`, `sales`, `status`) VALUES
-- 老北京烤鸭
(1, 1, '招牌北京烤鸭半只', '秘制酱料，外酥里嫩', 68.00, 50, 0, 1),
(1, 1, '烤鸭整只套装',     '整只烤鸭+饼+葱丝+酱料', 128.00, 30, 0, 1),
(1, 2, '凉拌黄瓜',         '清脆爽口', 12.00, 100, 0, 1),
(1, 2, '皮蛋豆腐',         '经典凉菜', 15.00, 80, 0, 1),
(1, 3, '双人烤鸭套餐',     '半只鸭+2份饼+2份小菜', 98.00, 40, 0, 1),
(1, 4, '酸梅汤',           '冰镇酸梅汤', 8.00, 200, 0, 1),
-- 老王麻辣烫
(2, 5, '经典麻辣烫',       '骨汤底料，麻辣鲜香', 22.00, 200, 0, 1),
(2, 5, '麻辣拌',           '干拌麻辣风味', 20.00, 150, 0, 1),
(2, 6, '肥牛卷',           '单点肥牛卷', 15.00, 300, 0, 1),
(2, 7, '土豆片',           '新鲜土豆', 5.00, 500, 0, 1),
(2, 8, '米饭',             '香喷喷白米饭', 3.00, 999, 0, 1),
-- 阿强面馆
(3, 9, '红烧牛肉面',       '大块牛肉，秘制汤底', 28.00, 80, 0, 1),
(3, 9, '西红柿鸡蛋面',     '酸甜可口，老少皆宜', 18.00, 100, 0, 1),
(3, 10, '兰州拉面',        '手工拉制，汤清味浓', 22.00, 120, 0, 1),
(3, 11, '卤蛋',            '卤制土鸡蛋', 3.00, 300, 0, 1),
(3, 11, '酱牛肉',          '秘制卤牛肉100g', 18.00, 60, 0, 0);

-- 优惠券 (seed data - 检查名称避免重复播种)
INSERT INTO `coupon` (`name`, `condition_amount`, `reduce_amount`, `total_count`, `remain_count`, `start_time`, `end_time`, `status`)
SELECT t.name, t.condition_amount, t.reduce_amount, t.total_count, t.remain_count, t.start_time, t.end_time, t.status
FROM (
    SELECT '新人满30减5' AS name, 30.00 AS condition_amount, 5.00 AS reduce_amount, 1000 AS total_count, 850 AS remain_count, '2026-01-01 00:00:00' AS start_time, '2026-12-31 23:59:59' AS end_time, 1 AS status
    UNION ALL
    SELECT '满50减10', 50.00, 10.00, 500, 320, '2026-01-01 00:00:00', '2026-12-31 23:59:59', 1
    UNION ALL
    SELECT '满100减25', 100.00, 25.00, 200, 150, '2026-05-01 00:00:00', '2026-06-30 23:59:59', 1
) t
WHERE NOT EXISTS (SELECT 1 FROM `coupon` c WHERE c.name = t.name);

-- 满减活动
INSERT IGNORE INTO `full_reduce_activity` (`name`, `shop_id`, `condition_amount`, `reduce_amount`, `start_time`, `end_time`, `status`) VALUES
('全平台满50减10',  NULL, 50.00,  10.00, '2026-05-01 00:00:00', '2026-06-30 23:59:59', 1),
('烤鸭店满100减20', 1,    100.00, 20.00, '2026-05-10 00:00:00', '2026-05-31 23:59:59', 1),
('冬日暖心满30减8',  NULL, 30.00,  8.00,  '2026-12-01 00:00:00', '2026-12-31 23:59:59', 0);

-- 用户收货地址 (含经纬度，确保骑手能正确导航)
INSERT IGNORE INTO `address` (`user_id`, `name`, `phone`, `province`, `city`, `district`, `detail`, `latitude`, `longitude`, `is_default`) VALUES
(1, '小明', '13900000001', '贵州省', '贵阳市', '观山湖区', '高新路88号创新大厦A座12层', 26.6485, 106.6320, 1),
(2, '小红', '13900000002', '贵州省', '贵阳市', '南明区', '中关村大街1号科技园B座5层', 26.6365, 106.6200, 1);
