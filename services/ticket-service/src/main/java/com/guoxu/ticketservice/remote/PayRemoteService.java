package com.guoxu.ticketservice.remote;

import com.guoxu.result.Result;
import com.guoxu.ticketservice.remote.dto.PayInfoRespDTO;
import com.guoxu.ticketservice.remote.dto.RefundReqDTO;
import com.guoxu.ticketservice.remote.dto.RefundRespDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * PayRemoteService
 * 支付单远程调用服务
 * @author 执笔画棠
 * @version 2025/11/12 22:21
 **/
@FeignClient(value = "index12306-pay${unique-name:}-service", url = "${aggregation.remote-url:}")
public interface PayRemoteService {
    /**
     * 支付单详情查询
     */
    @GetMapping("/api/pay-service/pay/query")
    Result<PayInfoRespDTO> getPayInfo(@RequestParam(value = "orderSn") String orderSn);

    /**
     * 公共退款接口
     */
    @PostMapping("/api/pay-service/common/refund")
    Result<RefundRespDTO> commonRefund(@RequestBody RefundReqDTO requestParam);
}