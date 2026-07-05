package com.example.backend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("delivery_record")
public class DeliveryRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long orderId;
    private Long deliveryId;
    private BigDecimal fee;
    private String status;
    private LocalDateTime pickupTime;
    private LocalDateTime deliverTime;
    private LocalDateTime createTime;
}
