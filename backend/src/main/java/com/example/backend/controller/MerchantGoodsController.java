package com.example.backend.controller;

import com.example.backend.common.Result;
import com.example.backend.dto.request.GoodsSaveRequest;
import com.example.backend.entity.GoodsCategory;
import com.example.backend.mapper.GoodsCategoryMapper;
import com.example.backend.mapper.GoodsMapper;
import com.example.backend.service.GoodsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/merchant/goods")
@RequiredArgsConstructor
public class MerchantGoodsController {

    private final GoodsService goodsService;
    private final GoodsCategoryMapper goodsCategoryMapper;
    private final GoodsMapper goodsMapper;

    // ==================== Category CRUD ====================

    @GetMapping("/categories")
    public Result<?> merchantCategories(@RequestAttribute("userId") Long userId) {
        log.info("Merchant categories: shopId={}", userId);
        return goodsService.merchantCategories(userId);
    }

    @PostMapping("/categories")
    public Result<?> createCategory(@RequestAttribute("userId") Long userId,
                                     @RequestBody Map<String, Object> data) {
        GoodsCategory category = new GoodsCategory();
        category.setShopId(userId);
        category.setName((String) data.get("name"));
        Object sort = data.get("sort");
        category.setSort(sort != null ? Integer.parseInt(sort.toString()) : 0);
        category.setCreateTime(LocalDateTime.now());
        goodsCategoryMapper.insert(category);
        return Result.ok(category);
    }

    @PutMapping("/categories/{id}")
    public Result<?> updateCategory(@RequestAttribute("userId") Long userId,
                                     @PathVariable Long id,
                                     @RequestBody Map<String, Object> data) {
        GoodsCategory category = goodsCategoryMapper.selectById(id);
        if (category == null || !category.getShopId().equals(userId)) {
            return Result.fail("分类不存在");
        }
        if (data.containsKey("name")) category.setName((String) data.get("name"));
        if (data.containsKey("sort")) category.setSort(Integer.parseInt(data.get("sort").toString()));
        goodsCategoryMapper.updateById(category);
        return Result.ok(category);
    }

    @DeleteMapping("/categories/{id}")
    public Result<?> deleteCategory(@RequestAttribute("userId") Long userId,
                                     @PathVariable Long id) {
        GoodsCategory category = goodsCategoryMapper.selectById(id);
        if (category == null || !category.getShopId().equals(userId)) {
            return Result.fail("分类不存在");
        }
        // Check if category has goods
        Long count = goodsMapper.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.example.backend.entity.Goods>()
                        .eq(com.example.backend.entity.Goods::getCategoryId, id));
        if (count > 0) {
            return Result.fail("该分类下还有商品，无法删除");
        }
        goodsCategoryMapper.deleteById(id);
        return Result.ok("删除成功");
    }

    @GetMapping
    public Result<?> merchantList(@RequestAttribute("userId") Long userId,
                                  @RequestParam(defaultValue = "1") Integer page,
                                  @RequestParam(defaultValue = "10") Integer pageSize,
                                  @RequestParam(required = false) String name,
                                  @RequestParam(required = false) Integer status,
                                  @RequestParam(required = false) String category) {
        log.info("Merchant goods list: shopId={}, page={}, pageSize={}", userId, page, pageSize);
        return goodsService.merchantList(userId, page, pageSize, name, status, category);
    }

    @GetMapping("/{id}")
    public Result<?> merchantDetail(@PathVariable Long id) {
        log.info("Merchant goods detail: id={}", id);
        return goodsService.merchantDetail(id);
    }

    @PostMapping
    public Result<?> merchantCreate(@RequestAttribute("userId") Long userId,
                                    @Valid @RequestBody GoodsSaveRequest request) {
        log.info("Merchant goods create: shopId={}, name={}", userId, request.getName());
        return goodsService.merchantCreate(userId, request);
    }

    @PutMapping("/{id}")
    public Result<?> merchantUpdate(@PathVariable Long id,
                                    @Valid @RequestBody GoodsSaveRequest request) {
        log.info("Merchant goods update: id={}, name={}", id, request.getName());
        return goodsService.merchantUpdate(id, request);
    }

    @DeleteMapping("/{id}")
    public Result<?> merchantDelete(@PathVariable Long id) {
        log.info("Merchant goods delete: id={}", id);
        return goodsService.merchantDelete(id);
    }

    @PatchMapping("/{id}/status")
    public Result<?> merchantToggleStatus(@PathVariable Long id,
                                          @RequestParam Integer status) {
        log.info("Merchant goods toggle status: id={}, status={}", id, status);
        return goodsService.merchantToggleStatus(id, status);
    }
}
