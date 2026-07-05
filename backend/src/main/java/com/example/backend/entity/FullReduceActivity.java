package com.example.backend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("full_reduce_activity")
public class FullReduceActivity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private Long shopId;
    private BigDecimal conditionAmount;
    private BigDecimal reduceAmount;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
