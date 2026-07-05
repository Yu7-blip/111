package com.example.backend.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.backend.common.PageResult;
import com.example.backend.common.Result;
import com.example.backend.entity.Goods;
import com.example.backend.entity.GoodsCategory;
import com.example.backend.entity.Shop;
import com.example.backend.mapper.GoodsCategoryMapper;
import com.example.backend.mapper.GoodsMapper;
import com.example.backend.mapper.ShopMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;

@Slf4j
@RestController
@RequestMapping("/api/admin/goods")
@RequiredArgsConstructor
public class AdminGoodsController {

    private final GoodsMapper goodsMapper;
    private final ShopMapper shopMapper;
    private final GoodsCategoryMapper goodsCategoryMapper;

    @GetMapping
    public Result<?> adminList(@RequestParam(defaultValue = "1") Integer page,
                               @RequestParam(defaultValue = "10") Integer pageSize,
                               @RequestParam(required = false) String name) {
        LambdaQueryWrapper<Goods> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(name)) {
            wrapper.like(Goods::getName, name);
        }
        wrapper.orderByDesc(Goods::getCreateTime);

        Page<Goods> mpPage = new Page<>(page, pageSize);
        goodsMapper.selectPage(mpPage, wrapper);

        // Collect shop IDs to batch-fetch shop names
        Set<Long> shopIds = new HashSet<>();
        Set<Long> catIds = new HashSet<>();
        for (Goods g : mpPage.getRecords()) {
            if (g.getShopId() != null) shopIds.add(g.getShopId());
            if (g.getCategoryId() != null) catIds.add(g.getCategoryId());
        }

        Map<Long, String> shopNameMap = new HashMap<>();
        for (Long sid : shopIds) {
            Shop s = shopMapper.selectById(sid);
            shopNameMap.put(sid, s != null ? s.getName() : "未知店铺");
        }
        Map<Long, String> catNameMap = new HashMap<>();
        for (Long cid : catIds) {
            GoodsCategory gc = goodsCategoryMapper.selectById(cid);
            catNameMap.put(cid, gc != null ? gc.getName() : "未分类");
        }

        List<Map<String, Object>> records = mpPage.getRecords().stream().map(g -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", g.getId());
            map.put("name", g.getName());
            map.put("shopId", g.getShopId());
            map.put("shopName", shopNameMap.getOrDefault(g.getShopId(), ""));
            map.put("categoryId", g.getCategoryId());
            map.put("categoryName", catNameMap.getOrDefault(g.getCategoryId(), ""));
            map.put("price", g.getPrice());
            map.put("stock", g.getStock());
            map.put("sales", g.getSales());
            map.put("status", g.getStatus());
            map.put("createTime", g.getCreateTime());
            return map;
        }).toList();

        return Result.ok(PageResult.of(records, mpPage.getTotal(), mpPage.getCurrent(), mpPage.getSize()));
    }
}
