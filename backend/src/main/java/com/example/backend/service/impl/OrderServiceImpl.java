package com.example.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.backend.common.PageResult;
import com.example.backend.common.Result;
import com.example.backend.common.ResultCode;
import com.example.backend.dto.request.CreateOrderRequest;
import com.example.backend.dto.response.OrderResponse;
import com.example.backend.entity.*;
import com.example.backend.event.EventMessage;
import com.example.backend.event.RedisMessagePublisher;
import com.example.backend.mapper.*;
import com.example.backend.service.DispatchService;
import com.example.backend.service.MarketingService;
import com.example.backend.service.OrderService;
import com.example.backend.service.impl.EventLogService;
import com.example.backend.utils.RandomUtil;
import com.example.backend.websocket.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final UserMapper userMapper;
    private final ShopMapper shopMapper;
    private final DeliveryMapper deliveryMapper;
    private final CartMapper cartMapper;
    private final GoodsMapper goodsMapper;
    private final EvaluationMapper evaluationMapper;
    private final AddressMapper addressMapper;
    private final CouponMapper couponMapper;
    private final UserCouponMapper userCouponMapper;
    private final NotificationService notificationService;
    private final DispatchService dispatchService;
    private final RedisMessagePublisher redisMessagePublisher;
    private final EventLogService eventLogService;
    private final MarketingService marketingService;

    // ==================== Admin Methods ====================

    @Override
    public Result<?> adminList(Integer page, Integer pageSize, String orderNo, String username, String merchantName, Integer status) {
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        if (orderNo != null && !orderNo.isEmpty()) {
            wrapper.like(Order::getOrderNo, orderNo);
        }
        if (status != null) {
            wrapper.eq(Order::getStatus, status);
        }
        wrapper.orderByDesc(Order::getCreateTime);

        Page<Order> orderPage = new Page<>(page, pageSize);
        orderMapper.selectPage(orderPage, wrapper);

        List<Map<String, Object>> records = orderPage.getRecords().stream().map(order -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", order.getId());
            map.put("orderNo", order.getOrderNo());
            map.put("userId", order.getUserId());
            // Look up username
            User user = userMapper.selectById(order.getUserId());
            map.put("username", user != null ? user.getNickname() : "");
            map.put("shopId", order.getShopId());
            // Look up merchant name
            Shop shop = shopMapper.selectById(order.getShopId());
            map.put("merchantName", shop != null ? shop.getName() : "");
            map.put("deliveryId", order.getDeliveryId());
            // Look up delivery name
            if (order.getDeliveryId() != null) {
                Delivery delivery = deliveryMapper.selectById(order.getDeliveryId());
                map.put("deliveryName", delivery != null ? delivery.getName() : "");
            } else {
                map.put("deliveryName", "");
            }
            map.put("totalPrice", order.getTotalPrice());
            map.put("actualAmount", order.getActualAmount());
            map.put("deliveryFee", order.getDeliveryFee());
            map.put("packageFee", order.getPackageFee());
            map.put("payMethod", order.getPayMethod());
            map.put("payTime", order.getPayTime());
            map.put("remark", order.getRemark());
            map.put("goodsDesc", order.getGoodsDesc());
            map.put("goodsCount", order.getGoodsCount());
            map.put("amount", order.getActualAmount() != null ? order.getActualAmount() : order.getTotalPrice());
            map.put("status", order.getStatus());
            map.put("createTime", order.getCreateTime());
            return map;
        }).collect(Collectors.toList());

        return Result.ok(PageResult.of(records, orderPage.getTotal(), page, pageSize));
    }

    @Override
    public Result<?> adminDetail(Long id) {
        Order order = orderMapper.selectById(id);
        if (order == null) {
            return Result.fail(ResultCode.NOT_FOUND);
        }

        // Build OrderResponse
        OrderResponse.OrderResponseBuilder builder = OrderResponse.builder()
                .id(order.getId())
                .orderNo(order.getOrderNo())
                .userId(order.getUserId())
                .shopId(order.getShopId())
                .deliveryId(order.getDeliveryId())
                .addressInfo(order.getAddressInfo())
                .goodsDesc(order.getGoodsDesc())
                .goodsCount(order.getGoodsCount())
                .totalPrice(order.getTotalPrice())
                .deliveryFee(order.getDeliveryFee())
                .packageFee(order.getPackageFee())
                .actualAmount(order.getActualAmount())
                .status(order.getStatus())
                .payMethod(order.getPayMethod())
                .payTime(order.getPayTime())
                .remark(order.getRemark())
                .createTime(order.getCreateTime());

        // Look up names
        User user = userMapper.selectById(order.getUserId());
        builder.username(user != null ? user.getNickname() : "");

        Shop shop = shopMapper.selectById(order.getShopId());
        builder.shopName(shop != null ? shop.getName() : "");

        if (order.getDeliveryId() != null) {
            Delivery delivery = deliveryMapper.selectById(order.getDeliveryId());
            builder.deliveryName(delivery != null ? delivery.getName() : "");
        } else {
            builder.deliveryName("");
        }

        // Get items
        LambdaQueryWrapper<OrderItem> itemWrapper = new LambdaQueryWrapper<>();
        itemWrapper.eq(OrderItem::getOrderId, id);
        List<OrderItem> items = orderItemMapper.selectList(itemWrapper);
        List<OrderResponse.OrderItemResponse> itemResponses = items.stream().map(item ->
                OrderResponse.OrderItemResponse.builder()
                        .id(item.getId())
                        .goodsName(item.getGoodsName())
                        .goodsPrice(item.getGoodsPrice())
                        .count(item.getCount())
                        .build()
        ).collect(Collectors.toList());
        builder.items(itemResponses);

        return Result.ok(builder.build());
    }

    @Override
    @Transactional
    public Result<?> adminCancel(Long id) {
        Order order = orderMapper.selectById(id);
        if (order == null) {
            return Result.fail(ResultCode.NOT_FOUND);
        }
        if (order.getStatus() != 0) {
            return Result.fail(ResultCode.ORDER_NOT_CANCELABLE);
        }
        order.setStatus(4);
        order.setUpdateTime(LocalDateTime.now());
        orderMapper.updateById(order);
        return Result.ok("取消成功");
    }

    // ==================== Merchant Methods ====================

    @Override
    public Result<?> merchantList(Long shopId, Integer page, Integer pageSize, String keyword, Integer status) {
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Order::getShopId, shopId);
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.like(Order::getOrderNo, keyword);
        }
        if (status != null) {
            wrapper.eq(Order::getStatus, status);
        }
        wrapper.orderByDesc(Order::getCreateTime);

        Page<Order> orderPage = new Page<>(page, pageSize);
        orderMapper.selectPage(orderPage, wrapper);

        List<Map<String, Object>> records = orderPage.getRecords().stream().map(order -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", order.getOrderNo());  // using orderNo as display id for merchant list
            map.put("orderId", order.getId());
            User user = userMapper.selectById(order.getUserId());
            map.put("customerName", user != null ? user.getNickname() : "");
            map.put("phone", user != null ? user.getPhone() : "");
            map.put("address", order.getAddressInfo());
            // Get items
            LambdaQueryWrapper<OrderItem> itemWrapper = new LambdaQueryWrapper<>();
            itemWrapper.eq(OrderItem::getOrderId, order.getId());
            List<OrderItem> items = orderItemMapper.selectList(itemWrapper);
            map.put("items", items);
            map.put("total", order.getTotalPrice());
            map.put("status", order.getStatus());
            map.put("createTime", order.getCreateTime());
            map.put("payTime", order.getPayTime());
            map.put("remark", order.getRemark());
            return map;
        }).collect(Collectors.toList());

        return Result.ok(PageResult.of(records, orderPage.getTotal(), page, pageSize));
    }

    @Override
    @Transactional
    public Result<?> merchantUpdateStatus(Long id, Integer status) {
        Order order = orderMapper.selectById(id);
        if (order == null) {
            return Result.fail(ResultCode.NOT_FOUND);
        }
        order.setStatus(status);
        order.setUpdateTime(LocalDateTime.now());
        orderMapper.updateById(order);
        return Result.ok();
    }

    // ==================== WeChat Methods ====================

    @Override
    @Transactional
    public Result<?> wxCreate(Long userId, CreateOrderRequest request) {
        // Validate all goods exist and are on shelf, calculate total
        BigDecimal totalPrice = BigDecimal.ZERO;
        List<Goods> goodsList = new ArrayList<>();

        for (CreateOrderRequest.OrderItemRequest item : request.getItems()) {
            Goods goods = goodsMapper.selectById(item.getGoodsId());
            if (goods == null) {
                return Result.fail(ResultCode.NOT_FOUND);
            }
            if (goods.getStatus() != 1) {
                return Result.fail(ResultCode.GOODS_OFF_SHELF);
            }
            if (goods.getStock() < item.getCount()) {
                return Result.fail(ResultCode.STOCK_NOT_ENOUGH);
            }
            totalPrice = totalPrice.add(goods.getPrice().multiply(BigDecimal.valueOf(item.getCount())));
            goodsList.add(goods);
        }

        // Get shop delivery fee
        Shop shop = shopMapper.selectById(request.getShopId());
        BigDecimal deliveryFee = (shop != null && shop.getDeliveryFee() != null) ? shop.getDeliveryFee() : BigDecimal.ZERO;

        // Get address
        Address address = addressMapper.selectById(request.getAddressId());
        String addressInfo = address != null ?
                (address.getProvince() != null ? address.getProvince() : "") +
                (address.getCity() != null ? address.getCity() : "") +
                (address.getDistrict() != null ? address.getDistrict() : "") +
                (address.getDetail() != null ? address.getDetail() : "")
                : "";
        Double addressLat = address != null ? address.getLatitude() : null;
        Double addressLng = address != null ? address.getLongitude() : null;

        // Generate order number
        String orderNo = RandomUtil.generateOrderNo();

        // Build goods description from item names
        int goodsCount = request.getItems().stream().mapToInt(CreateOrderRequest.OrderItemRequest::getCount).sum();
        String goodsDesc = goodsList.stream().map(Goods::getName).collect(Collectors.joining(","));
        if (goodsDesc.length() > 50) {
            goodsDesc = goodsDesc.substring(0, 50) + "...";
        }

        // Create order
        Order order = new Order();
        order.setOrderNo(orderNo);
        order.setUserId(userId);
        order.setShopId(request.getShopId());
        order.setAddressInfo(addressInfo);
        order.setAddressLat(addressLat);
        order.setAddressLng(addressLng);
        order.setGoodsDesc(goodsDesc);
        order.setGoodsCount(goodsCount);
        order.setTotalPrice(totalPrice);
        order.setDeliveryFee(deliveryFee);
        order.setPackageFee(BigDecimal.ZERO);
        // Apply coupon if provided
        BigDecimal discountAmount = BigDecimal.ZERO;
        String discountDesc = "";
        if (request.getCouponId() != null) {
            UserCoupon userCoupon = userCouponMapper.selectById(request.getCouponId());
            if (userCoupon != null && userCoupon.getUserId().equals(userId) && userCoupon.getStatus() == 0) {
                Coupon coupon = couponMapper.selectById(userCoupon.getCouponId());
                if (coupon != null && coupon.getStatus() == 1) {
                    // Validate coupon belongs to this shop (or is platform-wide)
                    if (coupon.getShopId() != null && !coupon.getShopId().equals(request.getShopId())) {
                        return Result.fail("该优惠券不适用于当前店铺");
                    }
                    if (totalPrice.compareTo(coupon.getConditionAmount()) >= 0) {
                        discountAmount = coupon.getReduceAmount();
                        discountDesc = "优惠券-" + coupon.getName();
                        userCoupon.setStatus(1);
                        userCoupon.setUseTime(LocalDateTime.now());
                        userCouponMapper.updateById(userCoupon);
                    }
                }
            }
        }

        // Apply full-reduce activity (auto, no user action needed)
        FullReduceActivity activity = marketingService.getBestActivity(request.getShopId(), totalPrice);
        BigDecimal activityDiscount = BigDecimal.ZERO;
        if (activity != null) {
            activityDiscount = activity.getReduceAmount();
            if (!discountDesc.isEmpty()) discountDesc += " + ";
            discountDesc += "满减-" + activity.getName();
        }

        BigDecimal totalDiscount = discountAmount.add(activityDiscount);
        BigDecimal actualAmount = totalPrice.add(deliveryFee).subtract(totalDiscount);
        if (actualAmount.compareTo(BigDecimal.ZERO) < 0) {
            actualAmount = BigDecimal.ZERO;
        }
        order.setActualAmount(actualAmount);
        order.setStatus(0);
        order.setRemark(request.getRemark());
        order.setCreateTime(LocalDateTime.now());
        order.setUpdateTime(LocalDateTime.now());
        orderMapper.insert(order);

        // Create order items
        for (CreateOrderRequest.OrderItemRequest item : request.getItems()) {
            Goods goods = goodsMapper.selectById(item.getGoodsId());
            OrderItem orderItem = new OrderItem();
            orderItem.setOrderId(order.getId());
            orderItem.setGoodsId(item.getGoodsId());
            orderItem.setGoodsName(goods.getName());
            orderItem.setGoodsPrice(goods.getPrice());
            orderItem.setCount(item.getCount());
            orderItem.setCreateTime(LocalDateTime.now());
            orderItemMapper.insert(orderItem);
        }

        // Clear user's cart for the shop's goods
        LambdaQueryWrapper<Cart> cartWrapper = new LambdaQueryWrapper<>();
        cartWrapper.eq(Cart::getUserId, userId);
        cartWrapper.in(Cart::getGoodsId, goodsList.stream().map(Goods::getId).collect(Collectors.toList()));
        cartMapper.delete(cartWrapper);

        // Build response
        return Result.ok(buildOrderDetail(order.getId()));
    }

    @Override
    public Result<?> wxList(Long userId, Integer tab, Integer page, Integer pageSize) {
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Order::getUserId, userId);

        if (tab != null) {
            if (tab == 1) {
                // Ongoing: status 0-2
                wrapper.in(Order::getStatus, 0, 1, 2);
            } else if (tab == 2) {
                // History: status 3-6
                wrapper.in(Order::getStatus, 3, 4, 5, 6);
            }
        }
        wrapper.orderByDesc(Order::getCreateTime);

        Page<Order> orderPage = new Page<>(page, pageSize);
        orderMapper.selectPage(orderPage, wrapper);

        List<Map<String, Object>> records = orderPage.getRecords().stream().map(order -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", order.getId());
            map.put("orderNo", order.getOrderNo());
            map.put("shopId", order.getShopId());

            Shop shop = shopMapper.selectById(order.getShopId());
            map.put("shopName", shop != null ? shop.getName() : "");

            map.put("goodsDesc", order.getGoodsDesc());
            map.put("goodsCount", order.getGoodsCount());
            map.put("totalPrice", order.getTotalPrice() != null ? order.getTotalPrice().toString() : "0");
            map.put("deliveryFee", order.getDeliveryFee());
            map.put("status", order.getStatus());
            map.put("statusText", getStatusText(order.getStatus()));

            // Check if evaluated
            LambdaQueryWrapper<Evaluation> evalWrapper = new LambdaQueryWrapper<>();
            evalWrapper.eq(Evaluation::getOrderId, order.getId());
            evalWrapper.eq(Evaluation::getUserId, userId);
            map.put("isRated", evaluationMapper.selectCount(evalWrapper) > 0);

            map.put("createTime", order.getCreateTime());
            map.put("payMethod", order.getPayMethod());

            // Get goods items
            LambdaQueryWrapper<OrderItem> itemWrapper = new LambdaQueryWrapper<>();
            itemWrapper.eq(OrderItem::getOrderId, order.getId());
            List<OrderItem> items = orderItemMapper.selectList(itemWrapper);
            map.put("goodsList", items);

            return map;
        }).collect(Collectors.toList());

        return Result.ok(PageResult.of(records, orderPage.getTotal(), page, pageSize));
    }

    @Override
    public Result<?> wxDetail(Long id) {
        Map<String, Object> detail = buildOrderDetail(id);
        return Result.ok(detail);
    }

    @Override
    @Transactional
    public Result<?> wxPay(Long userId, Long id, String payMethod) {
        // 验证支付方式
        if (!"微信支付".equals(payMethod) && !"支付宝".equals(payMethod)) {
            return Result.fail("不支持的支付方式");
        }

        Order order = orderMapper.selectById(id);
        if (order == null) {
            return Result.fail(ResultCode.NOT_FOUND);
        }
        if (!order.getUserId().equals(userId)) {
            return Result.fail(ResultCode.FORBIDDEN);
        }
        if (order.getStatus() != 0) {
            return Result.fail(ResultCode.ORDER_STATUS_ERROR);
        }

        // 支付沙箱模拟：90% 成功率
        double random = Math.random();
        if (random > 0.90) {
            return Result.fail("支付通道繁忙，请稍后重试");
        }

        // ============ 核心事务：订单状态 + 事件日志 原子写入 ============
        order.setStatus(1);
        order.setPayMethod(payMethod);
        order.setPayTime(LocalDateTime.now());
        order.setUpdateTime(LocalDateTime.now());
        orderMapper.updateById(order);

        Map<String, Object> eventData = new HashMap<>();
        eventData.put("orderId", id);
        eventData.put("orderNo", order.getOrderNo());
        eventData.put("shopId", order.getShopId());
        eventData.put("userId", userId);

        Long eventLogId = eventLogService.saveEvent(EventMessage.ORDER_PAID, eventData);

        // ============ 事务提交后 → Redis 实时推送 ============
        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        eventLogService.tryPublishAfterCommit(eventLogId);
                    }
                });

        // 如果 Redis 推送失败 → 定时任务 60秒后会扫描 event_log 兜底重试
        return Result.ok("支付成功");
    }

    @Override
    @Transactional
    public Result<?> wxCancle(Long userId, Long id) {
        Order order = orderMapper.selectById(id);
        if (order == null) {
            return Result.fail(ResultCode.NOT_FOUND);
        }
        if (!order.getUserId().equals(userId)) {
            return Result.fail(ResultCode.FORBIDDEN);
        }
        if (order.getStatus() != 0) {
            return Result.fail(ResultCode.ORDER_NOT_CANCELABLE);
        }

        order.setStatus(4);
        order.setUpdateTime(LocalDateTime.now());
        orderMapper.updateById(order);
        return Result.ok("取消成功");
    }

    @Override
    @Transactional
    public Result<?> wxRefund(Long userId, Long id, String reason) {
        Order order = orderMapper.selectById(id);
        if (order == null) {
            return Result.fail(ResultCode.NOT_FOUND);
        }
        if (!order.getUserId().equals(userId)) {
            return Result.fail(ResultCode.FORBIDDEN);
        }
        if (order.getStatus() != 1 && order.getStatus() != 2) {
            return Result.fail("当前订单状态不支持退款");
        }

        order.setStatus(5);
        order.setCancelReason(reason);
        order.setUpdateTime(LocalDateTime.now());
        orderMapper.updateById(order);

        notificationService.notifyOrderStatusChange(order, "用户已申请退款，等待审核");
        log.info("refund requested: orderId={}, reason={}", id, reason);
        return Result.ok("退款申请已提交");
    }

    // ==================== Merchant Refund Methods ====================

    @Override
    public Result<?> merchantRefundList(Long shopId, Integer page, Integer pageSize) {
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Order::getShopId, shopId);
        wrapper.eq(Order::getStatus, 5);
        wrapper.orderByDesc(Order::getUpdateTime);

        Page<Order> orderPage = new Page<>(page, pageSize);
        orderMapper.selectPage(orderPage, wrapper);

        List<Map<String, Object>> records = orderPage.getRecords().stream().map(order -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", order.getId());
            map.put("orderNo", order.getOrderNo());
            map.put("totalPrice", order.getTotalPrice());
            map.put("actualAmount", order.getActualAmount());
            map.put("reason", order.getCancelReason());
            map.put("createTime", order.getCreateTime());
            map.put("updateTime", order.getUpdateTime());
            User user = userMapper.selectById(order.getUserId());
            map.put("username", user != null ? user.getNickname() : "");
            map.put("userPhone", user != null ? user.getPhone() : "");
            return map;
        }).collect(Collectors.toList());

        return Result.ok(PageResult.of(records, orderPage.getTotal(), page, pageSize));
    }

    @Override
    @Transactional
    public Result<?> merchantRefundApprove(Long shopId, Long id) {
        Order order = orderMapper.selectById(id);
        if (order == null || !order.getShopId().equals(shopId)) {
            return Result.fail(ResultCode.NOT_FOUND);
        }
        if (order.getStatus() != 5) {
            return Result.fail("订单不在退款中状态");
        }
        order.setStatus(6);
        order.setUpdateTime(LocalDateTime.now());
        orderMapper.updateById(order);
        restoreStock(order);
        notificationService.notifyOrderStatusChange(order, "商家已同意退款");
        return Result.ok("已同意退款");
    }

    @Override
    @Transactional
    public Result<?> merchantRefundReject(Long shopId, Long id) {
        Order order = orderMapper.selectById(id);
        if (order == null || !order.getShopId().equals(shopId)) {
            return Result.fail(ResultCode.NOT_FOUND);
        }
        if (order.getStatus() != 5) {
            return Result.fail("订单不在退款中状态");
        }
        order.setStatus(7);
        order.setUpdateTime(LocalDateTime.now());
        orderMapper.updateById(order);
        notificationService.notifyOrderStatusChange(order, "商家已拒绝退款，等待平台处理");
        return Result.ok("已拒绝退款并提交平台裁定");
    }

    private void restoreStock(Order order) {
        LambdaQueryWrapper<OrderItem> itemWrapper = new LambdaQueryWrapper<>();
        itemWrapper.eq(OrderItem::getOrderId, order.getId());
        List<OrderItem> items = orderItemMapper.selectList(itemWrapper);
        for (OrderItem item : items) {
            Goods goods = goodsMapper.selectById(item.getGoodsId());
            if (goods != null) {
                goods.setStock(goods.getStock() + item.getCount());
                goods.setSales(Math.max(0, (goods.getSales() != null ? goods.getSales() : 0) - item.getCount()));
                goods.setUpdateTime(LocalDateTime.now());
                goodsMapper.updateById(goods);
            }
        }
    }

    @Override
    public Result<?> adminRefundList(Integer page, Integer pageSize) {
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(Order::getStatus, 5, 7);
        wrapper.orderByDesc(Order::getUpdateTime);

        Page<Order> orderPage = new Page<>(page, pageSize);
        orderMapper.selectPage(orderPage, wrapper);

        List<Map<String, Object>> records = orderPage.getRecords().stream().map(order -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", order.getId());
            map.put("orderNo", order.getOrderNo());
            map.put("totalPrice", order.getTotalPrice());
            map.put("actualAmount", order.getActualAmount());
            map.put("status", order.getStatus());
            map.put("reason", order.getCancelReason());
            map.put("createTime", order.getCreateTime());
            map.put("updateTime", order.getUpdateTime());

            User user = userMapper.selectById(order.getUserId());
            map.put("username", user != null ? user.getNickname() : "");
            map.put("userPhone", user != null ? user.getPhone() : "");

            Shop shop = shopMapper.selectById(order.getShopId());
            map.put("shopName", shop != null ? shop.getName() : "");

            return map;
        }).collect(Collectors.toList());

        return Result.ok(PageResult.of(records, orderPage.getTotal(), page, pageSize));
    }

    @Override
    @Transactional
    public Result<?> adminRefundApprove(Long id) {
        Order order = orderMapper.selectById(id);
        if (order == null) {
            return Result.fail(ResultCode.NOT_FOUND);
        }
        if (order.getStatus() != 5 && order.getStatus() != 7) {
            return Result.fail("订单不在退款审核状态");
        }

        order.setStatus(6);
        order.setUpdateTime(LocalDateTime.now());
        orderMapper.updateById(order);
        restoreStock(order);

        notificationService.notifyOrderStatusChange(order, "平台审核通过，退款成功");
        log.info("refund approved by admin: orderId={}", id);
        return Result.ok("退款审核通过");
    }

    @Override
    @Transactional
    public Result<?> adminRefundReject(Long id) {
        Order order = orderMapper.selectById(id);
        if (order == null) {
            return Result.fail(ResultCode.NOT_FOUND);
        }
        if (order.getStatus() != 5 && order.getStatus() != 7) {
            return Result.fail("订单不在退款审核状态");
        }

        order.setStatus(1);
        order.setCancelReason(null);
        order.setUpdateTime(LocalDateTime.now());
        orderMapper.updateById(order);

        notificationService.notifyOrderStatusChange(order, "平台驳回退款申请，订单继续有效");
        log.info("refund rejected by admin: orderId={}", id);
        return Result.ok("退款申请已驳回");
    }

    // ==================== Helper Methods ====================

    private String getStatusText(Integer status) {
        if (status == null) return "未知";
        switch (status) {
            case 0: return "待支付";
            case 1: return "已支付";
            case 2: return "配送中";
            case 3: return "已完成";
            case 4: return "已取消";
            case 5: return "退款中";
            case 6: return "已退款";
            case 7: return "商家拒绝退款";
            default: return "未知";
        }
    }

    private Map<String, Object> buildOrderDetail(Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            return new HashMap<>();
        }

        Map<String, Object> detail = new HashMap<>();
        detail.put("id", order.getId());
        detail.put("orderNo", order.getOrderNo());
        detail.put("userId", order.getUserId());
        detail.put("shopId", order.getShopId());
        detail.put("deliveryId", order.getDeliveryId());
        detail.put("addressInfo", order.getAddressInfo());
        detail.put("goodsDesc", order.getGoodsDesc());
        detail.put("goodsCount", order.getGoodsCount());
        detail.put("totalPrice", order.getTotalPrice());
        detail.put("deliveryFee", order.getDeliveryFee());
        detail.put("packageFee", order.getPackageFee());
        detail.put("actualAmount", order.getActualAmount());
        // 计算总优惠金额（满减 + 优惠券）
        BigDecimal totalPrice = order.getTotalPrice() != null ? order.getTotalPrice() : BigDecimal.ZERO;
        BigDecimal deliveryFee = order.getDeliveryFee() != null ? order.getDeliveryFee() : BigDecimal.ZERO;
        BigDecimal packageFee = order.getPackageFee() != null ? order.getPackageFee() : BigDecimal.ZERO;
        BigDecimal actual = order.getActualAmount() != null ? order.getActualAmount() : BigDecimal.ZERO;
        BigDecimal discountAmount = totalPrice.add(deliveryFee).add(packageFee).subtract(actual);
        detail.put("discountAmount", discountAmount.compareTo(BigDecimal.ZERO) > 0 ? discountAmount : BigDecimal.ZERO);
        detail.put("status", order.getStatus());
        detail.put("statusText", getStatusText(order.getStatus()));
        detail.put("payMethod", order.getPayMethod());
        detail.put("payTime", order.getPayTime());
        detail.put("cancelReason", order.getCancelReason());
        detail.put("remark", order.getRemark());
        detail.put("createTime", order.getCreateTime());
        detail.put("updateTime", order.getUpdateTime());

        // Shop info
        Shop shop = shopMapper.selectById(order.getShopId());
        if (shop != null) {
            detail.put("shopName", shop.getName());
            detail.put("shopPhone", shop.getPhone());
            detail.put("shopAddress", shop.getAddress());
        }

        // User info
        User user = userMapper.selectById(order.getUserId());
        if (user != null) {
            detail.put("username", user.getNickname());
            detail.put("userPhone", user.getPhone());
        }

        // Delivery rider info
        if (order.getDeliveryId() != null) {
            Delivery delivery = deliveryMapper.selectById(order.getDeliveryId());
            if (delivery != null) {
                detail.put("riderName", delivery.getName());
                detail.put("riderPhone", delivery.getPhone());
            }
        }

        // Check for rider rating (from Evaluation by deliveryId)
        if (order.getDeliveryId() != null) {
            LambdaQueryWrapper<Evaluation> evalWrapper = new LambdaQueryWrapper<>();
            evalWrapper.eq(Evaluation::getDeliveryId, order.getDeliveryId());
            evalWrapper.eq(Evaluation::getOrderId, orderId);
            Evaluation evaluation = evaluationMapper.selectOne(evalWrapper);
            detail.put("riderRating", evaluation != null ? evaluation.getRating() : null);
        }

        // Order items
        LambdaQueryWrapper<OrderItem> itemWrapper = new LambdaQueryWrapper<>();
        itemWrapper.eq(OrderItem::getOrderId, orderId);
        List<OrderItem> items = orderItemMapper.selectList(itemWrapper);
        detail.put("goodsList", items);

        return detail;
    }

    // ==================== 大订单拆单（多骑手协同配送） ====================

    @Override
    @Transactional
    public Result<?> splitLargeOrder(Long orderId) {
        Order parent = orderMapper.selectById(orderId);
        if (parent == null) return Result.fail(ResultCode.NOT_FOUND);
        if (parent.getIsLargeOrder() == null || parent.getIsLargeOrder() != 1) {
            return Result.fail("该订单不是大订单，无需拆分");
        }

        // 获取订单明细
        LambdaQueryWrapper<OrderItem> itemWrapper = new LambdaQueryWrapper<>();
        itemWrapper.eq(OrderItem::getOrderId, orderId);
        List<OrderItem> items = orderItemMapper.selectList(itemWrapper);
        if (items.size() < 2) {
            return Result.fail("订单商品不足2项，无法拆分");
        }

        // 每2个商品一个子订单
        int batchSize = Math.max(1, items.size() / 2);
        List<Map<String, Object>> children = new ArrayList<>();

        for (int i = 0; i < items.size(); i += batchSize) {
            int end = Math.min(i + batchSize, items.size());
            List<OrderItem> batch = items.subList(i, end);

            Order child = new Order();
            child.setOrderNo("S" + parent.getOrderNo() + "-" + ((i / batchSize) + 1));
            child.setUserId(parent.getUserId());
            child.setShopId(parent.getShopId());
            child.setAddressInfo(parent.getAddressInfo());
            child.setAddressLat(parent.getAddressLat());
            child.setAddressLng(parent.getAddressLng());
            child.setStatus(1);
            child.setParentOrderId(parent.getId());
            child.setIsLargeOrder(0);
            child.setPayMethod(parent.getPayMethod());
            child.setPayTime(parent.getPayTime());
            child.setCreateTime(LocalDateTime.now());
            child.setUpdateTime(LocalDateTime.now());

            BigDecimal totalPrice = BigDecimal.ZERO;
            int count = 0;
            StringBuilder desc = new StringBuilder();
            for (OrderItem item : batch) {
                totalPrice = totalPrice.add(item.getGoodsPrice().multiply(BigDecimal.valueOf(item.getCount())));
                count += item.getCount();
                if (desc.length() > 0) desc.append("、");
                desc.append(item.getGoodsName()).append("×").append(item.getCount());
            }
            child.setTotalPrice(totalPrice);
            child.setActualAmount(totalPrice);
            child.setGoodsCount(count);
            child.setGoodsDesc(desc.toString());
            child.setDeliveryFee(parent.getDeliveryFee() != null
                    ? parent.getDeliveryFee().divide(BigDecimal.valueOf(items.size() / batchSize + 1), 2, java.math.RoundingMode.HALF_UP)
                    : BigDecimal.ZERO);
            child.setPackageFee(BigDecimal.ZERO);
            orderMapper.insert(child);

            for (OrderItem item : batch) {
                OrderItem childItem = new OrderItem();
                childItem.setOrderId(child.getId());
                childItem.setGoodsId(item.getGoodsId());
                childItem.setGoodsName(item.getGoodsName());
                childItem.setGoodsPrice(item.getGoodsPrice());
                childItem.setCount(item.getCount());
                childItem.setCreateTime(LocalDateTime.now());
                orderItemMapper.insert(childItem);
            }

            Map<String, Object> childInfo = new LinkedHashMap<>();
            childInfo.put("childOrderId", child.getId());
            childInfo.put("orderNo", child.getOrderNo());
            childInfo.put("goodsDesc", desc.toString());
            childInfo.put("totalPrice", totalPrice);
            childInfo.put("goodsCount", count);
            children.add(childInfo);

            try { dispatchService.dispatch(child.getId()); } catch (Exception e) { }
        }

        log.info("Large order {} split into {} child orders", orderId, children.size());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("parentOrderId", parent.getId());
        result.put("parentOrderNo", parent.getOrderNo());
        result.put("childCount", children.size());
        result.put("children", children);
        return Result.ok(result);
    }

    @Override
    public Result<?> getChildren(Long parentOrderId) {
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Order::getParentOrderId, parentOrderId);
        wrapper.orderByAsc(Order::getId);
        List<Order> children = orderMapper.selectList(wrapper);

        List<Map<String, Object>> records = children.stream().map(child -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", child.getId());
            map.put("orderNo", child.getOrderNo());
            map.put("status", child.getStatus());
            map.put("totalPrice", child.getTotalPrice());
            map.put("actualAmount", child.getActualAmount());
            map.put("goodsDesc", child.getGoodsDesc());
            map.put("goodsCount", child.getGoodsCount());
            map.put("payMethod", child.getPayMethod());
            map.put("createTime", child.getCreateTime());
            if (child.getDeliveryId() != null) {
                Delivery delivery = deliveryMapper.selectById(child.getDeliveryId());
                map.put("deliveryName", delivery != null ? delivery.getName() : null);
            }
            return map;
        }).collect(Collectors.toList());

        return Result.ok(records);
    }
}
