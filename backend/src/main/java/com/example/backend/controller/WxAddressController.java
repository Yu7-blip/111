package com.example.backend.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.example.backend.common.BusinessException;
import com.example.backend.common.Result;
import com.example.backend.entity.Address;
import com.example.backend.mapper.AddressMapper;
import com.example.backend.service.TencentMapService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/wx/address")
@RequiredArgsConstructor
@Slf4j
public class WxAddressController {

    private final AddressMapper addressMapper;
    private final TencentMapService tencentMapService;

    @GetMapping
    public Result<List<Address>> list(@RequestAttribute("userId") Long userId) {
        log.info("wx address list: userId={}", userId);
        List<Address> list = addressMapper.selectList(new LambdaQueryWrapper<Address>()
                .eq(Address::getUserId, userId)
                .orderByDesc(Address::getIsDefault));
        return Result.ok(list);
    }

    @PostMapping
    public Result<?> create(@RequestAttribute("userId") Long userId,
                            @RequestBody Map<String, Object> data) {
        log.info("wx address create: userId={}", userId);
        Address address = new Address();
        address.setUserId(userId);
        address.setName((String) data.get("name"));
        address.setPhone((String) data.get("phone"));
        address.setProvince((String) data.get("province"));
        address.setCity((String) data.get("city"));
        address.setDistrict((String) data.get("district"));
        address.setDetail((String) data.get("detail"));
        if (data.get("latitude") != null) {
            address.setLatitude(Double.parseDouble(data.get("latitude").toString()));
        }
        if (data.get("longitude") != null) {
            address.setLongitude(Double.parseDouble(data.get("longitude").toString()));
        }
        Object isDefault = data.get("isDefault");
        int defaultVal = isDefault != null ? Integer.parseInt(isDefault.toString()) : 0;
        address.setIsDefault(defaultVal);
        if (defaultVal == 1) {
            addressMapper.update(null, new LambdaUpdateWrapper<Address>()
                    .eq(Address::getUserId, userId)
                    .eq(Address::getIsDefault, 1)
                    .set(Address::getIsDefault, 0));
        }
        address.setCreateTime(LocalDateTime.now());
        address.setUpdateTime(LocalDateTime.now());
        if (address.getLatitude() == null || address.getLongitude() == null) {
            geocodeAddress(address);
        }
        addressMapper.insert(address);
        return Result.ok(address);
    }

    @PutMapping("/{id}")
    public Result<?> update(@RequestAttribute("userId") Long userId,
                            @PathVariable Long id,
                            @RequestBody Map<String, Object> data) {
        log.info("wx address update: userId={}, id={}", userId, id);
        Address address = addressMapper.selectById(id);
        if (address == null || !address.getUserId().equals(userId)) {
            throw new BusinessException("地址不存在");
        }
        boolean needGeocode = false;
        boolean hasManualCoords = data.containsKey("latitude") && data.get("latitude") != null;
        if (data.containsKey("name")) address.setName((String) data.get("name"));
        if (data.containsKey("phone")) address.setPhone((String) data.get("phone"));
        if (data.containsKey("province")) { address.setProvince((String) data.get("province")); if (!hasManualCoords) needGeocode = true; }
        if (data.containsKey("city")) { address.setCity((String) data.get("city")); if (!hasManualCoords) needGeocode = true; }
        if (data.containsKey("district")) { address.setDistrict((String) data.get("district")); if (!hasManualCoords) needGeocode = true; }
        if (data.containsKey("detail")) { address.setDetail((String) data.get("detail")); if (!hasManualCoords) needGeocode = true; }
        if (hasManualCoords) {
            address.setLatitude(Double.parseDouble(data.get("latitude").toString()));
            address.setLongitude(Double.parseDouble(data.get("longitude").toString()));
        }
        if (data.containsKey("isDefault")) {
            Object isDefault = data.get("isDefault");
            int val = Integer.parseInt(isDefault.toString());
            if (val == 1) {
                // Clear previous default
                addressMapper.update(null, new LambdaUpdateWrapper<Address>()
                        .eq(Address::getUserId, userId)
                        .eq(Address::getIsDefault, 1)
                        .set(Address::getIsDefault, 0));
            }
            address.setIsDefault(val);
        }
        address.setUpdateTime(LocalDateTime.now());
        if (needGeocode) {
            geocodeAddress(address);
        }
        addressMapper.updateById(address);
        return Result.ok(address);
    }

    @DeleteMapping("/{id}")
    public Result<?> delete(@RequestAttribute("userId") Long userId,
                            @PathVariable Long id) {
        log.info("wx address delete: userId={}, id={}", userId, id);
        Address address = addressMapper.selectById(id);
        if (address == null || !address.getUserId().equals(userId)) {
            throw new BusinessException("地址不存在");
        }
        addressMapper.deleteById(id);
        return Result.ok();
    }

    private void geocodeAddress(Address address) {
        String fullAddress = buildFullAddress(address);
        if (fullAddress.isEmpty()) return;
        try {
            double[] coords = tencentMapService.geocode(fullAddress);
            if (coords != null) {
                address.setLongitude(coords[0]);
                address.setLatitude(coords[1]);
            }
        } catch (Exception e) {
            log.warn("Geocode failed for address id={}: {}", address.getId(), e.getMessage());
        }
    }

    private String buildFullAddress(Address address) {
        StringBuilder sb = new StringBuilder();
        if (address.getProvince() != null) sb.append(address.getProvince());
        if (address.getCity() != null) sb.append(address.getCity());
        if (address.getDistrict() != null) sb.append(address.getDistrict());
        if (address.getDetail() != null) sb.append(address.getDetail());
        return sb.toString();
    }
}
