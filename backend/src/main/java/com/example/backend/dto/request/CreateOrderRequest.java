package com.example.backend.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class CreateOrderRequest {
    @NotNull
    private Long addressId;
    @NotNull
    private Long shopId;
    private List<OrderItemRequest> items;
    private String remark;
    private Long couponId;

    @Data
    public static class OrderItemRequest {
        @NotNull
        private Long goodsId;
        @NotNull
        private Integer count;
    }
}
