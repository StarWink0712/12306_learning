package com.guoxu.ticketservice.service.Impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.guoxu.DistributedCache;
import com.guoxu.core.CacheLoader;
import com.guoxu.enmus.FlagEnum;
import com.guoxu.exception.ClientException;
import com.guoxu.ticketservice.common.enums.RegionStationQueryTypeEnum;
import com.guoxu.ticketservice.dao.entity.RegionDO;
import com.guoxu.ticketservice.dao.entity.StationDO;
import com.guoxu.ticketservice.dao.mapper.RegionMapper;
import com.guoxu.ticketservice.dao.mapper.StationMapper;
import com.guoxu.ticketservice.dto.req.RegionStationQueryReqDTO;
import com.guoxu.ticketservice.dto.resp.RegionStationQueryRespDTO;
import com.guoxu.ticketservice.dto.resp.StationQueryRespDTO;
import com.guoxu.ticketservice.service.RegionStationService;
import com.guoxu.toolkit.BeanUtil;
import com.guoxu.toolkit.CacheUtil;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.guoxu.ticketservice.common.constant.Index12306Constant.ADVANCE_TICKET_DAY;
import static com.guoxu.ticketservice.common.constant.RedisKeyConstant.*;

/**
 * RegionStationImpl
 * 地区以及车站接口实现层
 * @author 执笔画棠
 * @date 2025/11/13 20:33
 **/
@Service
@RequiredArgsConstructor
public class RegionStationImpl implements RegionStationService {

    // 注入地区数据访问对象，用于数据库操作
    private final RegionMapper regionMapper;
    // 注入站点数据访问对象，用于数据库操作
    private final StationMapper stationMapper;
    // 注入分布式缓存对象，用于操作缓存
    private final DistributedCache distributedCache;
    // 注入RedissonClient，用于获取分布式锁
    private final RedissonClient redissonClient;

    // 根据请求参数列出地区站点信息
    @Override
    public List<RegionStationQueryRespDTO> listRegionStation(RegionStationQueryReqDTO requestParam) {
        String key;
        // 如果请求参数中包含站点名称
        if (StrUtil.isNotBlank(requestParam.getName())) {
            // 构建缓存键，格式为"REGION_STATION + 站点名称"
            key = REGION_STATION + requestParam.getName();
            // 安全地获取地区站点信息，若缓存中没有则从数据库加载并更新缓存
            return safeGetRegionStation(
                    key,
                    () -> {
                        // 构建查询条件，通过站点名称或拼音模糊查询站点数据
                        LambdaQueryWrapper<StationDO> queryWrapper = Wrappers.lambdaQuery(StationDO.class)
                                .likeRight(StationDO::getName, requestParam.getName())
                                .or()
                                .likeRight(StationDO::getSpell, requestParam.getName());
                        // 从数据库中查询符合条件的站点数据列表
                        List<StationDO> stationDOList = stationMapper.selectList(queryWrapper);
                        // 将站点数据列表转换为RegionStationQueryRespDTO类型的JSON字符串
                        return JSON.toJSONString(BeanUtil.convert(stationDOList, RegionStationQueryRespDTO.class));
                    },
                    requestParam.getName());
        }
        // 如果请求参数中不包含站点名称，根据查询类型构建缓存键
        key = REGION_STATION + requestParam.getQueryType();
        LambdaQueryWrapper<RegionDO> queryWrapper = switch (requestParam.getQueryType()) {
            // 如果查询类型为0，查询热门地区
            case 0 -> Wrappers.lambdaQuery(RegionDO.class)
                    .eq(RegionDO::getPopularFlag, FlagEnum.TRUE.code());
            // 如果查询类型为1，查询以A - E开头的地区
            case 1 -> Wrappers.lambdaQuery(RegionDO.class)
                    .in(RegionDO::getInitial, RegionStationQueryTypeEnum.A_E.getSpells());
            // 如果查询类型为2，查询以F - J开头的地区
            case 2 -> Wrappers.lambdaQuery(RegionDO.class)
                    .in(RegionDO::getInitial, RegionStationQueryTypeEnum.F_J.getSpells());
            // 如果查询类型为3，查询以K - O开头的地区
            case 3 -> Wrappers.lambdaQuery(RegionDO.class)
                    .in(RegionDO::getInitial, RegionStationQueryTypeEnum.K_O.getSpells());
            // 如果查询类型为4，查询以P - T开头的地区
            case 4 -> Wrappers.lambdaQuery(RegionDO.class)
                    .in(RegionDO::getInitial, RegionStationQueryTypeEnum.P_T.getSpells());
            // 如果查询类型为5，查询以U - Z开头的地区
            case 5 -> Wrappers.lambdaQuery(RegionDO.class)
                    .in(RegionDO::getInitial, RegionStationQueryTypeEnum.U_Z.getSpells());
            // 如果查询类型不合法，抛出异常
            default -> throw new ClientException("查询失败，请检查查询参数是否正确");
        };
        // 安全地获取地区站点信息，若缓存中没有则从数据库加载并更新缓存
        return safeGetRegionStation(
                key,
                () -> {
                    // 从数据库中查询符合条件的地区数据列表
                    List<RegionDO> regionDOList = regionMapper.selectList(queryWrapper);
                    // 将地区数据列表转换为RegionStationQueryRespDTO类型的JSON字符串
                    return JSON.toJSONString(BeanUtil.convert(regionDOList, RegionStationQueryRespDTO.class));
                },
                String.valueOf(requestParam.getQueryType()));
    }

    // 列出所有站点信息
    @Override
    public List<StationQueryRespDTO> listAllStation() {
        // 从缓存中安全地获取所有站点信息，如果缓存中没有则从数据库加载并缓存
        return distributedCache.safeGet(
                STATION_ALL,
                List.class,
                () -> BeanUtil.convert(stationMapper.selectList(Wrappers.emptyWrapper()), StationQueryRespDTO.class),
                ADVANCE_TICKET_DAY,
                TimeUnit.DAYS);
    }

    // 安全地获取地区站点信息，处理缓存未命中和并发访问的情况
    private List<RegionStationQueryRespDTO> safeGetRegionStation(final String key, CacheLoader<String> loader,
                                                                 String param) {
        List<RegionStationQueryRespDTO> result;
        // 尝试从缓存中获取地区站点信息并转换为列表
        if (CollUtil.isNotEmpty(
                result = JSON.parseArray(distributedCache.get(key, String.class), RegionStationQueryRespDTO.class))) {
            return result;
        }
        // 构建分布式锁的键
        String lockKey = String.format(LOCK_QUERY_REGION_STATION_LIST, param);
        // 获取分布式锁
        RLock lock = redissonClient.getLock(lockKey);
        lock.lock();
        try {
            // 再次检查缓存中是否有数据，因为在获取锁期间可能其他线程已更新缓存
            if (CollUtil.isEmpty(result = JSON.parseArray(distributedCache.get(key, String.class),
                    RegionStationQueryRespDTO.class))) {
                // 从数据库加载地区站点信息并设置到缓存中
                if (CollUtil.isEmpty(result = loadAndSet(key, loader))) {
                    return Collections.emptyList();
                }
            }
        } finally {
            // 释放分布式锁
            lock.unlock();
        }
        return result;
    }

    // 从数据库加载地区站点信息并设置到缓存中
    private List<RegionStationQueryRespDTO> loadAndSet(final String key, CacheLoader<String> loader) {
        // 调用传入的加载器从数据库加载地区站点信息
        String result = loader.load();
        // 如果加载的数据为空，则返回空列表
        if (CacheUtil.isNullOrBlank(result)) {
            return Collections.emptyList();
        }
        // 将加载的JSON字符串转换为RegionStationQueryRespDTO类型的列表
        List<RegionStationQueryRespDTO> respDTOList = JSON.parseArray(result, RegionStationQueryRespDTO.class);
        // 将数据存入缓存
        distributedCache.put(
                key,
                result,
                ADVANCE_TICKET_DAY,
                TimeUnit.DAYS);
        return respDTOList;
    }
}
