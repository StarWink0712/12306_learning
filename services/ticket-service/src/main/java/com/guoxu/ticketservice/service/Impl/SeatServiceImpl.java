package com.guoxu.ticketservice.service.Impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.guoxu.DistributedCache;
import com.guoxu.ticketservice.common.enums.SeatStatusEnum;
import com.guoxu.ticketservice.dao.entity.SeatDO;
import com.guoxu.ticketservice.dao.mapper.SeatMapper;
import com.guoxu.ticketservice.dto.domain.RouteDTO;
import com.guoxu.ticketservice.dto.domain.SeatTypeCountDTO;
import com.guoxu.ticketservice.service.SeatService;
import com.guoxu.ticketservice.service.TrainStationService;
import com.guoxu.ticketservice.service.handler.ticket.dto.TrainPurchaseTicketRespDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static com.guoxu.ticketservice.common.constant.RedisKeyConstant.TRAIN_STATION_CARRIAGE_REMAINING_TICKET;

/**
 * SeatServiceImpl 座位接口实现层
 *
 * @author 执笔画棠
 * @date 2025/11/13 20:36
 **/
@Service
@RequiredArgsConstructor
public class SeatServiceImpl extends ServiceImpl<SeatMapper, SeatDO> implements SeatService {
    // SeatMapper：用于操作座位数据表
    private final SeatMapper seatMapper;

    // TrainStationService：用于获取车站、路线相关服务
    private final TrainStationService trainStationService;

    // 分布式缓存（如 Redis），用于缓存余票等高频访问数据
    private final DistributedCache distributedCache;

    /**
     * 查询指定车次、车厢、座位类型、起终站的可用座位号列表
     *
     */
    @Override
    public List<String> listAvailableSeat(String trainId, String carriageNumber, Integer seatType, String departure,
                                          String arrival) {
        // 构建查询条件构造器（Lambda 方式，更安全，防止字段写错）
        LambdaQueryWrapper<SeatDO> queryWrapper = Wrappers.lambdaQuery(SeatDO.class)
                // 指定查询的车次 ID
                .eq(SeatDO::getTrainId, trainId)
                // 指定车厢号
                .eq(SeatDO::getCarriageNumber, carriageNumber)
                // 指定座位类型（如二等座、一等座等）
                .eq(SeatDO::getSeatType, seatType)
                // 指定起始站
                .eq(SeatDO::getStartStation, departure)
                // 指定终点站
                .eq(SeatDO::getEndStation, arrival)
                // 只查询状态为可用的座位
                .eq(SeatDO::getSeatStatus, SeatStatusEnum.AVAILABLE.getCode())
                // 只查询座位号字段，优化查询性能（避免查询不必要的字段）
                .select(SeatDO::getSeatNumber);

        // 执行查询，获取符合条件的座位实体列表
        List<SeatDO> seatDOList = seatMapper.selectList(queryWrapper);

        // 将 SeatDO 中的座位号字段提取出来，转为字符串列表返回
        return seatDOList.stream().map(SeatDO::getSeatNumber).collect(Collectors.toList());
    }

    /**
     * 查询指定车次、起终站、多个车厢的剩余票数
     */
    @Override
    public List<Integer> listSeatRemainingTicket(String trainId, String departure, String arrival,
                                                 List<String> trainCarriageList) {
        // 构造缓存 key 的后缀，由 trainId、departure、arrival 拼接而成，用于唯一标识该查询
        String keySuffix = StrUtil.join("_", trainId, departure, arrival);

        // 判断分布式缓存中是否存在该 key（即是否已缓存余票信息）
        if (distributedCache.hasKey(TRAIN_STATION_CARRIAGE_REMAINING_TICKET + keySuffix)) {
            // 强制转换为 StringRedisTemplate，以便操作 Redis Hash 结构
            StringRedisTemplate stringRedisTemplate = (StringRedisTemplate) distributedCache.getInstance();

            // 从 Redis Hash 中批量获取多个车厢的余票信息，key 是车厢号，value 是余票数（String 类型）
            List<Object> trainStationCarriageRemainingTicket = stringRedisTemplate.opsForHash().multiGet(
                    TRAIN_STATION_CARRIAGE_REMAINING_TICKET + keySuffix,
                    Arrays.asList(trainCarriageList.toArray()));

            // 如果缓存中查询到的余票信息不为空
            if (CollUtil.isNotEmpty(trainStationCarriageRemainingTicket)) {
                // 将缓存中的余票信息（Object -> String -> Integer）转换成 Integer 列表并返回
                return trainStationCarriageRemainingTicket.stream()
                        .map(each -> Integer.parseInt(each.toString())) // 每个元素转成 Integer
                        .collect(Collectors.toList());
            }
        }

        // 如果缓存中没有，则构造一个 SeatDO 查询条件对象，只设置必要的字段
        SeatDO seatDO = SeatDO.builder()
                .trainId(Long.parseLong(trainId)) // 设置车次 ID
                .startStation(departure) // 设置起始站
                .endStation(arrival) // 设置终点站
                .build();

        // 调用 Mapper 方法，从数据库中查询多个车厢的剩余票数（未使用缓存，直接查库）
        return seatMapper.listSeatRemainingTicket(seatDO, trainCarriageList);
    }

    /**
     * 查询指定车次、座位类型、起终站的可用车厢号列表（去重，每个车厢只出现一次）
     */
    @Override
    public List<String> listUsableCarriageNumber(String trainId, Integer carriageType, String departure,
                                                 String arrival) {
        // 构建查询条件：查询指定车次、座位类型、起终站、且状态为可用的座位
        LambdaQueryWrapper<SeatDO> queryWrapper = Wrappers.lambdaQuery(SeatDO.class)
                // 指定车次 ID
                .eq(SeatDO::getTrainId, trainId)
                // 指定座位类型（如二等座、一等座等）
                .eq(SeatDO::getSeatType, carriageType)
                // 指定起始站
                .eq(SeatDO::getStartStation, departure)
                // 指定终点站
                .eq(SeatDO::getEndStation, arrival)
                // 只查询可用的座位
                .eq(SeatDO::getSeatStatus, SeatStatusEnum.AVAILABLE.getCode())
                // 按车厢号分组，确保每个车厢只返回一次
                .groupBy(SeatDO::getCarriageNumber)
                // 只查询车厢号字段
                .select(SeatDO::getCarriageNumber);

        // 执行查询，获取每个可用车厢的记录（去重后的）
        List<SeatDO> seatDOList = seatMapper.selectList(queryWrapper);

        // 提取所有车厢号，并转为字符串列表返回
        return seatDOList.stream().map(SeatDO::getCarriageNumber).collect(Collectors.toList());
    }

    /**
     * 查询指定车次、起终站、指定座位类型集合的各类型座位数量统计
     */
    @Override
    public List<SeatTypeCountDTO> listSeatTypeCount(Long trainId, String startStation, String endStation,
                                                    List<Integer> seatTypes) {
        // 直接调用 Mapper 方法，查询指定车次、起终站、座位类型集合的各类型座位数量
        return seatMapper.listSeatTypeCount(trainId, startStation, endStation, seatTypes);
    }

    /**
     * 锁座操作：将用户选择的座位状态设置为 LOCKED（已锁定，占座）
     */
    @Override
    public void lockSeat(String trainId, String departure, String arrival,
                         List<TrainPurchaseTicketRespDTO> trainPurchaseTicketRespList) {
        // 获取该车次、起终站的所有路线信息（用于定位每个座位所属的区间）
        List<RouteDTO> routeList = trainStationService.listTakeoutTrainStationRoute(trainId, departure, arrival);

        // 遍历用户选择的每一个座位信息（每个座位可能属于不同区间）
        trainPurchaseTicketRespList.forEach(each -> routeList.forEach(item -> {
            // 构建更新条件：精确匹配车次、车厢、区间、座位号
            LambdaUpdateWrapper<SeatDO> updateWrapper = Wrappers.lambdaUpdate(SeatDO.class)
                    .eq(SeatDO::getTrainId, trainId)
                    .eq(SeatDO::getCarriageNumber, each.getCarriageNumber())
                    .eq(SeatDO::getStartStation, item.getStartStation())
                    .eq(SeatDO::getEndStation, item.getEndStation())
                    .eq(SeatDO::getSeatNumber, each.getSeatNumber());

            // 构建要更新成的座位状态对象：状态设置为 LOCKED（已锁定）
            SeatDO updateSeatDO = SeatDO.builder()
                    .seatStatus(SeatStatusEnum.LOCKED.getCode())
                    .build();

            // 执行更新：将满足条件的座位状态更新为 LOCKED
            seatMapper.update(updateSeatDO, updateWrapper);
        }));
    }

    /**
     * 解锁座位操作：将之前锁定的座位状态恢复为 AVAILABLE（可用）
     */
    @Override
    public void unlock(String trainId, String departure, String arrival,
                       List<TrainPurchaseTicketRespDTO> trainPurchaseTicketResults) {
        // 获取车次、起终站的所有路线信息
        List<RouteDTO> routeList = trainStationService.listTakeoutTrainStationRoute(trainId, departure, arrival);

        // 遍历用户之前选择的每一个座位（可能分布在多个区间）
        trainPurchaseTicketResults.forEach(each -> routeList.forEach(item -> {
            // 构建更新条件：精确匹配车次、车厢、区间、座位号
            LambdaUpdateWrapper<SeatDO> updateWrapper = Wrappers.lambdaUpdate(SeatDO.class)
                    .eq(SeatDO::getTrainId, trainId)
                    .eq(SeatDO::getCarriageNumber, each.getCarriageNumber())
                    .eq(SeatDO::getStartStation, item.getStartStation())
                    .eq(SeatDO::getEndStation, item.getEndStation())
                    .eq(SeatDO::getSeatNumber, each.getSeatNumber());

            // 构建要更新成的座位状态对象：状态设置为 AVAILABLE（可用）
            SeatDO updateSeatDO = SeatDO.builder()
                    .seatStatus(SeatStatusEnum.AVAILABLE.getCode())
                    .build();

            // 执行更新：将满足条件的座位状态恢复为 AVAILABLE
            seatMapper.update(updateSeatDO, updateWrapper);
        }));
    }
}
