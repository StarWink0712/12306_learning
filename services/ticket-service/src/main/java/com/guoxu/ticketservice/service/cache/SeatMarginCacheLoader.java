package com.guoxu.ticketservice.service.cache;

/**
 * SeatMarginCacheLoader
 *
 * @author 执笔画棠
 * @date 2025/11/12 22:47
 **/

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.guoxu.DistributedCache;
import com.guoxu.ticketservice.common.enums.SeatStatusEnum;
import com.guoxu.ticketservice.common.enums.VehicleTypeEnum;
import com.guoxu.ticketservice.dao.entity.SeatDO;
import com.guoxu.ticketservice.dao.entity.TrainDO;
import com.guoxu.ticketservice.dao.mapper.SeatMapper;
import com.guoxu.ticketservice.dao.mapper.TrainMapper;
import com.guoxu.ticketservice.dto.domain.RouteDTO;
import com.guoxu.ticketservice.service.TrainStationService;
import com.guoxu.toolkit.CacheUtil;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static com.guoxu.ticketservice.common.constant.Index12306Constant.ADVANCE_TICKET_DAY;
import static com.guoxu.ticketservice.common.constant.RedisKeyConstant.*;

/**
 * 座位余量缓存加载
 * 该类的主要功能是加载特定列车、座位类型、出发站和终点站的座位余量缓存数据。
 * 它首先尝试从缓存中获取座位余量信息，如果缓存中没有，则根据列车类型计算相应的座位余量，
 * 并将计算结果存入缓存。在计算座位余量时，会先获取列车信息和站点路线信息，
 * 然后根据不同的列车类型和路线计算每种座位类型的余量。
 * 同时，为了确保在分布式环境下数据加载的安全性，使用了 RedissonClient 获取分布式锁。
 * 最后，通过selectSeatMargin方法查询数据库获取实际的座位余量数量。
 * 此外，代码中还标记了一些待优化或待实现的部分，
 * 如使用 LUA 脚本执行缓存操作和通过列车类型座位枚举重构代码。
 */
@Component
@RequiredArgsConstructor
// SeatMarginCacheLoader类负责加载座位余量缓存数据
public class SeatMarginCacheLoader {

    // 注入TrainMapper，用于操作列车相关数据库
    private final TrainMapper trainMapper;
    // 注入SeatMapper，用于操作座位相关数据库
    private final SeatMapper seatMapper;
    // 注入DistributedCache，用于操作分布式缓存
    private final DistributedCache distributedCache;
    // 注入RedissonClient，用于获取分布式锁
    private final RedissonClient redissonClient;
    // 注入TrainStationService，用于获取列车站点路线信息
    private final TrainStationService trainStationService;

    // 加载特定列车、座位类型、出发站和终点站的座位余量信息
    public Map<String, String> load(String trainId, String seatType, String departure, String arrival) {
        // 创建一个LinkedHashMap用于存储列车站点的剩余车票信息
        Map<String, Map<String, String>> trainStationRemainingTicketMaps = new LinkedHashMap<>();
        // 构建缓存键的后缀
        String keySuffix = CacheUtil.buildKey(trainId, departure, arrival);
        // 获取分布式锁，以确保在分布式环境下数据加载的安全性
        // 这里提到缓存带来的分布式互斥锁还有优化项，可查看指定链接获取详情
        RLock lock = redissonClient.getLock(String.format(LOCK_SAFE_LOAD_SEAT_MARGIN_GET, keySuffix));
        lock.lock();
        try {
            // 从分布式缓存中获取StringRedisTemplate实例，用于操作Redis缓存
            StringRedisTemplate stringRedisTemplate = (StringRedisTemplate) distributedCache.getInstance();
            // 从Redis哈希表中获取特定座位类型的余量信息,这里是双重检查，再查一下缓存中是否有库存信息
            Object quantityObj = stringRedisTemplate.opsForHash().get(TRAIN_STATION_REMAINING_TICKET + keySuffix,
                    seatType);
            // 如果缓存中没有该座位类型的余量信息
            if (CacheUtil.isNullOrBlank(quantityObj)) {
                // 从缓存中安全获取列车信息，如果缓存中没有则从数据库中查询并缓存
                TrainDO trainDO = distributedCache.safeGet(
                        TRAIN_INFO + trainId,
                        TrainDO.class,
                        () -> trainMapper.selectById(trainId),
                        ADVANCE_TICKET_DAY,
                        TimeUnit.DAYS);
                // 获取列车的站点路线信息
                List<RouteDTO> routeDTOList = trainStationService.listTrainStationRoute(trainId,
                        trainDO.getStartStation(), trainDO.getEndStation());
                if (CollUtil.isNotEmpty(routeDTOList)) {
                    // 根据列车类型进行不同的座位余量计算
                    switch (trainDO.getTrainType()) {
                        // 待通过已有列车类型座位枚举重构
                        case 0 -> {
                            for (RouteDTO each : routeDTOList) {
                                // 创建一个LinkedHashMap用于存储当前路线的剩余车票信息
                                Map<String, String> trainStationRemainingTicket = new LinkedHashMap<>();
                                // 计算并存储不同座位类型的余量
                                trainStationRemainingTicket.put("0",
                                        selectSeatMargin(trainId, 0, each.getStartStation(), each.getEndStation()));
                                trainStationRemainingTicket.put("1",
                                        selectSeatMargin(trainId, 1, each.getStartStation(), each.getEndStation()));
                                trainStationRemainingTicket.put("2",
                                        selectSeatMargin(trainId, 2, each.getStartStation(), each.getEndStation()));
                                // 构建实际的缓存键后缀
                                String actualKeySuffix = CacheUtil.buildKey(trainId, each.getStartStation(),
                                        each.getEndStation());
                                // 将当前路线的剩余车票信息存入map
                                trainStationRemainingTicketMaps.put(TRAIN_STATION_REMAINING_TICKET + actualKeySuffix,
                                        trainStationRemainingTicket);
                            }
                        }
                        case 1 -> {
                            for (RouteDTO each : routeDTOList) {
                                Map<String, String> trainStationRemainingTicket = new LinkedHashMap<>();
                                trainStationRemainingTicket.put("3",
                                        selectSeatMargin(trainId, 3, each.getStartStation(), each.getEndStation()));
                                trainStationRemainingTicket.put("4",
                                        selectSeatMargin(trainId, 4, each.getStartStation(), each.getEndStation()));
                                trainStationRemainingTicket.put("5",
                                        selectSeatMargin(trainId, 5, each.getStartStation(), each.getEndStation()));
                                trainStationRemainingTicket.put("13",
                                        selectSeatMargin(trainId, 13, each.getStartStation(), each.getEndStation()));
                                String actualKeySuffix = CacheUtil.buildKey(trainId, each.getStartStation(),
                                        each.getEndStation());
                                trainStationRemainingTicketMaps.put(TRAIN_STATION_REMAINING_TICKET + actualKeySuffix,
                                        trainStationRemainingTicket);
                            }
                        }
                        case 2 -> {
                            for (RouteDTO each : routeDTOList) {
                                Map<String, String> trainStationRemainingTicket = new LinkedHashMap<>();
                                trainStationRemainingTicket.put("6",
                                        selectSeatMargin(trainId, 6, each.getStartStation(), each.getEndStation()));
                                trainStationRemainingTicket.put("7",
                                        selectSeatMargin(trainId, 7, each.getStartStation(), each.getEndStation()));
                                trainStationRemainingTicket.put("8",
                                        selectSeatMargin(trainId, 8, each.getStartStation(), each.getEndStation()));
                                trainStationRemainingTicket.put("13",
                                        selectSeatMargin(trainId, 13, each.getStartStation(), each.getEndStation()));
                                String actualKeySuffix = CacheUtil.buildKey(trainId, each.getStartStation(),
                                        each.getEndStation());
                                trainStationRemainingTicketMaps.put(TRAIN_STATION_REMAINING_TICKET + actualKeySuffix,
                                        trainStationRemainingTicket);
                            }
                        }
                    }
                } else {
                    // 如果没有站点路线信息，则初始化所有座位类型的余量为0
                    Map<String, String> trainStationRemainingTicket = new LinkedHashMap<>();
                    VehicleTypeEnum.findSeatTypesByCode(trainDO.getTrainType())
                            .forEach(each -> trainStationRemainingTicket.put(String.valueOf(each), "0"));
                    trainStationRemainingTicketMaps.put(TRAIN_STATION_REMAINING_TICKET + keySuffix,
                            trainStationRemainingTicket);
                }
                // 待使用LUA脚本执行（目前只是占位注释）
                trainStationRemainingTicketMaps
                        .forEach((cacheKey, cacheMap) -> stringRedisTemplate.opsForHash().putAll(cacheKey, cacheMap));
            }
        } finally {
            // 释放分布式锁
            lock.unlock();
        }
        // 返回特定列车、座位类型、出发站和终点站的剩余车票信息，如果没有则返回空的LinkedHashMap
        return Optional.ofNullable(trainStationRemainingTicketMaps.get(TRAIN_STATION_REMAINING_TICKET + keySuffix))
                .orElse(new LinkedHashMap<>());
    }

    // 计算并返回特定列车、座位类型、出发站和终点站的座位余量
    private String selectSeatMargin(String trainId, Integer type, String departure, String arrival) {
        // 构建查询条件，查询特定列车、座位类型、座位状态为可用、出发站和终点站的座位数量
        LambdaQueryWrapper<SeatDO> queryWrapper = Wrappers.lambdaQuery(SeatDO.class)
                .eq(SeatDO::getTrainId, trainId)
                .eq(SeatDO::getSeatType, type)
                .eq(SeatDO::getSeatStatus, SeatStatusEnum.AVAILABLE.getCode())
                .eq(SeatDO::getStartStation, departure)
                .eq(SeatDO::getEndStation, arrival);
        // 返回查询到的座位数量，如果没有则返回"0"
        return Optional.ofNullable(seatMapper.selectCount(queryWrapper))
                .map(String::valueOf)
                .orElse("0");
    }
}