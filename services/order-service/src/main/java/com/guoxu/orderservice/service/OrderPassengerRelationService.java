package com.guoxu.orderservice.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.guoxu.orderservice.dao.entity.OrderItemPassengerDO;
import org.springframework.stereotype.Service;

/**
 * OrderPassengerRelationService
 * 乘车人订单关系接口层
 * @author 执笔画棠
 * @date 2025/11/09 21:17
 **/
@Service
public interface OrderPassengerRelationService extends IService<OrderItemPassengerDO> {
}
