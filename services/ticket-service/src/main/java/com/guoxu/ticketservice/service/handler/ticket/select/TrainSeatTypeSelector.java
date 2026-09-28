package com.guoxu.ticketservice.service.handler.ticket.select;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.guoxu.exception.RemoteException;
import com.guoxu.exception.ServiceException;
import com.guoxu.result.Result;
import com.guoxu.strategy.AbstractStrategyChoose;
import com.guoxu.ticketservice.common.enums.VehicleSeatTypeEnum;
import com.guoxu.ticketservice.common.enums.VehicleTypeEnum;
import com.guoxu.ticketservice.dao.entity.TrainStationPriceDO;
import com.guoxu.ticketservice.dao.mapper.TrainStationPriceMapper;
import com.guoxu.ticketservice.dto.domain.PurchaseTicketPassengerDetailDTO;
import com.guoxu.ticketservice.dto.req.PurchaseTicketReqDTO;
import com.guoxu.ticketservice.remote.UserRemoteService;
import com.guoxu.ticketservice.remote.dto.PassengerRespDTO;
import com.guoxu.ticketservice.service.SeatService;
import com.guoxu.ticketservice.service.handler.ticket.dto.SelectSeatDTO;
import com.guoxu.ticketservice.service.handler.ticket.dto.TrainPurchaseTicketRespDTO;
import core.UserContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Collectors;

/**
 * TrainSeatTypeSelector
 * 购票时列车座位选择器
 * @author 执笔画棠
 * @date 2025/11/13 20:07
 **/
@Slf4j
@Component
@RequiredArgsConstructor
// TrainSeatTypeSelector类负责根据不同的座位类型，
/*
 * 该类在整个座位选择流程中起到核心作用。它首先根据座位类型对乘客进行分组，
 * 然后通过线程池并行处理不同座位类型的分配任务。
 * 接着，通过远程调用获取乘客详细信息，
 * 并从数据库查询票价，填充到座位分配结果中。
 * 最后，调用座位服务锁定座位。distributeSeats方法负责具体的座位分配策略选择和执行
 * 实现了高效、灵活的座位分配和相关信息处理。
 */
// 为乘客分配座位，并处理与座位选择、用户信息查询以及价格获取相关的业务逻辑
public final class TrainSeatTypeSelector {

    // 座位服务接口，用于操作座位相关业务，如锁定座位
    private final SeatService seatService;
    // 用户远程服务接口，用于远程调用获取用户相关信息
    private final UserRemoteService userRemoteService;
    // 列车站点价格数据访问对象，用于查询列车站点价格信息
    private final TrainStationPriceMapper trainStationPriceMapper;
    // 抽象策略选择器，用于根据不同的策略选择并执行座位分配逻辑
    private final AbstractStrategyChoose abstractStrategyChoose;
    // 线程池执行器，用于并行处理座位分配任务
    private final ThreadPoolExecutor selectSeatThreadPoolExecutor;

    // 根据列车类型和购票请求参数选择座位
    public List<TrainPurchaseTicketRespDTO> select(Integer trainType, PurchaseTicketReqDTO requestParam) {
        // 获取购票请求中的乘客详细信息列表
        List<PurchaseTicketPassengerDetailDTO> passengerDetails = requestParam.getPassengers();
        // 根据座位类型对乘客详细信息进行分组
        Map<Integer, List<PurchaseTicketPassengerDetailDTO>> seatTypeMap = passengerDetails.stream()
                .collect(Collectors.groupingBy(PurchaseTicketPassengerDetailDTO::getSeatType));
        // 创建一个线程安全的列表，用于存储最终的座位分配结果
        List<TrainPurchaseTicketRespDTO> actualResult = Collections
                .synchronizedList(new ArrayList<>(seatTypeMap.size()));

        // 如果有多种座位类型
        if (seatTypeMap.size() > 1) {
            // 创建一个列表，用于存储每个座位类型分配座位任务的Future对象
            List<Future<List<TrainPurchaseTicketRespDTO>>> futureResults = new ArrayList<>(seatTypeMap.size());
            // 遍历每种座位类型及其对应的乘客详细信息列表
            seatTypeMap.forEach((seatType, passengerSeatDetails) -> {
                // 提交座位分配任务到线程池，并将返回的Future对象添加到列表中
                Future<List<TrainPurchaseTicketRespDTO>> completableFuture = selectSeatThreadPoolExecutor
                        .submit(() -> distributeSeats(trainType, seatType, requestParam, passengerSeatDetails));
                futureResults.add(completableFuture);
            });
            // 并行处理每个座位类型的分配结果，并将其添加到最终结果列表中
            futureResults.parallelStream().forEach(completableFuture -> {
                try {
                    actualResult.addAll(completableFuture.get());
                } catch (Exception e) {
                    // 如果出现异常，抛出服务异常，提示站点余票不足
                    throw new ServiceException("站点余票不足，请尝试更换座位类型或选择其它站点");
                }
            });
        } else {
            // 如果只有一种座位类型
            seatTypeMap.forEach((seatType, passengerSeatDetails) -> {
                // 分配座位并获取结果
                List<TrainPurchaseTicketRespDTO> aggregationResult = distributeSeats(trainType, seatType, requestParam,
                        passengerSeatDetails);
                // 将结果添加到最终结果列表中
                actualResult.addAll(aggregationResult);
            });
        }

        // 如果最终结果为空，或者结果数量与乘客数量不匹配，抛出服务异常
        if (CollUtil.isEmpty(actualResult) || !Objects.equals(actualResult.size(), passengerDetails.size())) {
            throw new ServiceException("站点余票不足，请尝试更换座位类型或选择其它站点");
        }

        // 提取最终结果中所有乘客的ID
        List<String> passengerIds = actualResult.stream()
                .map(TrainPurchaseTicketRespDTO::getPassengerId)
                .collect(Collectors.toList());
        // 定义变量，用于存储远程调用获取的乘客信息结果
        Result<List<PassengerRespDTO>> passengerRemoteResult;
        List<PassengerRespDTO> passengerRemoteResultList;
        try {
            // 远程调用用户服务，根据乘客ID获取乘客详细信息
            passengerRemoteResult = userRemoteService.listPassengerQueryByIds(UserContext.getUsername(), passengerIds);
            // 如果远程调用失败，或者返回的数据为空，抛出远程调用异常
            if (!passengerRemoteResult.isSuccess()
                    || CollUtil.isEmpty(passengerRemoteResultList = passengerRemoteResult.getData())) {
                throw new RemoteException("用户服务远程调用查询乘车人相关信息错误");
            }
        } catch (Throwable ex) {
            // 如果是远程调用异常，记录错误日志
            if (ex instanceof RemoteException) {
                log.error("用户服务远程调用查询乘车人相关信息错误，当前用户：{}，请求参数：{}", UserContext.getUsername(), passengerIds);
            } else {
                // 如果是其他异常，记录详细错误日志
                log.error("用户服务远程调用查询乘车人相关信息错误，当前用户：{}，请求参数：{}", UserContext.getUsername(), passengerIds, ex);
            }
            // 抛出异常
            throw ex;
        }

        // 遍历最终结果，填充乘客的详细信息和票价
        actualResult.forEach(each -> {
            // 获取乘客ID
            String passengerId = each.getPassengerId();
            // 在远程调用返回的乘客信息中找到对应的乘客信息，并填充到结果中
            passengerRemoteResultList.stream()
                    .filter(item -> Objects.equals(item.getId(), passengerId))
                    .findFirst()
                    .ifPresent(passenger -> {
                        each.setIdCard(passenger.getIdCard());
                        each.setPhone(passenger.getPhone());
                        each.setUserType(passenger.getDiscountType());
                        each.setIdType(passenger.getIdType());
                        each.setRealName(passenger.getRealName());
                    });
            // 根据列车ID、出发站、到达站和座位类型查询票价
            LambdaQueryWrapper<TrainStationPriceDO> lambdaQueryWrapper = Wrappers.lambdaQuery(TrainStationPriceDO.class)
                    .eq(TrainStationPriceDO::getTrainId, requestParam.getTrainId())
                    .eq(TrainStationPriceDO::getDeparture, requestParam.getDeparture())
                    .eq(TrainStationPriceDO::getArrival, requestParam.getArrival())
                    .eq(TrainStationPriceDO::getSeatType, each.getSeatType())
                    .select(TrainStationPriceDO::getPrice);
            TrainStationPriceDO trainStationPriceDO = trainStationPriceMapper.selectOne(lambdaQueryWrapper);
            // 设置票价到结果中
            each.setAmount(trainStationPriceDO.getPrice());
        });

        // 锁定座位
        seatService.lockSeat(requestParam.getTrainId(), requestParam.getDeparture(), requestParam.getArrival(),
                actualResult);
        // 返回最终的座位分配结果
        return actualResult;
    }

    // 分配座位的具体方法
    private List<TrainPurchaseTicketRespDTO> distributeSeats(Integer trainType, Integer seatType,
                                                             PurchaseTicketReqDTO requestParam, List<PurchaseTicketPassengerDetailDTO> passengerSeatDetails) {
        // 构建策略键，用于选择合适的座位分配策略
        String buildStrategyKey = VehicleTypeEnum.findNameByCode(trainType)
                + VehicleSeatTypeEnum.findNameByCode(seatType);
        // 创建座位选择DTO对象，包含座位类型、乘客详细信息和购票请求参数
        SelectSeatDTO selectSeatDTO = SelectSeatDTO.builder()
                .seatType(seatType)
                .passengerSeatDetails(passengerSeatDetails)
                .requestParam(requestParam)
                .build();
        try {
            // 根据策略键选择并执行座位分配逻辑
            return abstractStrategyChoose.chooseAndExecuteResp(buildStrategyKey, selectSeatDTO);
        } catch (ServiceException ex) {
            // 如果出现服务异常，抛出新的服务异常，提示当前车次列车类型暂未适配
            throw new ServiceException("当前车次列车类型暂未适配，请购买G35或G39车次");
        }
    }
}