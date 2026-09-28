package com.guoxu.payservice.dto.base;

import com.guoxu.payservice.common.enums.PayChannelEnum;
import com.guoxu.payservice.common.enums.PayTradeTypeEnum;
import com.guoxu.payservice.common.enums.TradeStatusEnum;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;

/**
 * AliRefundRequest
 * 支付宝退款请求入参实体
 * @author 执笔画棠
 * @date 2025/11/11 17:44
 **/
@Data
@Accessors(chain = true)
// 定义一个不可继承的最终类AliRefundRequest，它继承自AbstractRefundRequest，用于处理支付宝退款请求相关信息
public final class AliRefundRequest extends AbstractRefundRequest {
    /**
     * 支付金额，用于记录此次退款对应的支付金额数值
     */
    private BigDecimal payAmount;

    /**
     * 交易凭证号，用于标识该笔交易的唯一凭证编号
     */
    private String tradeNo;

    // 返回当前AliRefundRequest对象，因为该类本身就是处理支付宝退款请求的具体实现类
    @Override
    public AliRefundRequest getAliRefundRequest() {
        return this;
    }

    // 构建一个标识字符串，用于标识支付渠道、支付交易类型以及交易状态
    @Override
    public String buildMark() {
        // 初始化标识字符串为支付宝支付渠道名称
        String mark = PayChannelEnum.ALI_PAY.name();
        // 如果交易类型不为空
        if (getTradeType() != null) {
            // 重新构建标识字符串，添加支付交易类型名称和交易关闭状态码
            mark = PayChannelEnum.ALI_PAY.name() + "_" + PayTradeTypeEnum.findNameByCode(getTradeType()) + "_"
                    + TradeStatusEnum.TRADE_CLOSED.tradeCode();
        }
        // 返回构建好的标识字符串
        return mark;
    }
}
