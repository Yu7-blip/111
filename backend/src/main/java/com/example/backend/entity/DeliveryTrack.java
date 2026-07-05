package com.example.backend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("delivery_track")
public class DeliveryTrack {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long deliveryId;
    private Long orderId;
    private Double lat;
    private Double lng;
    private Double speed;
    private LocalDateTime reportTime;
    private LocalDateTime createTime;
}
