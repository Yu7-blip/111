package com.example.backend.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class ShopResponse {
    private Long id;
    private String name;
    private String logo;
    private String description;
    private String phone;
    private String email;
    private String address;
    private String openTime;
    private String closeTime;
    private BigDecimal minPrice;
    private BigDecimal deliveryFee;
    private BigDecimal rating;
    private Integer sales;
    private String notice;
    private Integer status;
    private String auditRemark;
    private String createTime;
}
