package com.example.backend.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class CartResponse {
    private Long id;
    private Long goodsId;
    private String goodsName;
    private BigDecimal price;
    private String image;
    private Integer count;
    private Long shopId;
    private String shopName;
}
