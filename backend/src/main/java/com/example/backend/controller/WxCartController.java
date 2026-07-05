package com.example.backend.controller;

import com.example.backend.common.Result;
import com.example.backend.dto.request.AddCartRequest;
import com.example.backend.service.CartService;
import jakarta.validation.Valid;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/wx/cart")
@RequiredArgsConstructor
@Slf4j
public class WxCartController {

    private final CartService cartService;

    @GetMapping
    public Result<?> list(@RequestAttribute("userId") Long userId) {
        log.info("wx cart list: userId={}", userId);
        return cartService.list(userId);
    }

    @PostMapping
    public Result<?> add(@RequestAttribute("userId") Long userId,
                         @Valid @RequestBody AddCartRequest request) {
        log.info("wx cart add: userId={}, goodsId={}, count={}", userId, request.getGoodsId(), request.getCount());
        return cartService.add(userId, request);
    }

    @PutMapping("/{id}")
    public Result<?> update(@RequestAttribute("userId") Long userId,
                            @PathVariable Long id,
                            @RequestParam Integer count) {
        log.info("wx cart update: userId={}, cartId={}, count={}", userId, id, count);
        return cartService.update(userId, id, count);
    }

    @DeleteMapping("/{id}")
    public Result<?> delete(@RequestAttribute("userId") Long userId,
                            @PathVariable Long id) {
        log.info("wx cart delete: userId={}, cartId={}", userId, id);
        return cartService.delete(userId, id);
    }
}
