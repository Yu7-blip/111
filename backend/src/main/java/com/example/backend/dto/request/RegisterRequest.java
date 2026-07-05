package com.example.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RegisterRequest {
    @NotBlank
    private String phone;
    private String code;
    private String password;
    private String nickname;
    private String role;
}
