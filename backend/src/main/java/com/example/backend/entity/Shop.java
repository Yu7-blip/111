package com.example.backend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("shop")
public class Shop {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String shopNo;
    private String name;
    private String logo;
    private String description;
    private String phone;
    private String email;
    private String address;
    private Double latitude;
    private Double longitude;
    private String openTime;
    private String closeTime;
    private BigDecimal minPrice;
    private BigDecimal deliveryFee;
    private BigDecimal rating;
    private Integer sales;
    private String notice;
    private Integer status;
    private Integer businessStatus;
    @TableField(exist = false)
    private Double distance;
    private String geohash;
    private String auditRemark;
    private String username;
    private String password;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
