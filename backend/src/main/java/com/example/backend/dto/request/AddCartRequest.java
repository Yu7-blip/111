package com.example.backend.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AddCartRequest {
    @NotNull
    private Long goodsId;
    @NotNull
    @Min(1)
    private Integer count;
}
