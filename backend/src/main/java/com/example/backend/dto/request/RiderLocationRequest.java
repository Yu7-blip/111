package com.example.backend.dto.request;

import lombok.Data;

import jakarta.validation.constraints.NotNull;

@Data
public class RiderLocationRequest {
    @NotNull
    private Double lat;
    @NotNull
    private Double lng;
    private Double speed;
    private Double bearing;
}
