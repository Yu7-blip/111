package com.example.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ShopRegisterRequest {
    @NotBlank
    private String name;
    private String phone;
    private String email;
    private String address;
    private String description;
    @NotBlank
    private String username;
    @NotBlank
    private String password;
}
