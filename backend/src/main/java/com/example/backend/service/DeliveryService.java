package com.example.backend.service;

import com.example.backend.common.Result;

public interface DeliveryService {
    Result<?> adminList(Integer page, Integer pageSize, String name, String phone, Integer status);
    Result<?> adminDetail(Long id);
    Result<?> adminUpdateStatus(Long id, Integer status);
    Result<?> wxLobby(Long userId, Double lat, Double lng);
    Result<?> wxGrab(Long userId, Long orderId);
    Result<?> wxTasks(Long userId, Double lat, Double lng);
    Result<?> wxUpdateTaskStatus(Long userId, Long recordId, String status);
    Result<?> wxIncome(Long userId);
    Result<?> wxUpdateStatus(Long userId, Integer status);
    Result<?> wxProfile(Long userId);
    Result<?> reportLocation(Long userId, Double lat, Double lng);
    Result<?> getRiderLocation(Long deliveryId);
    Result<?> getRiderLocationByOrder(Long orderId);
    Result<?> getTrackPoints(Long orderId);
    Result<?> applyVerification(Long userId, String realName, String idCard);
    Result<?> getVerificationStatus(Long userId);
    Result<?> reviewVerification(Long deliveryId, Integer verifyStatus, String remark);
    Result<?> updateVehicle(Long userId, String vehicle);
}
