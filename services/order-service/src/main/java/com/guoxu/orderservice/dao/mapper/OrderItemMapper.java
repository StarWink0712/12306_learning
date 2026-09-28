package com.guoxu.orderservice.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.guoxu.orderservice.dao.entity.OrderItemDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * OrderItemMapper
 *
 * @author 执笔画棠
 * @version 2025/11/10 20:20
 **/
@Mapper
public interface OrderItemMapper extends BaseMapper<OrderItemDO> {
}