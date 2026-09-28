package com.guoxu.orderservice.common.constant;

/**
 * OrderRocketMQConstant ROCKET MQ订单服务常量类
 *
 * @author 执笔画棠
 * @date 2025/11/09 19:47
 **/
public final class OrderRocketMQConstant {


    /**
     * 订单服务相关业务 Topic Key
     */
    public static final String ORDER_DELAY_CLOSE_TOPIC_KEY = "index12306_order-service_delay-close-order_topic${unique-name:}";

    /**
     * 购票服务创建订单后延时关闭业务 Tag Key
     */
    public static final String ORDER_DELAY_CLOSE_TAG_KEY = "index12306_order-service_delay-close-order_tag${unique-name:}";

    /**
     * 支付服务相关业务 Topic Key
     */
    public static final String PAY_GLOBAL_TOPIC_KEY = "index12306_pay-service_topic${unique-name:}";

    /**
     * 支付结果回调状态 Tag Key
     */
    public static final String PAY_RESULT_CALLBACK_TAG_KEY = "index12306_pay-service_pay-result-callback_tag${unique-name:}";

    /**
     * 支付结果回调订单消费者组 Key
     */
    public static final String PAY_RESULT_CALLBACK_ORDER_CG_KEY = "index12306_pay-service_pay-result-callback-order_cg${unique-name:}";

    /**
     * 退款结果回调订单 Tag Key
     */
    public static final String REFUND_RESULT_CALLBACK_TAG_KEY = "index12306_pay-service_refund-result-callback_tag${unique-name:}";

    /**
     * 退款结果回调订单消费者组 Key
     */
    public static final String REFUND_RESULT_CALLBACK_ORDER_CG_KEY = "index12306_pay-service_refund-result-callback-order_cg${unique-name:}";
}
