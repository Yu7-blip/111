package com.example.backend.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 异步事件消息 — 通过 Redis Pub/Sub 传递
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventMessage implements Serializable {

    /** 事件类型 */
    private String type;

    /** 事件数据 */
    private Map<String, Object> data;

    /** 事件时间 */
    private LocalDateTime timestamp;

    // ==================== 事件类型常量 ====================

    /** 支付成功 → 通知骑手、更新销量统计 */
    public static final String ORDER_PAID = "ORDER_PAID";

    /** 订单创建 → 异步清购物车通知 */
    public static final String ORDER_CREATED = "ORDER_CREATED";

    /** 评价提交 → 异步更新评分等级 */
    public static final String EVALUATION_SUBMITTED = "EVALUATION_SUBMITTED";

    /** 退款处理 → 异步通知、恢复库存 */
    public static final String REFUND_PROCESSED = "REFUND_PROCESSED";

    /** 配送完成 → 异步更新骑手统计 */
    public static final String DELIVERY_COMPLETED = "DELIVERY_COMPLETED";
}
