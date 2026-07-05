package com.example.backend.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class GoodsResponse {
    private Long id;
    private String name;
    private String description;
    private String richDesc;
    private String category;
    private Long categoryId;
    private BigDecimal price;
    private Integer stock;
    private Integer sales;
    private String image;
    private Integer status;
    private LocalDateTime createTime;
}
