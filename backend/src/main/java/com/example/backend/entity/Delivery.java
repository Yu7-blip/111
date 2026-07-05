package com.example.backend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("delivery")
public class Delivery {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String name;
    private String phone;
    private String idCard;
    private String vehicle;
    private Integer status;
    private BigDecimal balance;
    private BigDecimal onTimeRate;
    private BigDecimal praiseRate;
    private Integer totalDeliveries;
    private Integer level;
    private Integer verifyStatus;
    private String realName;
    private String verifyRemark;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
