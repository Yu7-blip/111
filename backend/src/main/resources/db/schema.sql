-- =============================================
-- 外卖配送平台 数据库初始化脚本
-- =============================================

CREATE DATABASE IF NOT EXISTS delivery_platform
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE delivery_platform;

-- =============================================
-- 1. C端用户表（小程序用户/骑手）
-- =============================================
CREATE TABLE IF NOT EXISTS `user` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '用户ID',
    `phone`       VARCHAR(20)  NOT NULL COMMENT '手机号',
    `nickname`    VARCHAR(50)  DEFAULT NULL COMMENT '昵称',
    `avatar`      VARCHAR(255) DEFAULT NULL COMMENT '头像URL',
    `password`    VARCHAR(255) DEFAULT NULL COMMENT '登录密码',
    `role`        VARCHAR(20)  NOT NULL DEFAULT 'user' COMMENT '角色: user-用户, delivery-骑手',
    `status`      TINYINT      NOT NULL DEFAULT 1 COMMENT '状态: 0-禁用, 1-正常',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_phone_role` (`phone`, `role`),
    KEY `idx_role` (`role`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- =============================================
-- 2. 平台管理员表
-- =============================================
CREATE TABLE IF NOT EXISTS `admin` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '管理员ID',
    `username`    VARCHAR(50)  NOT NULL COMMENT '用户名',
    `password`    VARCHAR(255) NOT NULL COMMENT '密码(BCrypt)',
    `name`        VARCHAR(50)  DEFAULT NULL COMMENT '姓名',
    `role`        VARCHAR(20)  NOT NULL DEFAULT 'operator' COMMENT '角色: admin-超级管理员, operator-运营人员',
    `avatar`      VARCHAR(255) DEFAULT NULL COMMENT '头像URL',
    `phone`       VARCHAR(20)  DEFAULT NULL COMMENT '手机号',
    `status`      TINYINT      NOT NULL DEFAULT 1 COMMENT '状态: 0-禁用, 1-正常',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='管理员表';

-- =============================================
-- 3. 商家/店铺表
-- =============================================
CREATE TABLE IF NOT EXISTS `shop` (
    `id`           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '店铺ID',
    `name`         VARCHAR(100) NOT NULL COMMENT '店铺名称',
    `logo`         VARCHAR(255) DEFAULT NULL COMMENT '店铺Logo',
    `description`  VARCHAR(500) DEFAULT NULL COMMENT '店铺简介',
    `phone`        VARCHAR(20)  DEFAULT NULL COMMENT '联系电话',
    `email`        VARCHAR(100) DEFAULT NULL COMMENT '联系邮箱',
    `address`      VARCHAR(255) DEFAULT NULL COMMENT '店铺地址',
    `open_time`    VARCHAR(10)  DEFAULT '09:00' COMMENT '营业开始时间',
    `close_time`   VARCHAR(10)  DEFAULT '22:00' COMMENT '营业结束时间',
    `min_price`    DECIMAL(10,2) DEFAULT 0.00 COMMENT '最低起送价',
    `delivery_fee` DECIMAL(10,2) DEFAULT 0.00 COMMENT '配送费',
    `rating`       DECIMAL(2,1)  DEFAULT 5.0 COMMENT '评分(0-5)',
    `sales`        INT           DEFAULT 0 COMMENT '月销量',
    `notice`       VARCHAR(255)  DEFAULT NULL COMMENT '店铺公告',
    `status`       TINYINT      NOT NULL DEFAULT 0 COMMENT '状态: 0-待审核, 1-已通过, 2-已拒绝',
    `audit_remark` VARCHAR(500) DEFAULT NULL COMMENT '审核备注',
    `username`     VARCHAR(50)  DEFAULT NULL COMMENT '商家登录用户名',
    `password`     VARCHAR(255) DEFAULT NULL COMMENT '商家登录密码(BCrypt)',
    `create_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='店铺表';

-- =============================================
-- 4. 商品分类表
-- =============================================
CREATE TABLE IF NOT EXISTS `goods_category` (
    `id`          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '分类ID',
    `shop_id`     BIGINT      NOT NULL COMMENT '店铺ID',
    `name`        VARCHAR(50) NOT NULL COMMENT '分类名称',
    `sort`        INT         DEFAULT 0 COMMENT '排序',
    `create_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_shop_id` (`shop_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品分类表';

-- =============================================
-- 5. 商品表
-- =============================================
CREATE TABLE IF NOT EXISTS `goods` (
    `id`          BIGINT         NOT NULL AUTO_INCREMENT COMMENT '商品ID',
    `shop_id`     BIGINT         NOT NULL COMMENT '店铺ID',
    `category_id` BIGINT         DEFAULT NULL COMMENT '分类ID',
    `name`        VARCHAR(100)   NOT NULL COMMENT '商品名称',
    `description` VARCHAR(500)   DEFAULT NULL COMMENT '商品描述',
    `price`       DECIMAL(10,2)  NOT NULL COMMENT '价格',
    `stock`       INT            DEFAULT 0 COMMENT '库存',
    `sales`       INT            DEFAULT 0 COMMENT '销量',
    `image`       VARCHAR(255)   DEFAULT NULL COMMENT '图片URL',
    `status`      TINYINT        NOT NULL DEFAULT 1 COMMENT '状态: 0-下架, 1-上架',
    `create_time` DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_shop_id` (`shop_id`),
    KEY `idx_category_id` (`category_id`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品表';

-- =============================================
-- 6. 购物车表
-- =============================================
CREATE TABLE IF NOT EXISTS `cart` (
    `id`          BIGINT   NOT NULL AUTO_INCREMENT COMMENT '购物车ID',
    `user_id`     BIGINT   NOT NULL COMMENT '用户ID',
    `goods_id`    BIGINT   NOT NULL COMMENT '商品ID',
    `count`       INT      NOT NULL DEFAULT 1 COMMENT '数量',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_goods` (`user_id`, `goods_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='购物车表';

-- =============================================
-- 7. 订单表
-- =============================================
CREATE TABLE IF NOT EXISTS `order` (
    `id`            BIGINT         NOT NULL AUTO_INCREMENT COMMENT '订单ID',
    `order_no`      VARCHAR(32)    NOT NULL COMMENT '订单编号',
    `user_id`       BIGINT         NOT NULL COMMENT '用户ID',
    `shop_id`       BIGINT         NOT NULL COMMENT '店铺ID',
    `delivery_id`   BIGINT         DEFAULT NULL COMMENT '骑手ID',
    `address_info`  VARCHAR(500)   DEFAULT NULL COMMENT '收货地址JSON',
    `goods_desc`    VARCHAR(255)   DEFAULT NULL COMMENT '商品概要描述',
    `goods_count`   INT            DEFAULT 0 COMMENT '商品总数量',
    `total_price`   DECIMAL(10,2)  NOT NULL COMMENT '订单总金额',
    `delivery_fee`  DECIMAL(10,2)  DEFAULT 0.00 COMMENT '配送费',
    `package_fee`   DECIMAL(10,2)  DEFAULT 0.00 COMMENT '包装费',
    `actual_amount` DECIMAL(10,2)  DEFAULT NULL COMMENT '实付金额（优惠后）',
    `status`        TINYINT        NOT NULL DEFAULT 0 COMMENT '订单状态: 0-待支付, 1-已支付, 2-配送中, 3-已完成, 4-已取消, 5-退款中, 6-已退款',
    `pay_method`    VARCHAR(20)    DEFAULT NULL COMMENT '支付方式',
    `pay_time`      DATETIME       DEFAULT NULL COMMENT '支付时间',
    `cancel_reason` VARCHAR(255)   DEFAULT NULL COMMENT '取消原因',
    `remark`        VARCHAR(500)   DEFAULT NULL COMMENT '用户备注',
    `create_time`   DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`   DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_order_no` (`order_no`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_shop_id` (`shop_id`),
    KEY `idx_delivery_id` (`delivery_id`),
    KEY `idx_status` (`status`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单表';

-- =============================================
-- 8. 订单明细表
-- =============================================
CREATE TABLE IF NOT EXISTS `order_item` (
    `id`          BIGINT         NOT NULL AUTO_INCREMENT COMMENT '明细ID',
    `order_id`    BIGINT         NOT NULL COMMENT '订单ID',
    `goods_id`    BIGINT         NOT NULL COMMENT '商品ID',
    `goods_name`  VARCHAR(100)   DEFAULT NULL COMMENT '商品名称快照',
    `goods_price` DECIMAL(10,2)  DEFAULT NULL COMMENT '商品单价快照',
    `count`       INT            NOT NULL DEFAULT 1 COMMENT '数量',
    `create_time` DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_order_id` (`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单明细表';

-- =============================================
-- 9. 骑手表
-- =============================================
CREATE TABLE IF NOT EXISTS `delivery` (
    `id`               BIGINT         NOT NULL AUTO_INCREMENT COMMENT '骑手ID',
    `user_id`          BIGINT         NOT NULL COMMENT '关联用户ID',
    `name`             VARCHAR(50)    NOT NULL COMMENT '姓名',
    `phone`            VARCHAR(20)    NOT NULL COMMENT '手机号',
    `id_card`          VARCHAR(18)    DEFAULT NULL COMMENT '身份证号',
    `vehicle`          VARCHAR(20)    DEFAULT NULL COMMENT '交通工具',
    `status`           TINYINT        NOT NULL DEFAULT 0 COMMENT '状态: 0-离线, 1-在线, 2-忙碌',
    `balance`          DECIMAL(10,2)  DEFAULT 0.00 COMMENT '账户余额',
    `on_time_rate`     DECIMAL(5,1)   DEFAULT 100.0 COMMENT '准时率(%)',
    `praise_rate`      DECIMAL(5,1)   DEFAULT 100.0 COMMENT '好评率(%)',
    `total_deliveries` INT            DEFAULT 0 COMMENT '总配送单数',
    `create_time`      DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`      DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_id` (`user_id`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='骑手表';

-- =============================================
-- 10. 配送记录表
-- =============================================
CREATE TABLE IF NOT EXISTS `delivery_record` (
    `id`            BIGINT         NOT NULL AUTO_INCREMENT COMMENT '记录ID',
    `order_id`      BIGINT         NOT NULL COMMENT '订单ID',
    `delivery_id`   BIGINT         NOT NULL COMMENT '骑手ID',
    `fee`           DECIMAL(10,2)  DEFAULT 0.00 COMMENT '配送收入',
    `status`        VARCHAR(20)    NOT NULL DEFAULT 'pickup' COMMENT '配送状态: pickup-待取餐, delivering-配送中, completed-已完成',
    `pickup_time`   DATETIME       DEFAULT NULL COMMENT '取餐时间',
    `deliver_time`  DATETIME       DEFAULT NULL COMMENT '送达时间',
    `create_time`   DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_order_id` (`order_id`),
    KEY `idx_delivery_id` (`delivery_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='配送记录表';

-- =============================================
-- 11. 评价表
-- =============================================
CREATE TABLE IF NOT EXISTS `evaluation` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '评价ID',
    `order_id`    BIGINT       NOT NULL COMMENT '订单ID',
    `user_id`     BIGINT       NOT NULL COMMENT '用户ID',
    `delivery_id` BIGINT       DEFAULT NULL COMMENT '骑手ID',
    `rating`      TINYINT      NOT NULL COMMENT '评分(1-5)',
    `content`     VARCHAR(500) DEFAULT NULL COMMENT '评价内容',
    `status`      TINYINT      NOT NULL DEFAULT 0 COMMENT '状态: 0-正常, 1-已撤销',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_order_id` (`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='评价表';

-- =============================================
-- 12. 优惠券模板表
-- =============================================
CREATE TABLE IF NOT EXISTS `coupon` (
    `id`              BIGINT         NOT NULL AUTO_INCREMENT COMMENT '优惠券ID',
    `name`            VARCHAR(100)   NOT NULL COMMENT '优惠券名称',
    `condition_amount` DECIMAL(10,2) NOT NULL COMMENT '使用门槛金额',
    `reduce_amount`   DECIMAL(10,2)  NOT NULL COMMENT '减免金额',
    `total_count`     INT            DEFAULT 0 COMMENT '发放总量',
    `remain_count`    INT            DEFAULT 0 COMMENT '剩余数量',
    `start_time`      DATETIME       DEFAULT NULL COMMENT '开始时间',
    `end_time`        DATETIME       DEFAULT NULL COMMENT '结束时间',
    `status`          TINYINT        NOT NULL DEFAULT 1 COMMENT '状态: 0-禁用, 1-启用',
    `shop_id`         BIGINT         DEFAULT NULL COMMENT '店铺ID(NULL=平台券)',
    `create_time`     DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='优惠券表';

-- =============================================
-- 13. 用户优惠券表
-- =============================================
CREATE TABLE IF NOT EXISTS `user_coupon` (
    `id`          BIGINT   NOT NULL AUTO_INCREMENT COMMENT '记录ID',
    `user_id`     BIGINT   NOT NULL COMMENT '用户ID',
    `coupon_id`   BIGINT   NOT NULL COMMENT '优惠券ID',
    `status`      TINYINT  NOT NULL DEFAULT 0 COMMENT '状态: 0-未使用, 1-已使用, 2-已过期',
    `use_time`    DATETIME DEFAULT NULL COMMENT '使用时间',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '领取时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户优惠券表';

-- =============================================
-- 14. 满减活动表
-- =============================================
CREATE TABLE IF NOT EXISTS `full_reduce_activity` (
    `id`               BIGINT         NOT NULL AUTO_INCREMENT COMMENT '活动ID',
    `name`             VARCHAR(100)   NOT NULL COMMENT '活动名称',
    `shop_id`          BIGINT         DEFAULT NULL COMMENT '店铺ID(NULL表示全平台活动)',
    `condition_amount` DECIMAL(10,2)  NOT NULL COMMENT '满减门槛金额',
    `reduce_amount`    DECIMAL(10,2)  NOT NULL COMMENT '减免金额',
    `start_time`       DATETIME       NOT NULL COMMENT '开始时间',
    `end_time`         DATETIME       NOT NULL COMMENT '结束时间',
    `status`           TINYINT        NOT NULL DEFAULT 0 COMMENT '状态: 0-未开始, 1-进行中, 2-已结束',
    `create_time`      DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`      DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_status` (`status`),
    KEY `idx_time` (`start_time`, `end_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='满减活动表';

-- =============================================
-- 15. 用户收货地址表
-- =============================================
CREATE TABLE IF NOT EXISTS `address` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '地址ID',
    `user_id`     BIGINT       NOT NULL COMMENT '用户ID',
    `name`        VARCHAR(50)  NOT NULL COMMENT '收货人姓名',
    `phone`       VARCHAR(20)  NOT NULL COMMENT '收货人手机号',
    `province`    VARCHAR(50)  DEFAULT NULL COMMENT '省份',
    `city`        VARCHAR(50)  DEFAULT NULL COMMENT '城市',
    `district`    VARCHAR(50)  DEFAULT NULL COMMENT '区县',
    `detail`      VARCHAR(255) NOT NULL COMMENT '详细地址',
    `is_default`  TINYINT      DEFAULT 0 COMMENT '是否默认: 0-否, 1-是',
    `latitude`    DOUBLE       DEFAULT NULL COMMENT '纬度',
    `longitude`   DOUBLE       DEFAULT NULL COMMENT '经度',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户收货地址表';

-- =============================================
-- 增量变更（老数据库升级用，每条失败不影响启动）
-- =============================================
ALTER TABLE `user`     ADD COLUMN `password` VARCHAR(255) DEFAULT NULL COMMENT '登录密码';
ALTER TABLE `shop`     ADD COLUMN `business_status` TINYINT NOT NULL DEFAULT 1 COMMENT '营业状态: 0-休息中, 1-营业中';
ALTER TABLE `delivery` ADD COLUMN `level` TINYINT NOT NULL DEFAULT 0 COMMENT '等级: 0-铜牌, 1-银牌, 2-金牌';
ALTER TABLE `shop`     ADD COLUMN `latitude` DOUBLE DEFAULT NULL COMMENT '纬度';
ALTER TABLE `shop`     ADD COLUMN `longitude` DOUBLE DEFAULT NULL COMMENT '经度';
ALTER TABLE `address`  ADD COLUMN `latitude` DOUBLE DEFAULT NULL COMMENT '纬度';
ALTER TABLE `address`  ADD COLUMN `longitude` DOUBLE DEFAULT NULL COMMENT '经度';
ALTER TABLE `order`    ADD COLUMN `address_lat` DOUBLE DEFAULT NULL COMMENT '收货地址纬度';
ALTER TABLE `order`    ADD COLUMN `address_lng` DOUBLE DEFAULT NULL COMMENT '收货地址经度';
ALTER TABLE `coupon`   ADD COLUMN `shop_id` BIGINT DEFAULT NULL COMMENT '店铺ID(NULL=平台券)';
ALTER TABLE `user`     DROP INDEX `uk_phone`;
ALTER TABLE `user`     ADD UNIQUE KEY `uk_phone_role` (`phone`, `role`);

ALTER TABLE `shop`     ADD COLUMN `geohash` VARCHAR(12) DEFAULT NULL COMMENT 'GeoHash编码(7位精度~150m)';
ALTER TABLE `shop`     ADD INDEX `idx_geohash` (`geohash`(6));
ALTER TABLE `goods`    ADD COLUMN `rich_desc` TEXT DEFAULT NULL COMMENT '富文本描述';
ALTER TABLE `delivery` ADD COLUMN `verify_status` TINYINT NOT NULL DEFAULT 0 COMMENT '实名认证: 0-未认证, 1-审核中, 2-已认证, 3-驳回';
ALTER TABLE `delivery` ADD COLUMN `real_name` VARCHAR(50) DEFAULT NULL COMMENT '真实姓名';
ALTER TABLE `delivery` ADD COLUMN `verify_remark` VARCHAR(255) DEFAULT NULL COMMENT '认证备注';
ALTER TABLE `order`    ADD COLUMN `is_large_order` TINYINT NOT NULL DEFAULT 0 COMMENT '是否大订单(需多骑手)';
ALTER TABLE `order`    ADD COLUMN `parent_order_id` BIGINT DEFAULT NULL COMMENT '父订单ID(拆单场景)';

CREATE TABLE IF NOT EXISTS `delivery_track` (
    `id`          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '轨迹ID',
    `delivery_id` BIGINT      NOT NULL COMMENT '骑手ID',
    `order_id`    BIGINT      DEFAULT NULL COMMENT '关联订单ID',
    `lat`         DOUBLE      NOT NULL COMMENT '纬度',
    `lng`         DOUBLE      NOT NULL COMMENT '经度',
    `speed`       DOUBLE      DEFAULT NULL COMMENT '速度(m/s)',
    `report_time` DATETIME    NOT NULL COMMENT '上报时间',
    `create_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_delivery_time` (`delivery_id`, `report_time`),
    KEY `idx_order_id` (`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='骑手配送轨迹表';

CREATE TABLE IF NOT EXISTS `withdraw` (
    `id`          BIGINT         NOT NULL AUTO_INCREMENT COMMENT '提现ID',
    `delivery_id` BIGINT         NOT NULL COMMENT '骑手ID',
    `user_id`     BIGINT         NOT NULL COMMENT '用户ID',
    `amount`      DECIMAL(10,2)  NOT NULL COMMENT '提现金额',
    `status`      TINYINT        NOT NULL DEFAULT 0 COMMENT '状态: 0-待处理, 1-已处理, 2-已拒绝',
    `remark`      VARCHAR(500)   DEFAULT NULL COMMENT '处理备注',
    `create_time` DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_delivery_id` (`delivery_id`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='提现申请表';

-- =============================================
-- 16. 本地事件表（分布式事务 — Transaction Outbox 模式）
-- =============================================
CREATE TABLE IF NOT EXISTS `event_log` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '事件ID',
    `event_type`  VARCHAR(50)  NOT NULL COMMENT '事件类型',
    `payload`     JSON         NOT NULL COMMENT '事件载荷',
    `status`      TINYINT      NOT NULL DEFAULT 0 COMMENT '状态: 0-待处理, 1-已处理, 2-失败',
    `retry_count` INT          DEFAULT 0 COMMENT '重试次数',
    `error_msg`   VARCHAR(500) DEFAULT NULL COMMENT '最后错误信息',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_status_create` (`status`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='本地事件表';

-- =============================================
-- 17. 用户反馈/投诉表
-- =============================================
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
