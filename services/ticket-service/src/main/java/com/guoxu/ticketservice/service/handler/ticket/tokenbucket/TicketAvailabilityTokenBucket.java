package com.guoxu.ticketservice.service.handler.ticket.tokenbucket;

import cn.hutool.core.io.resource.ClassPathResource;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Lists;
import com.guoxu.DistributedCache;
import com.guoxu.Singleton;
import com.guoxu.exception.ServiceException;
import com.guoxu.ticketservice.common.enums.VehicleTypeEnum;
import com.guoxu.ticketservice.dao.entity.TrainDO;
import com.guoxu.ticketservice.dao.mapper.TrainMapper;
import com.guoxu.ticketservice.dto.domain.PurchaseTicketPassengerDetailDTO;
import com.guoxu.ticketservice.dto.domain.RouteDTO;
import com.guoxu.ticketservice.dto.domain.SeatTypeCountDTO;
import com.guoxu.ticketservice.dto.req.PurchaseTicketReqDTO;
import com.guoxu.ticketservice.remote.dto.TicketOrderDetailRespDTO;
import com.guoxu.ticketservice.remote.dto.TicketOrderPassengerDetailRespDTO;
import com.guoxu.ticketservice.service.SeatService;
import com.guoxu.ticketservice.service.TrainStationService;
import com.guoxu.ticketservice.service.handler.ticket.dto.TokenResultDTO;
import com.guoxu.toolkit.Assert;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.guoxu.ticketservice.common.constant.Index12306Constant.ADVANCE_TICKET_DAY;
import static com.guoxu.ticketservice.common.constant.RedisKeyConstant.*;

/**
 * TicketAvailabilityTokenBucket
 *
 * @author 执笔画棠
 * @date 2025/11/13 20:12
 **/
@Slf4j
@Component
@RequiredArgsConstructor
public final class TicketAvailabilityTokenBucket {

    // 依赖：车站服务，用于获取车次路线信息
    private final TrainStationService trainStationService;

    // 依赖：分布式缓存（通常是 Redis），用于存储令牌桶等信息
    private final DistributedCache distributedCache;

    // 依赖：Redisson 客户端，用于实现分布式锁
    private final RedissonClient redissonClient;

    // 依赖：座位服务，用于查询每个区间的座位余量
    private final SeatService seatService;

    // 依赖：车次 Mapper，用于查询车次基本信息
    private final TrainMapper trainMapper;

    // Lua 脚本路径：用于从令牌桶中获取令牌（尝试购票）
    private static final String LUA_TICKET_AVAILABILITY_TOKEN_BUCKET_PATH = "lua/ticket_availability_token_bucket.lua";

    // Lua 脚本路径：用于回滚令牌（如订单取消时返还令牌）
    private static final String LUA_TICKET_AVAILABILITY_ROLLBACK_TOKEN_BUCKET_PATH = "lua/ticket_availability_rollback_token_bucket.lua";

    /**
     * 从令牌桶中获取令牌，判断是否允许继续购票流程
     * 返回 true 表示可以继续购票，false 表示令牌不足，不可购票
     */
    public TokenResultDTO takeTokenFromBucket(PurchaseTicketReqDTO requestParam) {
        // 从缓存中获取车次信息，如果缓存没有，则通过 trainMapper 从数据库查询并缓存
        TrainDO trainDO = distributedCache.safeGet(
                TRAIN_INFO + requestParam.getTrainId(), // 缓存 key：车次ID
                TrainDO.class, // 缓存对象类型
                () -> trainMapper.selectById(requestParam.getTrainId()), // 缓存未命中时，通过该 Lambda 从数据库加载
                ADVANCE_TICKET_DAY, // 缓存有效期（比如30天）
                TimeUnit.DAYS // 时间单位
        );

        // 查询该车次从始发站到终点站的所有路线信息（用于后续初始化每个区间的令牌）
        List<RouteDTO> routeDTOList = trainStationService
                .listTrainStationRoute(requestParam.getTrainId(), trainDO.getStartStation(), trainDO.getEndStation());

        // 获取分布式缓存实例（Redis 操作模板）
        StringRedisTemplate stringRedisTemplate = (StringRedisTemplate) distributedCache.getInstance();

        // 构造令牌桶的 Redis Hash 存储 key，格式如：TICKET_AVAILABILITY_TOKEN_BUCKET + trainId
        String tokenBucketHashKey = TICKET_AVAILABILITY_TOKEN_BUCKET + requestParam.getTrainId();

        // 检查该令牌桶 key 是否已经存在（是否已初始化）
        Boolean hasKey = distributedCache.hasKey(tokenBucketHashKey);

        if (!hasKey) {
            // 如果令牌桶还未初始化，则尝试获取分布式锁，防止并发初始化
            RLock lock = redissonClient
                    .getLock(String.format(LOCK_TICKET_AVAILABILITY_TOKEN_BUCKET, requestParam.getTrainId()));
            if (!lock.tryLock()) {
                // 获取锁失败，说明有其他线程正在初始化，抛出异常，让用户稍后重试
                throw new ServiceException("购票异常，请稍候再试");
            }
            try {
                // 再次检查 key 是否存在（双重检查锁定，避免加锁后其他线程已经初始化）
                Boolean hasKeyTwo = distributedCache.hasKey(tokenBucketHashKey);
                if (!hasKeyTwo) {
                    // 获取该车次支持的所有座位类型（根据车次类型，如高铁、动车等）
                    List<Integer> seatTypes = VehicleTypeEnum.findSeatTypesByCode(trainDO.getTrainType());

                    // 用于存储每个区间-座位类型对应的令牌数量，最终会存入 Redis Hash
                    Map<String, String> ticketAvailabilityTokenMap = new HashMap<>();

                    // 遍历每个路线（即每个出发站-到达站区间）
                    for (RouteDTO each : routeDTOList) {
                        // 查询该区间内各座位类型的余票数量
                        List<SeatTypeCountDTO> seatTypeCountDTOList = seatService.listSeatTypeCount(
                                Long.parseLong(requestParam.getTrainId()),
                                each.getStartStation(),
                                each.getEndStation(),
                                seatTypes);

                        // 遍历每个座位类型及其数量
                        for (SeatTypeCountDTO eachSeatTypeCountDTO : seatTypeCountDTOList) {
                            // 构造唯一 key：出发站_到达站_座位类型
                            String buildCacheKey = StrUtil.join("_", each.getStartStation(), each.getEndStation(),
                                    eachSeatTypeCountDTO.getSeatType());
                            // 将余票数量作为 value，存入 map（后续写入 Redis）
                            ticketAvailabilityTokenMap.put(buildCacheKey,
                                    String.valueOf(eachSeatTypeCountDTO.getSeatCount()));
                        }
                    }

                    // 将构造好的令牌数量 map，一次性写入 Redis Hash 中（完成令牌桶初始化）
                    stringRedisTemplate.opsForHash().putAll(
                            TICKET_AVAILABILITY_TOKEN_BUCKET + requestParam.getTrainId(), ticketAvailabilityTokenMap);
                }
            } finally {
                // 释放锁
                lock.unlock();
            }
        }

        // 加载用于获取令牌的 Lua 脚本对象
        DefaultRedisScript<String> actual = Singleton.get(LUA_TICKET_AVAILABILITY_TOKEN_BUCKET_PATH, () -> {
            DefaultRedisScript<String> redisScript = new DefaultRedisScript<>();
            redisScript.setScriptSource(
                    new ResourceScriptSource(new ClassPathResource(LUA_TICKET_AVAILABILITY_TOKEN_BUCKET_PATH)));
            redisScript.setResultType(String.class);
            return redisScript;
        });
        Assert.notNull(actual); // 确保脚本加载成功

        // 统计当前请求中，每个座位类型需要的数量（乘客 -> 座位类型 映射，统计总数）
        Map<Integer, Long> seatTypeCountMap = requestParam.getPassengers().stream()
                .collect(Collectors.groupingBy(PurchaseTicketPassengerDetailDTO::getSeatType, Collectors.counting()));

        // 将座位类型和数量转换为 JSON Array，每个元素是 {"seatType": "1", "count": "2"}
        JSONArray seatTypeCountArray = seatTypeCountMap.entrySet().stream()
                .map(entry -> {
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("seatType", String.valueOf(entry.getKey()));
                    jsonObject.put("count", String.valueOf(entry.getValue()));
                    return jsonObject;
                })
                .collect(Collectors.toCollection(JSONArray::new));

        // 获取该车次的出发-到达路线信息（用于 Lua 脚本定位区间）
        List<RouteDTO> takeoutRouteDTOList = trainStationService
                .listTakeoutTrainStationRoute(requestParam.getTrainId(), requestParam.getDeparture(),
                        requestParam.getArrival());

        // 构造一个 Lua 脚本使用的 key，通常是 出发站_到达站
        String luaScriptKey = StrUtil.join("_", requestParam.getDeparture(), requestParam.getArrival());

        // 执行 Lua 脚本，传入令牌桶 Hash Key、Lua key、座位需求 JSON、路线 JSON
        String resultStr = stringRedisTemplate.execute(actual, Lists.newArrayList(tokenBucketHashKey, luaScriptKey),
                JSON.toJSONString(seatTypeCountArray), JSON.toJSONString(takeoutRouteDTOList));

        // 将 Lua 脚本返回的 JSON 字符串解析为 TokenResultDTO 对象
        TokenResultDTO result = JSON.parseObject(resultStr, TokenResultDTO.class);

        // 如果解析结果为 null，返回一个默认结果，表示令牌为空
        return result == null
                ? TokenResultDTO.builder().tokenIsNull(Boolean.TRUE).build()
                : result;
    }

    /**
     * 回滚令牌（例如订单取消或超时未支付时，返还令牌到令牌桶）
     */
    public void rollbackInBucket(TicketOrderDetailRespDTO requestParam) {
        // 加载用于回滚令牌的 Lua 脚本
        DefaultRedisScript<Long> actual = Singleton.get(LUA_TICKET_AVAILABILITY_ROLLBACK_TOKEN_BUCKET_PATH, () -> {
            DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
            redisScript.setScriptSource(new ResourceScriptSource(new ClassPathResource(LUA_TICKET_AVAILABILITY_ROLLBACK_TOKEN_BUCKET_PATH));
            redisScript.setResultType(Long.class);
            return redisScript;
        });
        Assert.notNull(actual);

        // 统计需要回滚的每个座位类型数量
        Map<Integer, Long> seatTypeCountMap = requestParam.getPassengerDetails().stream()
                .collect(Collectors.groupingBy(TicketOrderPassengerDetailRespDTO::getSeatType, Collectors.counting()));

        // 转换为 JSON Array 格式
        JSONArray seatTypeCountArray = seatTypeCountMap.entrySet().stream()
                .map(entry -> {
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("seatType", String.valueOf(entry.getKey()));
                    jsonObject.put("count", String.valueOf(entry.getValue()));
                    return jsonObject;
                })
                .collect(Collectors.toCollection(JSONArray::new));

        // 获取 Redis 操作模板
        StringRedisTemplate stringRedisTemplate = (StringRedisTemplate) distributedCache.getInstance();

        // 构造令牌桶的 Hash Key
        String actualHashKey = TICKET_AVAILABILITY_TOKEN_BUCKET + requestParam.getTrainId();

        // 构造 Lua 脚本使用的 key（通常是出发站_到达站）
        String luaScriptKey = StrUtil.join("_", requestParam.getDeparture(), requestParam.getArrival());

        // 获取该车次的出发-到达路线信息
        List<RouteDTO> takeoutRouteDTOList = trainStationService.listTakeoutTrainStationRoute(String.valueOf(requestParam.getTrainId()), requestParam.getDeparture(), requestParam.getArrival());

        // 执行 Lua 脚本，进行令牌回滚
        Long result = stringRedisTemplate.execute(actual, Lists.newArrayList(actualHashKey, luaScriptKey), JSON.toJSONString(seatTypeCountArray), JSON.toJSONString(takeoutRouteDTOList));

        // 如果回滚失败（返回 null 或不为 0），记录错误并抛出异常
        if (result == null || !Objects.equals(result, 0L)) {
            log.error("回滚列车余票令牌失败，订单信息：{}", JSON.toJSONString(requestParam));
            throw new ServiceException("回滚列车余票令牌失败");
        }
    }

    /**
     * 删除令牌桶中的令牌数据（一般在令牌与数据库不一致时使用）
     */
    public void delTokenInBucket(PurchaseTicketReqDTO requestParam) {
        // 获取 Redis 操作模板
        StringRedisTemplate stringRedisTemplate = (StringRedisTemplate) distributedCache.getInstance();

        // 构造令牌桶的 Hash Key
        String tokenBucketHashKey = TICKET_AVAILABILITY_TOKEN_BUCKET + requestParam.getTrainId();

        // 从 Redis 中删除该 key（清空令牌桶）
        stringRedisTemplate.delete(tokenBucketHashKey);
    }

    /**
     * （空实现）预留方法：用于手动添加令牌到令牌桶
     */
    public void putTokenInBucket() {
    }

    /**
     * （空实现）预留方法：用于初始化令牌桶
     */
    public void initializeTokens() {
    }
}
