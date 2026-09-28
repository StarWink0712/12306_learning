package com.guoxu.orderservice.common.enums;

import com.guoxu.errorcode.IErrorCode;
import lombok.AllArgsConstructor;

/**
 * OrderCanalErrorCodeEnum
 * 实现平台错误码这个接口，定义订单服务相关的错误码
 * @author 执笔画棠
 * @version 2025/11/09 19:48
 **/
@AllArgsConstructor
public enum OrderCanalErrorCodeEnum implements IErrorCode {
    ORDER_CANAL_UNKNOWN_ERROR("B006001", "订单不存在，请检查相关订单记录"),

    ORDER_CANAL_STATUS_ERROR("B006002", "订单状态不正确，请检查相关订单记录"),

    ORDER_CANAL_ERROR("B006003", "订单取消失败，请稍后再试"),

    ORDER_CANAL_REPETITION_ERROR("B006004", "订单重复取消，请稍后再试"),

    ORDER_STATUS_REVERSAL_ERROR("B006005", "订单状态反转失败，请稍后再试"),

    ORDER_DELETE_ERROR("B006006", "订单状态反转失败，请稍后再试"),

    ORDER_ITEM_STATUS_REVERSAL_ERROR("B006007", "子订单状态反转失败，请稍后再试");

    /**
     * 错误码
     */
    private final String code;

    /**
     * 错误提示消息
     */
    private final String message;
    @Override
    public String code() {
        return "";
    }

    @Override
    public String message() {
        return "";
    }
}