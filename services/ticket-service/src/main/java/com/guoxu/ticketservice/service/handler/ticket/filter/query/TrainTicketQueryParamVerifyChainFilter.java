package com.guoxu.ticketservice.service.handler.ticket.filter.query;

import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.nacos.shaded.com.google.common.collect.Maps;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.guoxu.DistributedCache;
import com.guoxu.exception.ClientException;
import com.guoxu.ticketservice.dao.entity.RegionDO;
import com.guoxu.ticketservice.dao.entity.StationDO;
import com.guoxu.ticketservice.dao.mapper.RegionMapper;
import com.guoxu.ticketservice.dao.mapper.StationMapper;
import com.guoxu.ticketservice.dto.req.TicketPageQueryReqDTO;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Objects;

import static com.guoxu.ticketservice.common.constant.RedisKeyConstant.LOCK_QUERY_ALL_REGION_LIST;
import static com.guoxu.ticketservice.common.constant.RedisKeyConstant.QUERY_ALL_REGION_LIST;

/**
 * TrainTicketQueryParamVerifyChainFilter
 * 查询列车车票流程过滤器之验证数据是否正确
 * @author 执笔画棠
 * @date 2025/11/13 19:57
 **/
@Component
@RequiredArgsConstructor
// TrainTicketQueryParamVerifyChainFilter类实现了TrainTicketQueryChainFilter接口，
// 用于验证车票查询请求参数中的出发地和目的地是否存在
public class TrainTicketQueryParamVerifyChainFilter implements TrainTicketQueryChainFilter<TicketPageQueryReqDTO> {

    // 注入地区数据访问对象，用于从数据库获取地区相关信息
    private final RegionMapper regionMapper;
    // 注入站点数据访问对象，用于从数据库获取站点相关信息
    private final StationMapper stationMapper;
    // 注入分布式缓存对象，用于操作缓存
    private final DistributedCache distributedCache;
    // 注入RedissonClient，用于获取分布式锁
    private final RedissonClient redissonClient;

    // 缓存数据为空并且已经加载过标识
    private static boolean CACHE_DATA_ISNULL_AND_LOAD_FLAG = false;

    // 处理车票查询请求，验证出发地和目的地是否存在
    @Override
    public void handler(TicketPageQueryReqDTO requestParam) {
        // 从分布式缓存中获取StringRedisTemplate实例，用于操作Redis缓存
        StringRedisTemplate stringRedisTemplate = (StringRedisTemplate) distributedCache.getInstance();
        // 获取用于操作哈希类型缓存的HashOperations对象
        HashOperations<String, Object, Object> hashOperations = stringRedisTemplate.opsForHash();
        // 从缓存中批量获取出发地和目的地对应的信息
        List<Object> actualExistList = hashOperations.multiGet(
                // 查询所有地区列表的键，这个是哈希类型的key，其value是键值对形式
                // 下面从数据库中查完后，就是key是代码，value是车站或目的地的名称
                QUERY_ALL_REGION_LIST,
                // 包含出发地和目的地的列表
                // 这里就是在这个haxh类型缓存中，查value中有没有这两个出发地code和目的地code！
                // 查到了就表明出发地和目的地在缓存中都存在，可以接着买票了！
                ListUtil.toList(requestParam.getFromStation(), requestParam.getToStation()));
        // 统计获取到的信息中为空的数量
        long emptyCount = actualExistList.stream().filter(Objects::isNull).count();
        // 如果为空的数量为0，说明出发地和目的地在缓存中都存在，直接返回
        if (emptyCount == 0L) {
            return;
        }
        // 如果为空的数量为1，或者为空的数量为2且缓存数据为空并且已经加载过标识为true，同时缓存中存在所有地区列表的键
        if (emptyCount == 1L || (emptyCount == 2L && CACHE_DATA_ISNULL_AND_LOAD_FLAG
                && distributedCache.hasKey(QUERY_ALL_REGION_LIST))) {
            // 抛出异常，提示出发地或目的地不存在
            throw new ClientException("出发地或目的地不存在");
        }
        // 获取分布式锁，确保在并发环境下数据的一致性
        RLock lock = redissonClient.getLock(LOCK_QUERY_ALL_REGION_LIST);
        lock.lock();
        try {
            // 再次检查缓存中是否存在所有地区列表的键
            if (distributedCache.hasKey(QUERY_ALL_REGION_LIST)) {
                // 重新从缓存中批量获取出发地和目的地对应的信息
                actualExistList = hashOperations.multiGet(
                        QUERY_ALL_REGION_LIST,
                        ListUtil.toList(requestParam.getFromStation(), requestParam.getToStation()));
                // 统计获取到的信息中不为空的数量
                emptyCount = actualExistList.stream().filter(Objects::nonNull).count();
                // 如果不为空的数量不等于2，说明出发地或目的地有不存在的，抛出异常
                if (emptyCount != 2L) {
                    throw new ClientException("出发地或目的地不存在");
                }
                return;
            }
            // 从数据库中查询所有地区信息
            List<RegionDO> regionDOList = regionMapper.selectList(Wrappers.emptyWrapper());
            // 从数据库中查询所有站点信息
            List<StationDO> stationDOList = stationMapper.selectList(Wrappers.emptyWrapper());
            // 创建一个哈希映射，用于存储地区和站点的代码与名称的对应关系
            HashMap<Object, Object> regionValueMap = Maps.newHashMap();
            // 将地区信息存入哈希映射
            for (RegionDO each : regionDOList) {
                regionValueMap.put(each.getCode(), each.getName());
            }
            // 将站点信息存入哈希映射
            for (StationDO each : stationDOList) {
                regionValueMap.put(each.getCode(), each.getName());
            }
            // 将地区和站点的对应关系批量存入缓存
            /*
             * 这里key是QUERY_ALL_REGION_LIST，但value也是key:value结构，
             * key是地区或站点的代码，value是地区或站点的名称
             */
            hashOperations.putAll(QUERY_ALL_REGION_LIST, regionValueMap);
            // 设置缓存数据为空并且已经加载过标识为true
            CACHE_DATA_ISNULL_AND_LOAD_FLAG = true;
            // 统计哈希映射的键中与出发地或目的地匹配的数量
            emptyCount = regionValueMap.keySet().stream()
                    .filter(each -> StrUtil.equalsAny(each.toString(), requestParam.getFromStation(),
                            requestParam.getToStation()))
                    .count();
            // 如果匹配数量不等于2，说明出发地或目的地有不存在的，抛出异常
            if (emptyCount != 2L) {
                throw new ClientException("出发地或目的地不存在");
            }
        } finally {
            // 释放分布式锁
            lock.unlock();
        }
    }

    // 获取该过滤器在过滤链中的执行顺序
    @Override
    public int getOrder() {
        return 20;
    }
}