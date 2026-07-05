package com.example.backend.service;

import com.example.backend.common.Result;

public interface StatisticsService {
    Result<?> dashboardStats();
    Result<?> orderTrend();
    Result<?> merchantDashboardStats(Long shopId);
}
