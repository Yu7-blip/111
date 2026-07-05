package com.example.backend.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class OrderResponse {
    private Long id;
    private String orderNo;
    private Long userId;
    private String username;
    private Long shopId;
    private String shopName;
    private Long deliveryId;
    private String deliveryName;
    private String addressInfo;
    private String goodsDesc;
    private Integer goodsCount;
    private BigDecimal totalPrice;
    private BigDecimal deliveryFee;
    private BigDecimal packageFee;
    private BigDecimal actualAmount;
    private Integer status;
    private String payMethod;
    private LocalDateTime payTime;
    private String remark;
    private LocalDateTime createTime;
    private List<OrderItemResponse> items;

    @Data
    @Builder
    public static class OrderItemResponse {
        private Long id;
        private String goodsName;
        private BigDecimal goodsPrice;
        private Integer count;
    }
}
