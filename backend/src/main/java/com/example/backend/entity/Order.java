package com.example.backend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("`order`")
public class Order {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String orderNo;
    private Long userId;
    private Long shopId;
    private Long deliveryId;
    private String addressInfo;
    private Double addressLat;
    private Double addressLng;
    private String goodsDesc;
    private Integer goodsCount;
    private BigDecimal totalPrice;
    private BigDecimal deliveryFee;
    private BigDecimal packageFee;
    private BigDecimal actualAmount;
    private Integer status;
    private String payMethod;
    private LocalDateTime payTime;
    private String cancelReason;
    private String remark;
    private Integer isLargeOrder;
    private Long parentOrderId;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
