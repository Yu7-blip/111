package com.example.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.backend.common.PageResult;
import com.example.backend.common.Result;
import com.example.backend.entity.SystemConfig;
import com.example.backend.mapper.SystemConfigMapper;
import com.example.backend.service.SystemConfigService;
import com.example.backend.utils.RedisUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class SystemConfigServiceImpl implements SystemConfigService {

    private final SystemConfigMapper systemConfigMapper;
    private final RedisUtil redisUtil;

    private static final String CACHE_PREFIX = "config:";
    private static final String CACHE_ALL_KEY = "config:all";

    @Override
    public Result<?> list(Integer page, Integer pageSize, String key) {
        LambdaQueryWrapper<SystemConfig> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(key)) {
            wrapper.like(SystemConfig::getConfigKey, key);
        }
        wrapper.orderByAsc(SystemConfig::getId);

        Page<SystemConfig> mpPage = new Page<>(page, pageSize);
        systemConfigMapper.selectPage(mpPage, wrapper);

        return Result.ok(PageResult.of(mpPage.getRecords(), mpPage.getTotal(), mpPage.getCurrent(), mpPage.getSize()));
    }

    @Override
    public Result<?> getByKey(String key) {
        SystemConfig config = systemConfigMapper.selectOne(
                new LambdaQueryWrapper<SystemConfig>().eq(SystemConfig::getConfigKey, key));
        if (config == null) return Result.fail("配置不存在");
        return Result.ok(config);
    }

    @Override
    public Result<?> create(Map<String, Object> data) {
        String configKey = (String) data.get("configKey");
        String configValue = (String) data.get("configValue");
        String description = (String) data.get("description");

        if (!StringUtils.hasText(configKey) || !StringUtils.hasText(configValue)) {
            return Result.fail("配置键和值不能为空");
        }

        // 检查是否已存在
        Long count = systemConfigMapper.selectCount(
                new LambdaQueryWrapper<SystemConfig>().eq(SystemConfig::getConfigKey, configKey));
        if (count != null && count > 0) {
            return Result.fail("该配置键已存在");
        }

        SystemConfig config = new SystemConfig();
        config.setConfigKey(configKey);
        config.setConfigValue(configValue);
        config.setDescription(description);
        config.setCreateTime(LocalDateTime.now());
        config.setUpdateTime(LocalDateTime.now());
        systemConfigMapper.insert(config);

        clearCache();
        return Result.ok(config);
    }

    @Override
    public Result<?> update(Long id, Map<String, Object> data) {
        SystemConfig config = systemConfigMapper.selectById(id);
        if (config == null) return Result.fail("配置不存在");

        if (data.containsKey("configValue")) {
            config.setConfigValue((String) data.get("configValue"));
        }
        if (data.containsKey("description")) {
            config.setDescription((String) data.get("description"));
        }
        config.setUpdateTime(LocalDateTime.now());
        systemConfigMapper.updateById(config);

        clearCache();
        return Result.ok("更新成功");
    }

    @Override
    public Result<?> delete(Long id) {
        SystemConfig config = systemConfigMapper.selectById(id);
        if (config == null) return Result.fail("配置不存在");
        systemConfigMapper.deleteById(id);
        clearCache();
        return Result.ok("删除成功");
    }

    @Override
    public String getConfigValue(String key) {
        // 先从 Redis 取
        String cacheKey = CACHE_PREFIX + key;
        Object cached = redisUtil.get(cacheKey);
        if (cached != null) return cached.toString();

        // 再查 DB
        SystemConfig config = systemConfigMapper.selectOne(
                new LambdaQueryWrapper<SystemConfig>().eq(SystemConfig::getConfigKey, key));
        if (config != null) {
            redisUtil.set(cacheKey, config.getConfigValue(), 10, java.util.concurrent.TimeUnit.MINUTES);
            return config.getConfigValue();
        }
        return null;
    }

    private void clearCache() {
        redisUtil.deleteByPattern(CACHE_PREFIX + "*");
    }
}
