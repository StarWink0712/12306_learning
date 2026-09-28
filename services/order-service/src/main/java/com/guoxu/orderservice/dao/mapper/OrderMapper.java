package com.guoxu.orderservice.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.guoxu.orderservice.dao.entity.OrderDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * OrderMapper
 * 订单持久层
 * @author 执笔画棠
 * @version 2025/11/10 20:21
 **/
@Mapper
public interface OrderMapper extends BaseMapper<OrderDO> {
}