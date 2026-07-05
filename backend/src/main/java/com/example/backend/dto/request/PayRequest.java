package com.example.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PayRequest {
    @NotBlank(message = "支付方式不能为空")
    private String payMethod;
}
