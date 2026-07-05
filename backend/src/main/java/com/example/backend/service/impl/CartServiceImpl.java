package com.example.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.backend.common.Result;
import com.example.backend.common.ResultCode;
import com.example.backend.dto.request.AddCartRequest;
import com.example.backend.dto.response.CartResponse;
import com.example.backend.entity.Cart;
import com.example.backend.entity.Goods;
import com.example.backend.entity.Shop;
import com.example.backend.mapper.CartMapper;
import com.example.backend.mapper.GoodsMapper;
import com.example.backend.mapper.ShopMapper;
import com.example.backend.service.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {

    private final CartMapper cartMapper;
    private final GoodsMapper goodsMapper;
    private final ShopMapper shopMapper;

    @Override
    public Result<?> list(Long userId) {
        LambdaQueryWrapper<Cart> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Cart::getUserId, userId);
        List<Cart> cartItems = cartMapper.selectList(wrapper);

        List<CartResponse> records = new ArrayList<>();
        for (Cart cart : cartItems) {
            Goods goods = goodsMapper.selectById(cart.getGoodsId());
            if (goods == null) {
                continue;
            }
            Shop shop = shopMapper.selectById(goods.getShopId());

            CartResponse response = CartResponse.builder()
                    .id(cart.getId())
                    .goodsId(goods.getId())
                    .goodsName(goods.getName())
                    .price(goods.getPrice())
                    .image(goods.getImage())
                    .count(cart.getCount())
                    .shopId(goods.getShopId())
                    .shopName(shop != null ? shop.getName() : null)
                    .build();
            records.add(response);
        }

        return Result.ok(records);
    }

    @Override
    public Result<?> add(Long userId, AddCartRequest request) {
        Goods goods = goodsMapper.selectById(request.getGoodsId());
        if (goods == null || goods.getStatus() != 1) {
            return Result.fail(ResultCode.GOODS_OFF_SHELF);
        }

        // 检查购物车中是否已存在该商品
        LambdaQueryWrapper<Cart> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Cart::getUserId, userId)
                .eq(Cart::getGoodsId, request.getGoodsId());
        Cart existingCart = cartMapper.selectOne(wrapper);

        if (existingCart != null) {
            existingCart.setCount(existingCart.getCount() + request.getCount());
            cartMapper.updateById(existingCart);
            return Result.ok(existingCart.getId());
        } else {
            Cart cart = new Cart();
            cart.setUserId(userId);
            cart.setGoodsId(request.getGoodsId());
            cart.setCount(request.getCount());
            cartMapper.insert(cart);
            return Result.ok(cart.getId());
        }
    }

    @Override
    public Result<?> update(Long userId, Long cartId, Integer count) {
        if (count <= 0) {
            return delete(userId, cartId);
        }

        Cart cart = cartMapper.selectById(cartId);
        if (cart == null) {
            return Result.fail("购物车记录不存在");
        }
        if (!cart.getUserId().equals(userId)) {
            return Result.fail("无权操作");
        }
        cart.setCount(count);
        cartMapper.updateById(cart);
        return Result.ok();
    }

    @Override
    public Result<?> delete(Long userId, Long cartId) {
        Cart cart = cartMapper.selectById(cartId);
        if (cart == null) {
            return Result.fail("购物车记录不存在");
        }
        if (!cart.getUserId().equals(userId)) {
            return Result.fail("无权操作");
        }
        cartMapper.deleteById(cartId);
        return Result.ok();
    }

    @Override
    public Result<?> clear(Long userId) {
        LambdaQueryWrapper<Cart> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Cart::getUserId, userId);
        cartMapper.delete(wrapper);
        return Result.ok();
    }
}
