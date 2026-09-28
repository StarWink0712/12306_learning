package com.guoxu.payservice.controller;

import com.guoxu.Results;
import com.guoxu.payservice.dto.RefundReqDTO;
import com.guoxu.payservice.dto.RefundRespDTO;
import com.guoxu.payservice.service.RefundService;
import com.guoxu.result.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * RefundController 公共退款请求层
 *
 * @author 执笔画棠
 * @date 2025/11/12 16:22
 **/
@RestController
@RequiredArgsConstructor
public class RefundController {
    private final RefundService refundService;

    /**
     * 公共退款接口
     */
    @PostMapping("/api/pay-service/common/refund")
    public Result<RefundRespDTO> commonRefund(@RequestBody RefundReqDTO requestParam) {
        return Results.success(refundService.commonRefund(requestParam));
    }
}
