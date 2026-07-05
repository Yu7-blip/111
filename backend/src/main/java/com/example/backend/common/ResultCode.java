package com.example.backend.common;

import lombok.Getter;

@Getter
public enum ResultCode {

    SUCCESS(200, "success"),
    BAD_REQUEST(400, "请求参数错误"),
    UNAUTHORIZED(401, "未登录或登录已过期"),
    FORBIDDEN(403, "无权限"),
    NOT_FOUND(404, "资源不存在"),
    INTERNAL_ERROR(500, "服务器内部错误"),

    // 业务错误码
    LOGIN_FAILED(1001, "用户名或密码错误"),
    PHONE_CODE_ERROR(1002, "验证码错误"),
    ACCOUNT_DISABLED(1003, "账号已被禁用"),
    GOODS_OFF_SHELF(2001, "商品已下架"),
    STOCK_NOT_ENOUGH(2002, "库存不足"),
    ORDER_STATUS_ERROR(3001, "订单状态异常"),
    ORDER_NOT_CANCELABLE(3002, "该订单状态不可取消"),
    DELIVERY_NOT_AVAILABLE(4001, "骑手不在线"),
    MERCHANT_NOT_APPROVED(5001, "商家未通过审核");

    private final int code;
    private final String message;

    ResultCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
