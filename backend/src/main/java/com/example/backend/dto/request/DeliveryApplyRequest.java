package com.example.backend.dto.request;

import lombok.Data;

@Data
public class DeliveryApplyRequest {
    private String name;
    private String idCard;
    private String vehicle;
}
