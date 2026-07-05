package com.example.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class GoodsSaveRequest {
    @NotBlank
    private String name;
    private Long categoryId;
    private String description;
    private String richDesc;
    @NotNull
    private BigDecimal price;
    @NotNull
    private Integer stock;
    private String image;
    private Integer status;
}
