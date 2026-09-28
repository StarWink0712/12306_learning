package com.guoxu.orderservice.service.Impl;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.text.StrBuilder;
import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.guoxu.exception.ServiceException;
import com.guoxu.orderservice.common.enums.OrderCanalErrorCodeEnum;
import com.guoxu.orderservice.dao.entity.OrderDO;
import com.guoxu.orderservice.dao.entity.OrderItemDO;
import com.guoxu.orderservice.dao.mapper.OrderItemMapper;
import com.guoxu.orderservice.dao.mapper.OrderMapper;
import com.guoxu.orderservice.dto.domain.OrderItemStatusReversalDTO;
import com.guoxu.orderservice.dto.req.TicketOrderItemQueryReqDTO;
import com.guoxu.orderservice.dto.resp.TicketOrderPassengerDetailRespDTO;
import com.guoxu.orderservice.service.OrderItemService;
import com.guoxu.toolkit.BeanUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.transaction.annotation.Transactional;

import java.io.Serial;
import java.util.List;

/**
 * OrderItemServiceImpl 订单项服务实现类
 * 继承MyBatis-Plus的ServiceImpl，实现OrderItemService接口
 * 负责订单项相关的业务逻辑处理
 * @author 执笔画棠
 * @date 2025/11/10 20:17
 **/
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderItemServiceImpl extends ServiceImpl<OrderItemMapper, OrderItemDO> implements OrderItemService {
    
    //订单数据访问对象，用来操作订单主表
    private final OrderMapper orderMapper;

    //订单项数据访问对象，用于操作订单项表
    private final OrderItemMapper orderItemMapper;

    //redisson客户端，用于分布式锁操作
    private final RedissonClient redissonClient;


    /**
     * 订单项状态反转方法
     * 用于更新订单和订单项的状态，支持部分退款和全额退款等场景
     * 使用分布式锁防止并发修改
     *
     * @param requestParam 订单项状态反转请求参数DTO
     */
    @Override
    @Transactional
    public void orderItemStatusReversal(OrderItemStatusReversalDTO requestParam) {
        //构建订单查询条件，根据订单号查询
        /*
         * LambdaQueryWrapper是mybatis-plus提供的查询条件构造器
         * 构建订单查询条件，根据订单号查询订单主表信息
         * 调用 .eq(...) 方法添加了一个 “等于” 条件：OrderDO::getOrderSn 是 Lambda 方法引用，
         * 表示引用 OrderDO 实体中与 “订单编号” 对应的字段（通过 getter 方法关联，避免直接写字符串字段名，
         * 如 "order_sn"）；第二个参数 requestParam.getOrderSn() 是要匹配的值。
         * 最终这个 queryWrapper 会被 MyBatis-Plus 解析为 SQL 中的 WHERE order_sn = ? 条件（
         * order_sn 是 OrderDO 中 orderSn 字段对应的数据库列名）。
         */
        LambdaQueryWrapper<OrderDO> queryWrapper = Wrappers.lambdaQuery(OrderDO.class)
                .eq(OrderDO::getOrderSn,requestParam.getOrderSn());

        //根据订单号，查询订单主表信息，把查询结果封装到orderDO对象中
        OrderDO orderDO=orderMapper.selectOne(queryWrapper);

        //如果订单不存在，则抛出业务异常
        if(orderDO==null){
            throw new ServiceException(OrderCanalErrorCodeEnum.ORDER_CANAL_UNKNOWN_ERROR);
        }

        //获取分布式锁，仿真同一订单被多个服务实例同时修改
        //锁的格式是order:status-reversal:order_sn_订单号
        RLock Lock=redissonClient.getLock(
                StrBuilder.create("order:status-reversal:order_sn_").append(requestParam.getOrderSn()).toString());

        //常数获取锁，如果获取失败就表示其他服务进程正在处理同一订单
        if (!Lock.tryLock()){
            log.warn("订单重复修改状态，状态反转请求参数：{}", JSON.toJSONString(requestParam));
        }

        //在finally块中释放锁，确保锁的释放
        try{
            //创建订单更新对象
            OrderDO updateOrderDO=new OrderDO();
            //设置新的订单状态
            updateOrderDO.setStatus(requestParam.getOrderStatus());

            //设置订单更新条件，根据订单号更新
            LambdaUpdateWrapper<OrderDO> updateWrapper=Wrappers.lambdaUpdate(OrderDO.class)
                    .eq(OrderDO::getOrderSn, requestParam.getOrderSn());
            //执行订单更新操作
            int orderUpdateResult=orderMapper.update(updateOrderDO,updateWrapper);

            //如果更新影响行数小于0，说明更新失败，抛出业务异常
            if(orderUpdateResult<=0){
                throw new ServiceException(OrderCanalErrorCodeEnum.ORDER_STATUS_REVERSAL_ERROR);
            }

            //检查请求参数中是否包含需要更新的订单项列表
            if(CollectionUtil.isNotEmpty(requestParam.getOrderItemDOList())){
                //获取订单项列表
                List<OrderItemDO> orderItemDOList=requestParam.getOrderItemDOList();

                //再次检查订单项列表是否为空，双重检查确保安全
                if(CollectionUtil.isNotEmpty(orderItemDOList)){
                    //遍历订单项列表，逐个更新状态
                    orderItemDOList.forEach(o ->{
                        //创建订单项更新对象，=
                        OrderItemDO orderItemDO=new OrderItemDO();
                        //设置新的订单项状态
                        orderItemDO.setStatus(requestParam.getOrderItemStatus());

                        //构建订单项更新条件，根据订单号和乘客真实姓名更新
                        LambdaUpdateWrapper<OrderItemDO> orderItemUpdateWrapper= Wrappers
                                .lambdaUpdate(OrderItemDO.class)
                                .eq(OrderItemDO::getOrderSn, requestParam.getOrderSn())
                                .eq(OrderItemDO::getRealName, o.getRealName());
                        //执行订单项更新操作
                        int orderItemUpdateResult = orderItemMapper.update(orderItemDO, orderItemUpdateWrapper);

                        // 如果更新影响行数小于等于0，说明更新失败，抛出业务异常
                        if (orderItemUpdateResult <= 0) {
                            throw new ServiceException(OrderCanalErrorCodeEnum.ORDER_ITEM_STATUS_REVERSAL_ERROR);
                        }
                    });
                }
            }
        }finally {
                //释放分布式锁，确保资源的及时释放
                Lock.unlock();
        }
    }

    /**
     * 根据订单号和订单项ID列表查询票务订单乘客详情
     * 用于获取指定订单项对应的乘客详细信息
     *
     * @param requestParam 票务订单项查询请求DTO
     * @return 乘客详情响应DTO列表
     */
    @Override
    public List<TicketOrderPassengerDetailRespDTO> queryTicketItemOrderById(TicketOrderItemQueryReqDTO requestParam) {
        // 构建订单项查询条件：根据订单号和订单项ID列表查询
        LambdaQueryWrapper<OrderItemDO> queryWrapper = Wrappers.lambdaQuery(OrderItemDO.class)
                .eq(OrderItemDO::getOrderSn, requestParam.getOrderSn())
                .in(OrderItemDO::getId, requestParam.getOrderItemRecordIds());

        // 执行查询，获取订单项数据对象列表
        List<OrderItemDO> orderItemDOList = orderItemMapper.selectList(queryWrapper);

        // 将订单项数据对象列表转换为乘客详情响应DTO列表
        return BeanUtil.convert(orderItemDOList, TicketOrderPassengerDetailRespDTO.class);
    }
}
