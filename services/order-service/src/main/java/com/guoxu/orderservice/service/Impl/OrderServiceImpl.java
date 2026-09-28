package com.guoxu.orderservice.service.Impl;


import cn.crane4j.annotation.AutoOperate;
import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.text.StrBuilder;
import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.guoxu.exception.ClientException;
import com.guoxu.exception.ServiceException;
import com.guoxu.orderservice.common.enums.OrderCanalErrorCodeEnum;
import com.guoxu.orderservice.common.enums.OrderItemStatusEnum;
import com.guoxu.orderservice.common.enums.OrderStatusEnum;
import com.guoxu.orderservice.dao.entity.OrderDO;
import com.guoxu.orderservice.dao.entity.OrderItemDO;
import com.guoxu.orderservice.dao.entity.OrderItemPassengerDO;
import com.guoxu.orderservice.dao.mapper.OrderItemMapper;
import com.guoxu.orderservice.dao.mapper.OrderMapper;
import com.guoxu.orderservice.dto.domain.OrderStatusReversalDTO;
import com.guoxu.orderservice.dto.req.*;
import com.guoxu.orderservice.dto.resp.TicketOrderDetailRespDTO;
import com.guoxu.orderservice.dto.resp.TicketOrderDetailSelfRespDTO;
import com.guoxu.orderservice.dto.resp.TicketOrderPassengerDetailRespDTO;
import com.guoxu.orderservice.mq.event.DelayCloseOrderEvent;
import com.guoxu.orderservice.mq.event.PayResultCallbackOrderEvent;
import com.guoxu.orderservice.mq.produce.DelayCloseOrderSendProduce;
import com.guoxu.orderservice.remote.UserRemoteService;
import com.guoxu.orderservice.remote.dto.UserQueryActualRespDTO;
import com.guoxu.orderservice.service.OrderItemService;
import com.guoxu.orderservice.service.OrderPassengerRelationService;
import com.guoxu.orderservice.service.OrderService;
import com.guoxu.orderservice.service.orderid.OrderIdGeneratorManager;
import com.guoxu.page.PageResponse;
import com.guoxu.result.Result;
import com.guoxu.toolkit.BeanUtil;
import com.guoxu.toolkit.PageUtil;
import core.UserContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.client.producer.SendStatus;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * OrderServiceImpl 订单服务实现类
 * 订单服务实现类
 * 实现订单相关的核心业务逻辑，包括订单创建、查询、取消、状态更新等
 * @author 执笔画棠
 * @date 2025/11/10 20:58
 **/
@Service
@Slf4j
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    // 订单数据访问对象
    private final OrderMapper orderMapper;
    // 订单项数据访问对象
    private final OrderItemMapper orderItemMapper;
    // 订单项服务
    private final OrderItemService orderItemService;
    // 订单乘客关联服务
    private final OrderPassengerRelationService orderPassengerRelationService;
    // Redisson分布式锁客户端
    private final RedissonClient redissonClient;
    // 延迟关闭订单消息生产者
    private final DelayCloseOrderSendProduce delayCloseOrderSendProduce;
    // 用户远程服务
    private final UserRemoteService userRemoteService;

    /**
     * 根据订单号查询票务订单详情
     * 包括订单主信息和乘客详细信息
     * 根据订单号查询票务包含的所有乘客详细信息
     * 有多少订单项，就有多少乘客详细信息
     * @param orderSn 订单号
     * @return 票务订单详情响应DTO
     */
     @Override
     public TicketOrderDetailRespDTO queryTicketOrderByOrderSn(String orderSn){

         //构建订单查询条件：根据订单号查询
         LambdaQueryWrapper<OrderDO> queryWrapper = Wrappers.lambdaQuery(OrderDO.class)
                 .eq(OrderDO::getOrderSn, orderSn);

         //查询主订单信息
         OrderDO orderDO=orderMapper.selectOne(queryWrapper);
         //将订单DO信息转换为响应DTO
         TicketOrderDetailRespDTO result = BeanUtil.convert(orderDO, TicketOrderDetailRespDTO.class);

         // 构建订单项查询条件：根据订单号查询所有订单项
         LambdaQueryWrapper<OrderItemDO> orderItemQueryWrapper = Wrappers.lambdaQuery(OrderItemDO.class)
                 .eq(OrderItemDO::getOrderSn, orderSn);

         // 查询订单项列表
         List<OrderItemDO> orderItemDOList = orderItemMapper.selectList(orderItemQueryWrapper);

         // 将订单项DO列表转换为乘客详情DTO列表，并设置到结果中
         result.setPassengerDetails(BeanUtil.convert(orderItemDOList, TicketOrderPassengerDetailRespDTO.class));
         return result;
     }

    /**
     * 分页查询票务订单
     * 使用@AutoOperate注解自动处理返回结果
     * AutoOperate注解自动处理返回结果，将data.records中的每个元素转换为TicketOrderDetailRespDTO
     * 总的来说就是根据分页请求参数，查询乘客详情
     * 中间通过查订单项，设置到passengerDetails字段中
     *
     * TicketOrderPageQueryReqDTO 票务订单分页查询请求DTO
     * 包含用户ID、状态列表、分页信息（pageNum, pageSize）
     * @param requestParam 票务订单分页查询请求DTO
     * @return 分页响应，包含票务订单详情列表
     */
     @AutoOperate(type = TicketOrderDetailRespDTO.class, on = "data.records")
     @Override
     public PageResponse<TicketOrderDetailRespDTO> pageTicketOrder(TicketOrderPageQueryReqDTO requestParam) {
         // 构建订单查询条件：根据用户ID和状态列表查询，按订单时间倒序排列
         LambdaQueryWrapper<OrderDO> queryWrapper = Wrappers.lambdaQuery(OrderDO.class)
                 .eq(OrderDO::getUserId, requestParam.getUserId())
                 .in(OrderDO::getStatus, buildOrderStatusList(requestParam))
                 .orderByDesc(OrderDO::getOrderTime);
         // 执行分页查询
         IPage<OrderDO> orderPage = orderMapper.selectPage(PageUtil.convert(requestParam), queryWrapper);
         // 转换分页结果，为每个订单补充乘客详情信息，IPage转换为pageResponse
         return PageUtil.convert(orderPage, each -> {
             // 将订单DO转换为响应DTO
             TicketOrderDetailRespDTO result = BeanUtil.convert(each, TicketOrderDetailRespDTO.class);
             // 构建订单项查询条件：根据订单号查询所有订单项
             LambdaQueryWrapper<OrderItemDO> orderItemQueryWrapper = Wrappers.lambdaQuery(OrderItemDO.class)
                     .eq(OrderItemDO::getOrderSn, each.getOrderSn());
             // 查询订单项列表
             List<OrderItemDO> orderItemDOList = orderItemMapper.selectList(orderItemQueryWrapper);
             // 将订单项DO列表转换为乘客详情DTO列表，并设置到结果中,即OrderItemDO转换为TicketOrderPassengerDetailRespDTO
             result.setPassengerDetails(BeanUtil.convert(orderItemDOList, TicketOrderPassengerDetailRespDTO.class));
             return result;
         });
     }

    /**
     * 创建票务订单
     * 包含订单主表、订单项表、乘客关联表的插入，以及发送延迟关闭订单消息
     * 返回生成的订单号，用于后续查询和操作
     * @param requestParam 票务订单创建请求DTO
     * @return 生成的订单号
     */
     @Transactional(rollbackFor = Exception.class)
     @Override
     public String createTicketOrder(TicketOrderCreateReqDTO requestParam){

         //用自己定义的全局唯一id生成器生成一个订单号，其中融入了用户id
         String orderSn= OrderIdGeneratorManager.generateId(requestParam.getUserId());

         // 构建订单主表数据对象,从前端传入的参数构建
         OrderDO orderDO = OrderDO.builder().orderSn(orderSn)
                 .orderTime(requestParam.getOrderTime())
                 .departure(requestParam.getDeparture())
                 .departureTime(requestParam.getDepartureTime())
                 .ridingDate(requestParam.getRidingDate())
                 .arrivalTime(requestParam.getArrivalTime())
                 .trainNumber(requestParam.getTrainNumber())
                 .arrival(requestParam.getArrival())
                 .trainId(requestParam.getTrainId())
                 .source(requestParam.getSource())
                 .status(OrderStatusEnum.PENDING_PAYMENT.getStatus()) // 初始状态为待支付
                 .username(requestParam.getUsername())
                 .userId(String.valueOf(requestParam.getUserId()))
                 .build();

         // 插入订单主表数据
         orderMapper.insert(orderDO);
         //获取票务订单项列表
         List<TicketOrderItemCreateReqDTO> ticketOrderItems=requestParam.getTicketOrderItems();
         //初始化订单项和乘客关联数据对象列表
         List<OrderItemDO> orderItemDOList=new ArrayList<>();
         List<OrderItemPassengerDO> orderPassengerRelationDOList = new ArrayList<>();
         // 遍历票务订单项，构建订单项和乘客关联数据
         // 即将票务订单项转换为订单项和乘客关联数据对象
         ticketOrderItems.forEach(each -> {
             // 构建订单项数据对象
             OrderItemDO orderItemDO = OrderItemDO.builder()
                     .trainId(requestParam.getTrainId())
                     .seatNumber(each.getSeatNumber())
                     .carriageNumber(each.getCarriageNumber())
                     .realName(each.getRealName())
                     .orderSn(orderSn)
                     .phone(each.getPhone())
                     .seatType(each.getSeatType())
                     .username(requestParam.getUsername()).amount(each.getAmount())
                     .carriageNumber(each.getCarriageNumber())
                     .idCard(each.getIdCard())
                     .ticketType(each.getTicketType())
                     .idType(each.getIdType())
                     .userId(String.valueOf(requestParam.getUserId()))
                     .status(0) // 初始状态
                     .build();
             orderItemDOList.add(orderItemDO);
             // 构建乘客关联数据对象
             // 把订单和乘客信息关联起来，乘客信息代表了订单中的一个乘车人，一个订单可以有多个乘车人，也是一个订单项
             OrderItemPassengerDO orderPassengerRelationDO = OrderItemPassengerDO.builder()
                     .idType(each.getIdType())
                     .idCard(each.getIdCard())
                     .orderSn(orderSn)
                     .build();
             orderPassengerRelationDOList.add(orderPassengerRelationDO);
         });

         // 批量插入订单项和乘客关联记录
         orderItemService.saveBatch(orderItemDOList);
         orderPassengerRelationService.saveBatch(orderPassengerRelationDOList);
         try {
             // 构建延迟关闭订单事件，用于在订单创建后延迟关闭订单，确保用户有足够时间完成支付
             DelayCloseOrderEvent delayCloseOrderEvent = DelayCloseOrderEvent.builder()
                     .trainId(String.valueOf(requestParam.getTrainId()))
                     .departure(requestParam.getDeparture())
                     .arrival(requestParam.getArrival())
                     .orderSn(orderSn)
                     .trainPurchaseTicketResults(requestParam.getTicketOrderItems())
                     .build();
             // 发送 RocketMQ 延时消息，指定时间后取消未支付订单
             // 这一步是搞定了。
             // 这里是发送延迟关闭订单消息到 RocketMQ 队列，确保用户有足够时间完成支付
             // sendresult是发送消息的结果对象，包含了消息发送状态和消息ID
             SendResult sendResult = delayCloseOrderSendProduce.sendMessage(delayCloseOrderEvent);
             // 检查消息发送状态，如果发送失败则抛出异常
             if (!Objects.equals(sendResult.getSendStatus(), SendStatus.SEND_OK)) {
                 throw new ServiceException("投递延迟关闭订单消息队列失败");
             }
         } catch (Throwable ex) {
             // 记录消息发送错误日志，但重新抛出异常触发事务回滚
             log.error("延迟关闭订单消息队列发送错误，请求参数：{}", JSON.toJSONString(requestParam), ex);
             throw ex;
         }

         return orderSn;
     }


    /**
     * 关闭票务订单（用于待支付订单的关闭）
     * 取消票务订单请求DTO单一个订单号
     * @param requestParam 取消票务订单请求DTO
     * @return 关闭是否成功
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public boolean closeTickOrder(CancelTicketOrderReqDTO requestParam) {
        String orderSn = requestParam.getOrderSn();
        // 构建查询条件：只查询订单状态
        LambdaQueryWrapper<OrderDO> queryWrapper = Wrappers.lambdaQuery(OrderDO.class)
                .eq(OrderDO::getOrderSn, orderSn)
                .select(OrderDO::getStatus);
        // 查询订单状态
        OrderDO orderDO = orderMapper.selectOne(queryWrapper);
        // 如果订单不存在或状态不是待支付，则返回关闭失败
        if (Objects.isNull(orderDO) || orderDO.getStatus() != OrderStatusEnum.PENDING_PAYMENT.getStatus()) {
            return false;
        }
        // 原则上订单关闭和订单取消这两个方法可以复用，为了区分未来考虑到的场景，这里对方法进行拆分但复用逻辑
        return cancelTickOrder(requestParam);
    }

    /**
     * 取消票务订单
     * 使用分布式锁防止并发取消
     * 传进来的是一个订单号，根据订单号查询订单状态，
     * 如果订单状态不是待支付，则返回取消失败
     * @param requestParam 取消票务订单请求DTO
     * @return 取消是否成功
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public boolean cancelTickOrder(CancelTicketOrderReqDTO requestParam) {
        String orderSn = requestParam.getOrderSn();
        // 构建订单查询条件
        LambdaQueryWrapper<OrderDO> queryWrapper = Wrappers.lambdaQuery(OrderDO.class)
                .eq(OrderDO::getOrderSn, orderSn);
        // 查询订单信息
        OrderDO orderDO = orderMapper.selectOne(queryWrapper);
        // 订单不存在时抛出异常
        if (orderDO == null) {
            throw new ServiceException(OrderCanalErrorCodeEnum.ORDER_CANAL_UNKNOWN_ERROR);
        } else if (orderDO.getStatus() != OrderStatusEnum.PENDING_PAYMENT.getStatus()) {
            // 订单状态不是待支付时抛出异常
            throw new ServiceException(OrderCanalErrorCodeEnum.ORDER_CANAL_STATUS_ERROR);
        }
        // 获取分布式锁，防止同一订单被多个线程同时取消
        RLock lock = redissonClient.getLock(StrBuilder.create("order:canal:order_sn_").append(orderSn).toString());
        if (!lock.tryLock()) {
            // 获取锁失败，说明有其他线程正在处理同一订单
            throw new ClientException(OrderCanalErrorCodeEnum.ORDER_CANAL_REPETITION_ERROR);
        }
        try {
            // 构建订单更新对象，设置状态为已关闭
            OrderDO updateOrderDO = new OrderDO();
            updateOrderDO.setStatus(OrderStatusEnum.CLOSED.getStatus());
            // 构建订单更新条件
            LambdaUpdateWrapper<OrderDO> updateWrapper = Wrappers.lambdaUpdate(OrderDO.class)
                    .eq(OrderDO::getOrderSn, orderSn);
            // 执行订单状态更新
            int updateResult = orderMapper.update(updateOrderDO, updateWrapper);
            if (updateResult <= 0) {
                throw new ServiceException(OrderCanalErrorCodeEnum.ORDER_CANAL_ERROR);
            }
            // 构建订单项更新对象，设置状态为已关闭
            OrderItemDO updateOrderItemDO = new OrderItemDO();
            updateOrderItemDO.setStatus(OrderItemStatusEnum.CLOSED.getStatus());
            // 构建订单项更新条件
            LambdaUpdateWrapper<OrderItemDO> updateItemWrapper = Wrappers.lambdaUpdate(OrderItemDO.class)
                    .eq(OrderItemDO::getOrderSn, orderSn);
            // 执行订单项状态更新
            int updateItemResult = orderItemMapper.update(updateOrderItemDO, updateItemWrapper);
            if (updateItemResult <= 0) {
                throw new ServiceException(OrderCanalErrorCodeEnum.ORDER_CANAL_ERROR);
            }
        } finally {
            // 释放分布式锁
            lock.unlock();
        }
        return true;
    }

    /**
     * 订单状态反转
     * 用于支付成功、退款等场景下的状态更新
     *
     * @param requestParam 订单状态反转DTO
     */
    @Override
    public void statusReversal(OrderStatusReversalDTO requestParam) {
        // 构建订单查询条件
        LambdaQueryWrapper<OrderDO> queryWrapper = Wrappers.lambdaQuery(OrderDO.class)
                .eq(OrderDO::getOrderSn, requestParam.getOrderSn());
        // 查询订单信息
        OrderDO orderDO = orderMapper.selectOne(queryWrapper);
        // 订单不存在时抛出异常
        if (orderDO == null) {
            throw new ServiceException(OrderCanalErrorCodeEnum.ORDER_CANAL_UNKNOWN_ERROR);
        } else if (orderDO.getStatus() != OrderStatusEnum.PENDING_PAYMENT.getStatus()) {
            // 订单状态不是待支付时抛出异常
            throw new ServiceException(OrderCanalErrorCodeEnum.ORDER_CANAL_STATUS_ERROR);
        }
        // 获取分布式锁，防止重复状态修改
        RLock lock = redissonClient.getLock(
                StrBuilder.create("order:status-reversal:order_sn_").append(requestParam.getOrderSn()).toString());
        if (!lock.tryLock()) {
            // 获取锁失败，记录警告日志但不阻塞业务流程
            // 为什么不阻塞业务流程？
            // 因为状态反转是一个异步操作，不影响订单的正常运行
            log.warn("订单重复修改状态，状态反转请求参数：{}", JSON.toJSONString(requestParam));
        }
        try {
            // 构建订单更新对象，设置新的订单状态
            OrderDO updateOrderDO = new OrderDO();
            updateOrderDO.setStatus(requestParam.getOrderStatus());
            // 构建订单更新条件
            LambdaUpdateWrapper<OrderDO> updateWrapper = Wrappers.lambdaUpdate(OrderDO.class)
                    .eq(OrderDO::getOrderSn, requestParam.getOrderSn());
            // 执行订单状态更新
            int updateResult = orderMapper.update(updateOrderDO, updateWrapper);
            if (updateResult <= 0) {
                throw new ServiceException(OrderCanalErrorCodeEnum.ORDER_STATUS_REVERSAL_ERROR);
            }
            // 构建订单项更新对象，设置新的订单项状态
            OrderItemDO orderItemDO = new OrderItemDO();
            orderItemDO.setStatus(requestParam.getOrderItemStatus());
            // 构建订单项更新条件
            LambdaUpdateWrapper<OrderItemDO> orderItemUpdateWrapper = Wrappers.lambdaUpdate(OrderItemDO.class)
                    .eq(OrderItemDO::getOrderSn, requestParam.getOrderSn());
            // 执行订单项状态更新
            int orderItemUpdateResult = orderItemMapper.update(orderItemDO, orderItemUpdateWrapper);
            if (orderItemUpdateResult <= 0) {
                throw new ServiceException(OrderCanalErrorCodeEnum.ORDER_STATUS_REVERSAL_ERROR);
            }
        } finally {
            // 释放分布式锁
            lock.unlock();
        }
    }

    /**
     * 支付回调订单处理
     * 更新订单的支付时间和支付方式
     * 即支付成功后更新订单的支付时间和支付方式
     * @param requestParam 支付结果回调订单事件
     */
    @Override
    public void payCallbackOrder(PayResultCallbackOrderEvent requestParam) {
        // 构建订单更新对象，设置支付时间和支付方式
        OrderDO updateOrderDO = new OrderDO();
        updateOrderDO.setPayTime(requestParam.getGmtPayment());
        updateOrderDO.setPayType(requestParam.getChannel());
        // 构建订单更新条件
        LambdaUpdateWrapper<OrderDO> updateWrapper = Wrappers.lambdaUpdate(OrderDO.class)
                .eq(OrderDO::getOrderSn, requestParam.getOrderSn());
        // 执行订单更新
        int updateResult = orderMapper.update(updateOrderDO, updateWrapper);
        if (updateResult <= 0) {
            throw new ServiceException(OrderCanalErrorCodeEnum.ORDER_STATUS_REVERSAL_ERROR);
        }
    }

    /**
     * 分页查询本人票务订单
     * 根据当前登录用户的身份证号查询相关订单
     * 即查询当前登录用户作为乘车人或被乘车人参与的订单
     * 不查自己订单中的其他乘车人
     *
     * OrderItemPassengerDO对应数据库的t_order_item_passenger表
     * 即订单和乘客的多对多关系
     * 即可通过乘客的idcard查询这个乘客参与的订单
     * @param requestParam 票务订单自助分页查询请求DTO
     * @return 分页响应，包含本人票务订单详情列表
     */
    @Override
    public PageResponse<TicketOrderDetailSelfRespDTO> pageSelfTicketOrder(TicketOrderSelfPageQueryReqDTO requestParam) {
        // 调用远程服务查询当前用户的实际信息，UserQueryActualRespDTO是用户实际信息响应DTO，无脱敏处理
        //无脱敏是因为用户实际信息中包含用户的真实姓名，而不是脱敏后的姓名
        Result<UserQueryActualRespDTO> userActualResp = userRemoteService
                //用自定义用户上下文获取当前登录用户的实际信息，从中获取用户名
                //再根据用户名查询当前登录用户的实际信息
                .queryActualUserByUsername(UserContext.getUsername());
        // 构建乘客关联查询条件：根据身份证号查询，按创建时间倒序排列
        //OrderItemPassengerDO是订单乘客关联实体类，用于存储订单与乘客的关联关系,即通过idcard查询订单中的乘客
        LambdaQueryWrapper<OrderItemPassengerDO> queryWrapper = Wrappers.lambdaQuery(OrderItemPassengerDO.class)
                //getData即UserQueryActualRespDTO，是自定义result中的data字段
                .eq(OrderItemPassengerDO::getIdCard, userActualResp.getData().getIdCard())
                .orderByDesc(OrderItemPassengerDO::getCreateTime);
        // 执行分页查询乘客关联记录
        IPage<OrderItemPassengerDO> orderItemPassengerPage = orderPassengerRelationService
                .page(PageUtil.convert(requestParam), queryWrapper);
        //这些和前面分页查询订单的逻辑相同
        // 转换分页结果，为每个乘客关联记录补充订单和订单项信息
        return PageUtil.convert(orderItemPassengerPage, each -> {
            // 构建订单查询条件：根据订单号查询
            LambdaQueryWrapper<OrderDO> orderQueryWrapper = Wrappers.lambdaQuery(OrderDO.class)
                    .eq(OrderDO::getOrderSn, each.getOrderSn());
            // 查询订单主信息
            OrderDO orderDO = orderMapper.selectOne(orderQueryWrapper);
            // 构建订单项查询条件：根据订单号和身份证号查询
            LambdaQueryWrapper<OrderItemDO> orderItemQueryWrapper = Wrappers.lambdaQuery(OrderItemDO.class)
                    .eq(OrderItemDO::getOrderSn, each.getOrderSn())
                    .eq(OrderItemDO::getIdCard, each.getIdCard());
            // 查询订单项信息
            OrderItemDO orderItemDO = orderItemMapper.selectOne(orderItemQueryWrapper);
            // 将订单DO转换为自助订单详情响应DTO
            TicketOrderDetailSelfRespDTO actualResult = BeanUtil.convert(orderDO, TicketOrderDetailSelfRespDTO.class);
            // 将订单项DO的属性拷贝到结果中，忽略空值和空白
            BeanUtil.convertIgnoreNullAndBlank(orderItemDO, actualResult);
            return actualResult;
        });
    }

    /**
     * 构建订单状态列表
     * 根据前端传入的状态类型返回对应的状态码列表
     * 为什么返回的是list集合？
     * 是因为状态1包含了已支付、部分退款和全额退款三种状态
     * @param requestParam 票务订单分页查询请求DTO
     * @return 订单状态码列表
     */
    private List<Integer> buildOrderStatusList(TicketOrderPageQueryReqDTO requestParam) {
        List<Integer> result = new ArrayList<>();
        // 根据状态类型返回不同的状态列表
        switch (requestParam.getStatusType()) {
            // 状态类型0：待支付
            case 0 -> result = ListUtil.of(
                    OrderStatusEnum.PENDING_PAYMENT.getStatus());
            // 状态类型1：已支付（包括部分退款和全额退款）
            case 1 -> result = ListUtil.of(
                    OrderStatusEnum.ALREADY_PAID.getStatus(),
                    OrderStatusEnum.PARTIAL_REFUND.getStatus(),
                    OrderStatusEnum.FULL_REFUND.getStatus());
            // 状态类型2：已完成
            case 2 -> result = ListUtil.of(
                    OrderStatusEnum.COMPLETED.getStatus());
        }
        return result;
    }

}
