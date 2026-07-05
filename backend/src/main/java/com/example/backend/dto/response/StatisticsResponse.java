package com.example.backend.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class StatisticsResponse {
    private Long userCount;
    private Long merchantCount;
    private Long todayOrderCount;
    private Long onlineDeliveryCount;
    private String todayRevenue;
    private Map<String, Object> orderTrend;
}
