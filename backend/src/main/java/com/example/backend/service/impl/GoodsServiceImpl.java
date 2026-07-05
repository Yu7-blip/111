package com.example.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.backend.common.PageResult;
import com.example.backend.common.Result;
import com.example.backend.dto.request.GoodsSaveRequest;
import com.example.backend.dto.response.GoodsResponse;
import com.example.backend.entity.Goods;
import com.example.backend.entity.GoodsCategory;
import com.example.backend.entity.Shop;
import com.example.backend.mapper.GoodsCategoryMapper;
import com.example.backend.mapper.GoodsMapper;
import com.example.backend.mapper.ShopMapper;
import com.example.backend.service.GoodsService;
import com.example.backend.utils.CacheUtil;
import com.example.backend.utils.RedisUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class GoodsServiceImpl implements GoodsService {

    private final GoodsMapper goodsMapper;
    private final GoodsCategoryMapper goodsCategoryMapper;
    private final ShopMapper shopMapper;
    private final RedisUtil redisUtil;
    private final CacheUtil cacheUtil;

    @Override
    public Result<?> merchantList(Long shopId, Integer page, Integer pageSize, String name, Integer status, String category) {
        LambdaQueryWrapper<Goods> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Goods::getShopId, shopId);

        if (StringUtils.hasText(name)) {
            wrapper.like(Goods::getName, name);
        }
        if (status != null) {
            wrapper.eq(Goods::getStatus, status);
        }
        if (StringUtils.hasText(category)) {
            // 根据分类名称查询分类ID
            LambdaQueryWrapper<GoodsCategory> categoryWrapper = new LambdaQueryWrapper<>();
            categoryWrapper.eq(GoodsCategory::getShopId, shopId)
                    .eq(GoodsCategory::getName, category);
            GoodsCategory goodsCategory = goodsCategoryMapper.selectOne(categoryWrapper);
            if (goodsCategory != null) {
                wrapper.eq(Goods::getCategoryId, goodsCategory.getId());
            }
        }
        wrapper.orderByDesc(Goods::getCreateTime);

        Page<Goods> mpPage = new Page<>(page, pageSize);
        Page<Goods> resultPage = goodsMapper.selectPage(mpPage, wrapper);

        List<GoodsResponse> records = new ArrayList<>();
        for (Goods goods : resultPage.getRecords()) {
            GoodsResponse response = GoodsResponse.builder()
                    .id(goods.getId())
                    .name(goods.getName())
                    .description(goods.getDescription())
                    .richDesc(goods.getRichDesc())
                    .categoryId(goods.getCategoryId())
                    .price(goods.getPrice())
                    .stock(goods.getStock())
                    .sales(goods.getSales())
                    .image(goods.getImage())
                    .status(goods.getStatus())
                    .createTime(goods.getCreateTime())
                    .build();
            if (goods.getCategoryId() != null) {
                GoodsCategory gc = goodsCategoryMapper.selectById(goods.getCategoryId());
                if (gc != null) {
                    response.setCategory(gc.getName());
                }
            }
            records.add(response);
        }

        return Result.ok(PageResult.of(records, resultPage.getTotal(), resultPage.getCurrent(), resultPage.getSize()));
    }

    @Override
    public Result<?> merchantDetail(Long id) {
        Goods goods = goodsMapper.selectById(id);
        if (goods == null) {
            return Result.fail("商品不存在");
        }
        GoodsResponse response = GoodsResponse.builder()
                .id(goods.getId())
                .name(goods.getName())
                .description(goods.getDescription())
                .categoryId(goods.getCategoryId())
                .price(goods.getPrice())
                .stock(goods.getStock())
                .sales(goods.getSales())
                .image(goods.getImage())
                .status(goods.getStatus())
                .createTime(goods.getCreateTime())
                .build();
        if (goods.getCategoryId() != null) {
            GoodsCategory gc = goodsCategoryMapper.selectById(goods.getCategoryId());
            if (gc != null) {
                response.setCategory(gc.getName());
            }
        }
        return Result.ok(response);
    }

    @Override
    public Result<?> merchantCreate(Long shopId, GoodsSaveRequest request) {
        Goods goods = new Goods();
        goods.setShopId(shopId);
        goods.setName(request.getName());
        goods.setCategoryId(request.getCategoryId());
        goods.setDescription(request.getDescription());
        goods.setRichDesc(request.getRichDesc());
        goods.setPrice(request.getPrice());
        goods.setStock(request.getStock());
        goods.setImage(request.getImage());
        goods.setStatus(request.getStatus() != null ? request.getStatus() : 1);
        goods.setSales(0);
        goodsMapper.insert(goods);
        evictGoodsCache(shopId);
        return Result.ok("创建成功");
    }

    @Override
    public Result<?> merchantUpdate(Long id, GoodsSaveRequest request) {
        Goods goods = goodsMapper.selectById(id);
        if (goods == null) {
            return Result.fail("商品不存在");
        }
        goods.setName(request.getName());
        goods.setCategoryId(request.getCategoryId());
        goods.setDescription(request.getDescription());
        goods.setRichDesc(request.getRichDesc());
        goods.setPrice(request.getPrice());
        goods.setStock(request.getStock());
        goods.setImage(request.getImage());
        if (request.getStatus() != null) {
            goods.setStatus(request.getStatus());
        }
        goodsMapper.updateById(goods);
        evictGoodsCache(goods.getShopId());
        return Result.ok("更新成功");
    }

    @Override
    public Result<?> merchantDelete(Long id) {
        Goods goods = goodsMapper.selectById(id);
        if (goods != null) {
            goodsMapper.deleteById(id);
            evictGoodsCache(goods.getShopId());
        }
        return Result.ok("删除成功");
    }

    @Override
    public Result<?> merchantToggleStatus(Long id, Integer status) {
        Goods goods = goodsMapper.selectById(id);
        if (goods == null) {
            return Result.fail("商品不存在");
        }
        goods.setStatus(status);
        goodsMapper.updateById(goods);
        evictGoodsCache(goods.getShopId());
        return Result.ok("操作成功");
    }

    @Override
    public Result<?> wxShopGoods(Long shopId) {
        log.info("wxShopGoods: shopId={}", shopId);
        String cacheKey = "cache:goods:shop:" + shopId;

        // 互斥锁防缓存击穿：热点key过期时只有第一个请求重建，其余等待
        Map<String, Object> result = cacheUtil.getOrLoadWithMutex(
            cacheKey, Map.class,
            () -> {
                Shop shop = shopMapper.selectById(shopId);
                Map<String, Object> shopMap = new HashMap<>();
                if (shop != null) {
                    shopMap.put("id", shop.getId());
                    shopMap.put("name", shop.getName());
                    shopMap.put("minPrice", shop.getMinPrice());
                    shopMap.put("deliveryFee", shop.getDeliveryFee());
                    shopMap.put("notice", shop.getNotice());
                    shopMap.put("rating", shop.getRating());
                    shopMap.put("sales", shop.getSales());
                    shopMap.put("address", shop.getAddress());
                    shopMap.put("latitude", shop.getLatitude());
                    shopMap.put("longitude", shop.getLongitude());
                    shopMap.put("phone", shop.getPhone());
                    shopMap.put("openTime", shop.getOpenTime());
                    shopMap.put("closeTime", shop.getCloseTime());
                    shopMap.put("businessStatus", shop.getBusinessStatus());
                }

                LambdaQueryWrapper<GoodsCategory> categoryWrapper = new LambdaQueryWrapper<>();
                categoryWrapper.eq(GoodsCategory::getShopId, shopId)
                        .orderByAsc(GoodsCategory::getSort);
                List<GoodsCategory> categories = goodsCategoryMapper.selectList(categoryWrapper);

                List<Map<String, Object>> categoryList = new ArrayList<>();
                for (GoodsCategory category : categories) {
                    Map<String, Object> categoryMap = new HashMap<>();
                    categoryMap.put("id", category.getId());
                    categoryMap.put("name", category.getName());

                    LambdaQueryWrapper<Goods> goodsWrapper = new LambdaQueryWrapper<>();
                    goodsWrapper.eq(Goods::getShopId, shopId)
                            .eq(Goods::getCategoryId, category.getId())
                            .eq(Goods::getStatus, 1)
                            .orderByDesc(Goods::getSales);
                    List<Goods> goodsList = goodsMapper.selectList(goodsWrapper);

                    List<Map<String, Object>> foods = new ArrayList<>();
                    for (Goods goods : goodsList) {
                        Map<String, Object> foodMap = new HashMap<>();
                        foodMap.put("id", goods.getId());
                        foodMap.put("name", goods.getName());
                        foodMap.put("desc", goods.getDescription());
                        foodMap.put("price", goods.getPrice());
                        foodMap.put("count", 0);
                        foodMap.put("image", goods.getImage());
                        foodMap.put("sales", goods.getSales());
                        foods.add(foodMap);
                    }
                    categoryMap.put("goods", foods);
                    categoryList.add(categoryMap);
                }

                Map<String, Object> data = new HashMap<>();
                data.put("shop", shopMap);
                data.put("categories", categoryList);
                return data;
            },
            10, TimeUnit.MINUTES
        );

        return Result.ok(result != null ? result : new HashMap<>());
    }

    private void evictGoodsCache(Long shopId) {
        redisUtil.delete("cache:goods:shop:" + shopId);
    }

    @Override
    public Result<?> merchantCategories(Long shopId) {
        LambdaQueryWrapper<GoodsCategory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(GoodsCategory::getShopId, shopId)
                .orderByAsc(GoodsCategory::getSort);
        List<GoodsCategory> list = goodsCategoryMapper.selectList(wrapper);
        // Auto-create default categories for new/existing shops that have none
        if (list.isEmpty()) {
            String[][] defaults = {
                {"热销推荐", "1"}, {"主食", "2"}, {"小食", "3"}, {"饮品", "4"}
            };
            for (String[] cat : defaults) {
                GoodsCategory category = new GoodsCategory();
                category.setShopId(shopId);
                category.setName(cat[0]);
                category.setSort(Integer.parseInt(cat[1]));
                category.setCreateTime(LocalDateTime.now());
                goodsCategoryMapper.insert(category);
                list.add(category);
            }
            log.info("Auto-created default categories for shop {}", shopId);
        }
        return Result.ok(list);
    }
}
