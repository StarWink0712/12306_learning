package com.guoxu.ticketservice.service.Impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.collect.Lists;
import com.guoxu.ApplicationContextHolder;
import com.guoxu.DistributedCache;
import com.guoxu.annotation.Idempotent;
import com.guoxu.chain.AbstractChainContext;
import com.guoxu.enums.IdempotentSceneEnum;
import com.guoxu.enums.IdempotentTypeEnum;
import com.guoxu.exception.ServiceException;
import com.guoxu.result.Result;
import com.guoxu.ticketservice.common.enums.*;
import com.guoxu.ticketservice.dao.entity.*;
import com.guoxu.ticketservice.dao.mapper.*;
import com.guoxu.ticketservice.dto.domain.*;
import com.guoxu.ticketservice.dto.req.*;
import com.guoxu.ticketservice.dto.resp.RefundTicketRespDTO;
import com.guoxu.ticketservice.dto.resp.TicketOrderDetailRespDTO;
import com.guoxu.ticketservice.dto.resp.TicketPageQueryRespDTO;
import com.guoxu.ticketservice.dto.resp.TicketPurchaseRespDTO;
import com.guoxu.ticketservice.remote.PayRemoteService;
import com.guoxu.ticketservice.remote.TicketOrderRemoteService;


import com.guoxu.ticketservice.remote.dto.*;
import com.guoxu.ticketservice.service.SeatService;
import com.guoxu.ticketservice.service.TicketService;
import com.guoxu.ticketservice.service.TrainStationService;
import com.guoxu.ticketservice.service.cache.SeatMarginCacheLoader;
import com.guoxu.ticketservice.service.handler.ticket.dto.TokenResultDTO;
import com.guoxu.ticketservice.service.handler.ticket.dto.TrainPurchaseTicketRespDTO;
import com.guoxu.ticketservice.service.handler.ticket.select.TrainSeatTypeSelector;
import com.guoxu.ticketservice.service.handler.ticket.tokenbucket.TicketAvailabilityTokenBucket;
import com.guoxu.ticketservice.toolkit.DateUtil;
import com.guoxu.ticketservice.toolkit.TimeStringComparator;
import com.guoxu.toolkit.BeanUtil;
import com.guoxu.toolkit.CacheUtil;
import core.UserContext;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.opengoofy.index12306.framework.starter.log.annotation.ILog;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import static com.guoxu.ticketservice.common.constant.Index12306Constant.ADVANCE_TICKET_DAY;
import static com.guoxu.ticketservice.common.constant.RedisKeyConstant.*;
import static com.guoxu.ticketservice.toolkit.DateUtil.convertDateToLocalTime;

/**
 * TicketServiceImpl 车票接口实现层
 *
 * @author 执笔画棠
 * @date 2025/11/13 20:39
 **/
@Slf4j
@Service
@RequiredArgsConstructor
public class TicketServiceImpl extends ServiceImpl<TicketMapper, TicketDO> implements TicketService, CommandLineRunner {
    // 注入列车数据访问对象，用于操作列车相关数据库表
    private final TrainMapper trainMapper;
    // 注入列车站点关系数据访问对象，用于操作列车站点关系相关数据库表
    private final TrainStationRelationMapper trainStationRelationMapper;
    // 注入列车站点价格数据访问对象，用于操作列车站点价格相关数据库表
    private final TrainStationPriceMapper trainStationPriceMapper;
    // 注入分布式缓存对象，用于操作缓存
    private final DistributedCache distributedCache;
    // 注入车票订单远程服务对象，用于远程调用车票订单相关服务
    private final TicketOrderRemoteService ticketOrderRemoteService;
    // 注入支付远程服务对象，用于远程调用支付相关服务
    private final PayRemoteService payRemoteService;
    // 注入站点数据访问对象，用于操作站点相关数据库表
    private final StationMapper stationMapper;
    // 注入座位服务对象，用于操作座位相关业务
    private final SeatService seatService;
    // 注入列车站点服务对象，用于操作列车站点相关业务
    private final TrainStationService trainStationService;
    // 注入列车座位类型选择器对象，用于选择列车座位类型及相关分配逻辑
    private final TrainSeatTypeSelector trainSeatTypeSelector;
    // 注入座位余量缓存加载器对象，用于加载座位余量缓存数据
    private final SeatMarginCacheLoader seatMarginCacheLoader;
    // 注入车票查询责任链上下文对象，用于处理车票查询相关的责任链逻辑
    private final AbstractChainContext<TicketPageQueryReqDTO> ticketPageQueryAbstractChainContext;
    // 注入购票责任链上下文对象，用于处理购票相关的责任链逻辑
    private final AbstractChainContext<PurchaseTicketReqDTO> purchaseTicketAbstractChainContext;
    // 注入退票责任链上下文对象，用于处理退票相关的责任链逻辑
    private final AbstractChainContext<RefundTicketReqDTO> refundReqDTOAbstractChainContext;
    // 注入Redisson客户端对象，用于获取分布式锁等操作
    private final RedissonClient redissonClient;
    // 注入可配置环境对象，用于获取配置文件中的属性值
    private final ConfigurableEnvironment environment;
    // 注入车票可用性令牌桶对象，可能用于限制车票查询频率等
    private final TicketAvailabilityTokenBucket ticketAvailabilityTokenBucket;
    // 车票服务对象，这里先声明，后续可能会进行初始化或赋值操作
    private TicketService ticketService;

    // 从配置文件中获取车票可用性缓存更新类型，若未配置则为空字符串
    @Value("${ticket.availability.cache-update.type:}")
    private String ticketAvailabilityCacheUpdateType;
    // 从配置文件中获取缓存Redis前缀，若未配置则为空字符串
    @Value("${framework.cache.redis.prefix:}")
    private String cacheRedisPrefix;

    // 分页查询车票信息的方法（版本1）
    /*
     * 这段代码实现了车票分页查询的功能，通过责任链模式进行前置条件验证，
     * 从缓存和数据库中获取并处理列车、站点、价格等信息，
     * 最终构建并返回包含列车列表及相关信息的响应对象。在处理过程中，
     * 使用了分布式锁来确保并发环境下缓存操作的一致性，并对数据进行了缓存管理以提高性能。
     *
     * 前端传过来的是 出发地，目的地，出发站点，目的站点，出发日期，根据这些信息
     * 根据这些信息查询可买的车票，即TicketPageQueryRespDTO
     * 其包含TicketListDTO车次集合，即有哪些车票
     * TicketListDTO，因为两站之间肯定不止一班车，包含座位类型，价格，余票数
     * ；Integer车次类型，比如D-动车 Z-直达
     * 出发车站集合，到达车站集合，车次席别集合即一等二等
     *
     * 通过过滤器处理查询参数，验证出发地、目的地、出发站点、目的站点是否存在，
     * 并验证出发日期是否小于当前日期等。若验证失败，则直接返回错误响应。
     *
     *
     */
    @Override
    public TicketPageQueryRespDTO pageListTicketQueryV1(TicketPageQueryReqDTO requestParam) {
        // 使用责任链模式验证城市名称是否存在，若不存在则加载缓存，同时验证出发日期不能小于当前日期等
        ticketPageQueryAbstractChainContext.handler(TicketChainMarkEnum.TRAIN_QUERY_FILTER.name(), requestParam);
        // 从分布式缓存中获取StringRedisTemplate实例，用于操作Redis缓存
        StringRedisTemplate stringRedisTemplate = (StringRedisTemplate) distributedCache.getInstance();
        // 列车查询逻辑较为复杂，详细解析文章查看 https://nageoffer.com/12306/question
        // v1版本存在严重的性能深渊问题，v2版本完美的解决了该问题。通过Jmeter压测聚合报告得知，性能提升在300% - 500%+


        /*
         * 从缓存中批量获取出发站和到达站的详细信息
         * REGION_TRAIN_STATION_MAPPING是站点与地区的映射，
         * key是地区，value是这个地区中的站点
         * Lists.newArrayList(requestParam.getFromStation(), requestParam.getToStation())
         * 这里就是根据出发地和目的地找这两个地区中的站点
         */
        List<Object> stationDetails = stringRedisTemplate.opsForHash()
                .multiGet(REGION_TRAIN_STATION_MAPPING,
                        Lists.newArrayList(requestParam.getFromStation(), requestParam.getToStation()));
        // 统计获取到的车站详细信息中为空的数量
        long count = stationDetails.stream().filter(Objects::isNull).count();
        // 如果存在为空的车站详细信息

        /*
         * 这里就是缓存中查不到地区中的站点，就要从数据库中找
         * 这里用分布式锁，只允许一个线程去查询数据库，其他线程等待
         * 再用双重检查，一个线程在等待分布式锁时，其他线程处理好了就不需要再查询数据库了
         * 然后从数据库中查询出所有站点信息StationDO，包含车站编码车站名称，地区编码地区名称
         * 然后用一个map集合，key存车站code，value存地区名称，这样就得到了车站和地区的关系表
         * 再存到缓存里，用哈希型数据结构，key是REGION_TRAIN_STATION_MAPPING即地区车站关系表，value是map
         * 然后就是从这个map中，根据传入的出发站点code和到达站点code（即map的key），取出value，即出发达到地区名称
         * 存到stationDetails，再下面就是查询座位了，根据这个
         */
        if (count > 0) {
            // 获取分布式锁，确保在并发环境下数据的一致性
            RLock lock = redissonClient.getLock(LOCK_REGION_TRAIN_STATION_MAPPING);
            lock.lock();
            try {
                // 再次从缓存中批量获取出发站和到达站的详细信息
                stationDetails = stringRedisTemplate.opsForHash()
                        .multiGet(REGION_TRAIN_STATION_MAPPING,
                                Lists.newArrayList(requestParam.getFromStation(), requestParam.getToStation()));
                // 重新统计获取到的车站详细信息中为空的数量
                count = stationDetails.stream().filter(Objects::isNull).count();
                // 如果仍然存在为空的车站详细信息
                if (count > 0) {
                    // 从数据库中查询所有站点信息
                    List<StationDO> stationDOList = stationMapper.selectList(Wrappers.emptyWrapper());
                    // 创建一个用于存储地区与列车站点映射关系的Map
                    Map<String, String> regionTrainStationMap = new HashMap<>();
                    // 遍历站点信息，将站点代码和地区名称存入映射Map
                    stationDOList.forEach(each -> regionTrainStationMap.put(each.getCode(), each.getRegionName()));
                    // 将||地区与列车站点的映射关系||批量存入缓存
                    stringRedisTemplate.opsForHash().putAll(REGION_TRAIN_STATION_MAPPING, regionTrainStationMap);
                    // 重新构建车站详细信息列表，从新存入缓存的映射Map中获取出发站和到达站的信息
                    stationDetails = new ArrayList<>();
                    // 从映射Map中获取出发站和到达站的地区名称，并添加到结果列表中
                    stationDetails.add(regionTrainStationMap.get(requestParam.getFromStation()));
                    stationDetails.add(regionTrainStationMap.get(requestParam.getToStation()));
                }
            } finally {
                // 释放分布式锁
                lock.unlock();
            }
        }

        /*
         * 衔接上面，这里通过出发地区名称和到达地区名称，构建哈希键
         * stationDetails.get(0)是出发地区名称
         * stationDetails.get(1)是到达地区名称
         * 然后从缓存中根据该哈希键，获取对应的缓存数据，这个也是哈希型数据结构
         * key是前面构建的哈希键，value是个map
         * 再看查询出的map是不是空，是空就还从数据库中查询数据
         * 依然用分布式锁和双重检查。
         * 到了数据库中，根据出发地区名称和到达地区名称，查询TrainStationRelationDO
         * 再根据其车次id，查询TrainDO
         * 查询出所有符合的车次，得到trainStationRelationList
         * 然后遍历里面的每一个车次，根据trainid，从缓存中再查询车次traindo
         * 然后再创建一个TicketListDTO对象，表示这个车次能卖的车票的信息
         * 最后得到seatResults，表示出发地区到到达地区的所有车次的所有座位信息
         * 最后把缓存数据regionTrainStationAllMap填充好，key是车次id+出发站地区名称+到达站地区名称
         * value是TicketListDTO对象的json字符串，一个列车车次id对应一个TicketListDTO对象
         * seatResults对应的是两个地区的所有车次的所有TicketListDTO对象
         * 到这就把重复地区到到达地区所包含的车次信息和车票信息填入了缓存。
         * 然后把seatResults所有车次的TicketListDTO对象传到下面的逻辑中
         */
        // 创建一个用于存储座位查询结果的列表
        List<TicketListDTO> seatResults = new ArrayList<>();
        // 构建地区与列车站点哈希键，格式为：REGION_TRAIN_STATION + 出发站地区名称 + 到达站地区名称
        String buildRegionTrainStationHashKey = String.format(REGION_TRAIN_STATION, stationDetails.get(0),
                stationDetails.get(1));
        // 获取该哈希键对应的所有缓存数据
        Map<Object, Object> regionTrainStationAllMap = stringRedisTemplate.opsForHash()
                .entries(buildRegionTrainStationHashKey);
        // 如果缓存数据为空
        if (MapUtil.isEmpty(regionTrainStationAllMap)) {
            // 获取分布式锁，确保在并发环境下数据的一致性
            RLock lock = redissonClient.getLock(LOCK_REGION_TRAIN_STATION);
            lock.lock();
            try {
                // 再次获取该哈希键对应的所有缓存数据
                regionTrainStationAllMap = stringRedisTemplate.opsForHash().entries(buildRegionTrainStationHashKey);
                // 如果缓存数据仍然为空
                if (MapUtil.isEmpty(regionTrainStationAllMap)) {
                    // 构建查询条件，查询从指定出发地区到指定到达地区的列车站点关系
                    LambdaQueryWrapper<TrainStationRelationDO> queryWrapper = Wrappers
                            .lambdaQuery(TrainStationRelationDO.class)
                            .eq(TrainStationRelationDO::getStartRegion, stationDetails.get(0))
                            .eq(TrainStationRelationDO::getEndRegion, stationDetails.get(1));
                    // 从数据库中查询符合条件的列车站点关系列表
                    List<TrainStationRelationDO> trainStationRelationList = trainStationRelationMapper
                            .selectList(queryWrapper);
                    // 遍历列车站点关系列表
                    for (TrainStationRelationDO each : trainStationRelationList) {
                        // 从缓存中安全获取列车信息，若缓存不存在则从数据库加载并缓存
                        TrainDO trainDO = distributedCache.safeGet(
                                TRAIN_INFO + each.getTrainId(),
                                TrainDO.class,
                                () -> trainMapper.selectById(each.getTrainId()),
                                ADVANCE_TICKET_DAY,
                                TimeUnit.DAYS);
                        // 创建一个车票列表DTO对象，用于存储列车相关信息
                        TicketListDTO result = new TicketListDTO();
                        // 设置列车ID
                        result.setTrainId(String.valueOf(trainDO.getId()));
                        // 设置列车车次
                        result.setTrainNumber(trainDO.getTrainNumber());
                        // 将出发时间转换为指定格式的字符串
                        result.setDepartureTime(convertDateToLocalTime(each.getDepartureTime(), "HH:mm"));
                        // 将到达时间转换为指定格式的字符串
                        result.setArrivalTime(convertDateToLocalTime(each.getArrivalTime(), "HH:mm"));
                        // 计算列车行驶时长
                        result.setDuration(
                                DateUtil.calculateHourDifference(each.getDepartureTime(), each.getArrivalTime()));
                        // 设置出发站
                        result.setDeparture(each.getDeparture());
                        // 设置到达站
                        result.setArrival(each.getArrival());
                        // 设置出发标志
                        result.setDepartureFlag(each.getDepartureFlag());
                        // 设置到达标志
                        result.setArrivalFlag(each.getArrivalFlag());
                        // 设置列车类型
                        result.setTrainType(trainDO.getTrainType());
                        // 设置列车品牌
                        result.setTrainBrand(trainDO.getTrainBrand());
                        // 如果列车标签不为空，将其拆分并设置为列车标签列表
                        if (StrUtil.isNotBlank(trainDO.getTrainTag())) {
                            result.setTrainTags(StrUtil.split(trainDO.getTrainTag(), ","));
                        }
                        // 计算列车行驶天数
                        long betweenDay = cn.hutool.core.date.DateUtil.betweenDay(each.getDepartureTime(),
                                each.getArrivalTime(), false);
                        result.setDaysArrived((int) betweenDay);
                        // 设置车票销售状态，根据当前时间与列车开售时间比较
                        result.setSaleStatus(new Date().after(trainDO.getSaleTime()) ? 0 : 1);
                        // 将列车开售时间转换为指定格式的字符串
                        result.setSaleTime(convertDateToLocalTime(trainDO.getSaleTime(), "MM-dd HH:mm"));
                        // 将该列车信息添加到座位查询结果列表
                        seatResults.add(result);
                        // 将列车信息存入缓存，构建缓存键格式为：列车ID_出发站_到达站，value是TicketListDTO对象的json字符串
                        regionTrainStationAllMap.put(CacheUtil.buildKey(String.valueOf(each.getTrainId()),
                                each.getDeparture(), each.getArrival()), JSON.toJSONString(result));
                    }
                    // 将构建好的缓存数据批量存入缓存
                    stringRedisTemplate.opsForHash().putAll(buildRegionTrainStationHashKey, regionTrainStationAllMap);
                }
            } finally {
                // 释放分布式锁
                lock.unlock();
            }
        }

        /*
         * 从缓存中获取所有出发地区到到达地区的所有车次的所有车票信息
         * 上面的逻辑查询到了trainDO，创建了TicketListDTO对象，
         * 并将其添加到了seatResults列表中。如果为空了，再从上面的缓存中取出来就行了
         *
         * 然后要查询TicketListDTO这个代表的车次的车票的价格，从缓存中获取
         * key是TRAIN_STATION_PRICE+trainId+departure+arrival
         * 即前缀加车次id加出发站加到达站，缓存中没有就去数据库查
         * 这里是用for循环遍历seatResults列表中的每个TicketListDTO对象
         * 对每个TicketListDTO对象，都要查询它的价格，并且是一个对象一次redis查询
         * 因此在V2版本中，通过管道操作把所有的查询请求都放到管道中，一次性发送给redis
         *
         * 查询之后得到TrainStationPriceDO对象，代表一个座位类型的价格
         * 一个车次有多种座位类型，因此用一个list集合包装TrainStationPriceDO对象
         * 然后再遍历这个集合中的TrainStationPriceDO对象，从中获取座位类型，即seattype
         * 然后再构建新的缓存键，由缓存余票查询TRAIN_STATION_REMAINING_TICKET+keySuffix组成
         * keySuffix由车次id等信息组成，即trainId+departure+arrival。
         * 然后在缓存中传入这个key和seatType，查询这个座位类型的余票数量
         * 这里一个TicketListDTO车次对应多个TrainStationPriceDO对象，每个对象代表一个座位类型的价格
         * 因此trainStationPriceDOList.forEach这里又通过循环向redis多次查询座位席别价格
         *
         * 因此这两个循环导致了与redis的交互次数增加，从而导致了性能问题，在v2版本中进行优化
         *
         * 然后将这个数量转换为整数，创建一个SeatClassDTO对象，存储座位类型、余票数量、价格等信息
         * 再用一个List集合seatClassList把这些SeatClassDTO对象包围起来
         * 最后再把这些座位类型对象集合seatClassList存到TicketListDTO中，即存到这个车次的车票集合实体！
         *
         * 到这基本上完成了闭环，通过层层查询，根据前端传过来的出发地，目的地，出发站点，目的站点等信息
         * 找到了这两个站点上所有的车次，然后又查询到了这些车次的所有座位类型的价格和余票数量
         * 最后把前端需要的信息封装到TicketPageQueryRespDTO对象中，返回给前端
         */
        // 如果座位查询结果列表为空，则从缓存数据中解析出车票列表DTO对象并赋值给座位查询结果列表
        seatResults = CollUtil.isEmpty(seatResults)
                ? regionTrainStationAllMap.values().stream()
                .map(each -> JSON.parseObject(each.toString(), TicketListDTO.class)).toList()
                : seatResults;
        // 根据出发时间对座位查询结果列表进行排序
        seatResults = seatResults.stream().sorted(new TimeStringComparator()).toList();
        // 遍历座位查询结果列表，核心问题就在这，这里每次循环都会发起一次redis查询请求，带来很大的网络开销
        for (TicketListDTO each : seatResults) {
            // 从缓存中安全获取列车站点价格信息，若缓存不存在则从数据库加载并缓存
            String trainStationPriceStr = distributedCache.safeGet(
                    String.format(TRAIN_STATION_PRICE, each.getTrainId(), each.getDeparture(), each.getArrival()),
                    String.class,
                    () -> {
                        LambdaQueryWrapper<TrainStationPriceDO> trainStationPriceQueryWrapper = Wrappers
                                .lambdaQuery(TrainStationPriceDO.class)
                                .eq(TrainStationPriceDO::getDeparture, each.getDeparture())
                                .eq(TrainStationPriceDO::getArrival, each.getArrival())
                                .eq(TrainStationPriceDO::getTrainId, each.getTrainId());
                        return JSON.toJSONString(trainStationPriceMapper.selectList(trainStationPriceQueryWrapper));
                    },
                    ADVANCE_TICKET_DAY,
                    TimeUnit.DAYS);
            // 将获取到的列车站点价格信息字符串解析为列车站点价格DO对象列表
            List<TrainStationPriceDO> trainStationPriceDOList = JSON.parseArray(trainStationPriceStr,
                    TrainStationPriceDO.class);
            // 创建一个用于存储座位类型信息的列表
            List<SeatClassDTO> seatClassList = new ArrayList<>();
            // 遍历列车站点价格DO对象列表
            trainStationPriceDOList.forEach(item -> {
                // 获取座位类型
                String seatType = String.valueOf(item.getSeatType());
                // 构建缓存键后缀，格式为：列车ID_出发站_到达站
                String keySuffix = StrUtil.join("_", each.getTrainId(), item.getDeparture(), item.getArrival());
                // 从缓存中获取该座位类型的余票数量
                Object quantityObj = stringRedisTemplate.opsForHash().get(TRAIN_STATION_REMAINING_TICKET + keySuffix,
                        seatType);
                // 将余票数量转换为整数，若缓存中不存在则从座位余量缓存加载器获取
                int quantity = Optional.ofNullable(quantityObj)
                        .map(Object::toString)
                        .map(Integer::parseInt)
                        .orElseGet(() -> {
                            Map<String, String> seatMarginMap = seatMarginCacheLoader.load(
                                    String.valueOf(each.getTrainId()), seatType, item.getDeparture(),
                                    item.getArrival());
                            return Optional.ofNullable(seatMarginMap.get(String.valueOf(item.getSeatType())))
                                    .map(Integer::parseInt).orElse(0);
                        });
                // 创建一个座位类型DTO对象，存储座位类型、余票数量、价格等信息
                seatClassList.add(new SeatClassDTO(item.getSeatType(), quantity,
                        new BigDecimal(item.getPrice()).divide(new BigDecimal("100"), 1, RoundingMode.HALF_UP), false));
            });
            // 将座位类型信息列表设置到车票列表DTO对象中
            each.setSeatClassList(seatClassList);
        }
        // 构建车票分页查询响应DTO对象，包含列车列表、出发站列表、到达站列表、列车品牌列表、座位类型列表等信息
        return TicketPageQueryRespDTO.builder()
                .trainList(seatResults)
                .departureStationList(buildDepartureStationList(seatResults))
                .arrivalStationList(buildArrivalStationList(seatResults))
                .trainBrandList(buildTrainBrandList(seatResults))
                .seatClassTypeList(buildSeatClassList(seatResults))
                .build();
    }

    // 实现车票分页查询功能的方法（版本2）
    /*
     * 该方法pageListTicketQueryV2实现了车票分页查询功能的优化版本。
     * 它首先通过责任链模式验证相关参数，然后从缓存中获取车站详细信息和列车信息
     * ，并对列车信息进行排序。接着，利用 Redis
     * 管道操作一次性获取列车站点价格和余票信息，根据列车类型处理并匹配相应的价格和余票数据，构建座位类型信息列表并设置到车票列表 DTO
     * 对象中。最后，构建并返回包含列车及相关信息的响应 DTO。此版本通过减少 Redis 交互次数，优化了性能，更适用于高并发场景。
     */
    @Override
    public TicketPageQueryRespDTO pageListTicketQueryV2(TicketPageQueryReqDTO requestParam) {
        // 使用责任链模式验证城市名称是否存在，若不存在则加载缓存，同时验证出发日期不能小于当前日期等
        ticketPageQueryAbstractChainContext.handler(TicketChainMarkEnum.TRAIN_QUERY_FILTER.name(), requestParam);
        // 从分布式缓存中获取StringRedisTemplate实例，用于操作Redis缓存
        StringRedisTemplate stringRedisTemplate = (StringRedisTemplate) distributedCache.getInstance();
        // 列车查询逻辑较为复杂，详细解析文章查看 https://nageoffer.com/12306/question
        // v2版本更符合企业级高并发真实场景解决方案，完美解决了v1版本性能深渊问题。通过Jmeter压测聚合报告得知，性能提升在300% - 500%+
        // 其实还能有v3版本，性能估计在原基础上还能进一步提升一倍。不过v3版本太过于复杂，不易读且不易扩展，就不写具体的代码了。面试中v2版本已经够和面试官吹的了
        // 从缓存中批量获取出发站和到达站的详细信息
        List<Object> stationDetails = stringRedisTemplate.opsForHash()
                .multiGet(REGION_TRAIN_STATION_MAPPING,
                        Lists.newArrayList(requestParam.getFromStation(), requestParam.getToStation()));
        // 构建地区与列车站点哈希键，格式为：REGION_TRAIN_STATION + 出发站地区名称 + 到达站地区名称
        String buildRegionTrainStationHashKey = String.format(REGION_TRAIN_STATION, stationDetails.get(0),
                stationDetails.get(1));
        // 获取该哈希键对应的所有缓存数据
        Map<Object, Object> regionTrainStationAllMap = stringRedisTemplate.opsForHash()
                .entries(buildRegionTrainStationHashKey);
        // 从缓存数据中解析出车票列表DTO对象，并根据出发时间排序
        List<TicketListDTO> seatResults = regionTrainStationAllMap.values().stream()
                .map(each -> JSON.parseObject(each.toString(), TicketListDTO.class))
                .sorted(new TimeStringComparator())
                .toList();

        /*
         * 这里构建缓存键列表集合，把所有要查询的列车站点座位价格缓存键都放到这个集合中
         * v1方法中是在String.format(TRAIN_STATION_PRICE, each.getTrainId(), each.getDeparture(), each.getArrival()),
         * 这里每次都构建架构缓存键，然后与redis交互查询
         * 而v2版本中，通过构建缓存键列表集合，一次性查询所有列车站点价格缓存值，减少了与redis的交互次数
         */
        // 构建列车站点价格缓存键列表
        List<String> trainStationPriceKeys = seatResults.stream()
                .map(each -> String.format(cacheRedisPrefix + TRAIN_STATION_PRICE, each.getTrainId(),
                        each.getDeparture(), each.getArrival()))
                .toList();
        // 通过管道操作一次性获取所有列车站点价格缓存值
        List<Object> trainStationPriceObjs = stringRedisTemplate
                .executePipelined((RedisCallback<String>) connection -> {
                    trainStationPriceKeys.forEach(each -> connection.stringCommands().get(each.getBytes()));
                    return null;
                });
        // 用于存储列车站点价格DO对象的列表
        List<TrainStationPriceDO> trainStationPriceDOList = new ArrayList<>();
        // 用于存储列车站点余票缓存键的列表
        List<String> trainStationRemainingKeyList = new ArrayList<>();

        /*
         * 这里遍历列车站点座位价格缓存值，解析并存储列车站点价格DO对象，同时构建余票缓存键列表
         * 余票缓存键的格式为：TRAIN_STATION_REMAINING_TICKET + 车次id + 出发站 + 到达站 + 座位类型
         *  //这里就是余票缓存键，格式和v1方法是一样的，座位类型不是缓存键的一部分
                //座位类型在字段名，即哈希数据结构中键值对的健，值是这个座位类型的余票
                * 然后在下面通过管道操作一次性从redis中查询出全部的余票缓存值
                *
                * TrainStationPriceDO对象中包含了车次id、出发站、到达站、座位类型、车票价格等信息
                * 这个由前面查到的
                *
                *
         */
        // 遍历列车站点价格缓存值，解析并存储列车站点价格DO对象，同时构建余票缓存键列表
        for (Object each : trainStationPriceObjs) {
            /*
             * 这里是解析上面通过通道查询出来的trainStationPriceObjs
             * 这里查询出来就是TrainStationPriceDO对象的列表
             * 包含了所有车次的所有座位类型的价格信息
             * 因此trainStationPriceDOList中就包含了所有车次的所有座位类型的价格信息
             * 然后就遍历每一个TrainStationPriceDO对象，构建余票缓存键
             * 这和v1中有些区别，v1中是单个车次处理，先找出这个车次中的所有座位类型价格
             * 然后把这些封装到一个list中，再遍历这个集合，找每一个座位类型的余票缓存值
             * 但是在v2中不是，v2中舍弃了两层for循环，在第一次查询座位类型价格时，
             * 通过管道操作一次查询了所有车次的所有座位类型的价格信息，因此一个集合就
             * 包含了所有车次的所有座位类型价格对象，即trainStationPriceDOList中
             * 是包含了所有车次的TrainStationPriceDO。
             * 那就只需要遍历这些对象，从中提取车次id，出发站点到达站点构造缓存键
             * 就能查出所有的座位类型余票缓存值！
             * 所以说还是要一行行分析代码！
             * 余票缓存值没有是从缓存数据加载中获得，SeatMarginCacheLoader
             * 在其内部会从数据库中查询存入缓存
             */
            List<TrainStationPriceDO> trainStationPriceList = JSON.parseArray(each.toString(),
                    TrainStationPriceDO.class);
            trainStationPriceDOList.addAll(trainStationPriceList);


            for (TrainStationPriceDO item : trainStationPriceList) {
                //这里就是余票缓存键，格式和v1方法是一样的，座位类型不是缓存键的一部分
                //座位类型在字段名，即哈希数据结构中键值对的健，值是这个座位类型的余票
                String trainStationRemainingKey = cacheRedisPrefix + TRAIN_STATION_REMAINING_TICKET
                        + StrUtil.join("_", item.getTrainId(), item.getDeparture(), item.getArrival());

                trainStationRemainingKeyList.add(trainStationRemainingKey);
            }
        }
        // 通过管道操作一次性获取所有列车站点余票缓存值
        List<Object> trainStationRemainingObjs = stringRedisTemplate
                .executePipelined((RedisCallback<String>) connection -> {
                    for (int i = 0; i < trainStationRemainingKeyList.size(); i++) {
                        connection.hashCommands().hGet(trainStationRemainingKeyList.get(i).getBytes(),
                                // 余票缓存值的字段名是座位类型，这里根据当前遍历的座位类型构建缓存键
                                trainStationPriceDOList.get(i).getSeatType().toString().getBytes());
                    }
                    return null;
                });

        // 遍历座位查询结果列表,为每个车次添加座位类型信息
        for (TicketListDTO each : seatResults) {
            // 根据列车类型获取对应的座位类型列表
            List<Integer> seatTypesByCode = VehicleTypeEnum.findSeatTypesByCode(each.getTrainType());
            // 从余票缓存值列表中截取当前列车对应的余票数据
            List<Object> remainingTicket = new ArrayList<>(
                    trainStationRemainingObjs.subList(0, seatTypesByCode.size()));
            // 从列车站点价格DO对象列表中截取当前列车对应的价格数据
            List<TrainStationPriceDO> trainStationPriceDOSub = new ArrayList<>(
                    trainStationPriceDOList.subList(0, seatTypesByCode.size()));
            // 清空已处理的余票缓存值和列车站点价格DO对象
            trainStationRemainingObjs.subList(0, seatTypesByCode.size()).clear();
            trainStationPriceDOList.subList(0, seatTypesByCode.size()).clear();
            // 用于存储座位类型信息的列表
            List<SeatClassDTO> seatClassList = new ArrayList<>();
            // 遍历当前列车的价格数据，构建座位类型DTO对象并添加到列表
            for (int i = 0; i < trainStationPriceDOSub.size(); i++) {
                TrainStationPriceDO trainStationPriceDO = trainStationPriceDOSub.get(i);
                SeatClassDTO seatClassDTO = SeatClassDTO.builder()
                        .type(trainStationPriceDO.getSeatType())
                        .quantity(Integer.parseInt(remainingTicket.get(i).toString()))
                        .price(new BigDecimal(trainStationPriceDO.getPrice()).divide(new BigDecimal("100"), 1,
                                RoundingMode.HALF_UP))
                        .candidate(false)
                        .build();
                seatClassList.add(seatClassDTO);
            }
            // 将座位类型信息列表设置到车票列表DTO对象中
            each.setSeatClassList(seatClassList);
        }
        // 构建车票分页查询响应DTO对象，包含列车列表、出发站列表、到达站列表、列车品牌列表、座位类型列表等信息
        return TicketPageQueryRespDTO.builder()
                .trainList(seatResults)
                .departureStationList(buildDepartureStationList(seatResults))
                .arrivalStationList(buildArrivalStationList(seatResults))
                .trainBrandList(buildTrainBrandList(seatResults))
                .seatClassTypeList(buildSeatClassList(seatResults))
                .build();
    }

    // 日志记录注解
    @ILog
    // 幂等性注解，设置唯一键前缀、键表达式、提示信息、场景和类型
    @Idempotent(uniqueKeyPrefix = "index12306 - ticket:lock_purchase - tickets:", key = "T(org.opengoofy.index12306.framework.starter.bases.ApplicationContextHolder).getBean('environment').getProperty('unique - name', '')"
            + "+'_'+"
            + "T(org.opengoofy.index12306.frameworks.starter.user.core.UserContext).getUsername()", message = "正在执行下单流程，请稍后...", scene = IdempotentSceneEnum.RESTAPI, type = IdempotentTypeEnum.SPEL)
    // 实现购票功能的方法（版本1）
    @Override
    public TicketPurchaseRespDTO purchaseTicketsV1(PurchaseTicketReqDTO requestParam) {
        // 使用责任链模式进行验证：1. 参数必填 2. 参数正确性 3. 余票是否充足 4. 乘客是否已买当前车次等
        purchaseTicketAbstractChainContext.handler(TicketChainMarkEnum.TRAIN_PURCHASE_TICKET_FILTER.name(),
                requestParam);
        // v1版本购票存在4个较为严重的问题，v2版本相比较v1版本更具有业务特点以及性能，整体提升较大
        // 写了详细的v2版本购票升级指南，详情查看：https://nageoffer.com/12306/question
        // 构建分布式锁的键，通过解析占位符生成
        String lockKey = environment
                .resolvePlaceholders(String.format(LOCK_PURCHASE_TICKETS, requestParam.getTrainId()));
        // 获取Redisson分布式锁
        RLock lock = redissonClient.getLock(lockKey);
        // 锁定分布式锁
        lock.lock();
        try {
            // 执行购票操作
            return ticketService.executePurchaseTickets(requestParam);
        } finally {
            // 释放分布式锁
            lock.unlock();
        }
    }

    // 定义一个本地锁的缓存，使用Caffeine构建，设置1天后过期
    private final Cache<String, ReentrantLock> localLockMap = Caffeine.newBuilder()
            .expireAfterWrite(1, TimeUnit.DAYS)
            .build();

    // 定义一个用于存储车票令牌刷新相关信息的缓存，使用Caffeine构建，设置10分钟后过期
    private final Cache<String, Object> tokenTicketsRefreshMap = Caffeine.newBuilder()
            .expireAfterWrite(10, TimeUnit.MINUTES)
            .build();

    // 日志记录注解
    @ILog
    // 幂等性注解，设置唯一键前缀、键表达式、提示信息、场景和类型
    @Idempotent(uniqueKeyPrefix = "index12306 - ticket:lock_purchase - tickets:", key = "T(org.opengoofy.index12306.framework.starter.bases.ApplicationContextHolder).getBean('environment').getProperty('unique - name', '')"
            + "+'_'+"
            + "T(org.opengoofy.index12306.frameworks.starter.user.core.UserContext).getUsername()", message = "正在执行下单流程，请稍后...", scene = IdempotentSceneEnum.RESTAPI, type = IdempotentTypeEnum.SPEL)
    // 实现购票功能的方法（版本2）
    @Override
    public TicketPurchaseRespDTO purchaseTicketsV2(PurchaseTicketReqDTO requestParam) {
        // 使用责任链模式进行验证：1. 参数必填 2. 参数正确性 3. 乘客是否已买当前车次等
        purchaseTicketAbstractChainContext.handler(TicketChainMarkEnum.TRAIN_PURCHASE_TICKET_FILTER.name(),
                requestParam);
        //这里就不用了，直接用lua脚本+余票缓存限流和避免超卖就行了，纯脱裤子放屁
        /*
         *核心就是用lua脚本+余票缓存限流和避免超卖然后用分布式锁和本地锁实现锁细粒度提升
         */
//        // 为什么需要令牌限流？余票缓存限流不可以么？详情查看：https://nageoffer.com/12306/question
//        // 从令牌桶中获取令牌
//        TokenResultDTO tokenResult = ticketAvailabilityTokenBucket.takeTokenFromBucket(requestParam);
//        // 如果获取的令牌为空
//        if (tokenResult.getTokenIsNull()) {
//            // 检查缓存中是否存在该列车ID对应的令牌刷新信息
//            Object ifPresentObj = tokenTicketsRefreshMap.getIfPresent(requestParam.getTrainId());
//            if (ifPresentObj == null) {
//                // 使用类锁进行同步
//                synchronized (TicketService.class) {
//                    // 再次检查缓存中是否存在该列车ID对应的令牌刷新信息
//                    if (tokenTicketsRefreshMap.getIfPresent(requestParam.getTrainId()) == null) {
//                        // 如果不存在，则创建一个新的对象并放入缓存
//                        ifPresentObj = new Object();
//                        tokenTicketsRefreshMap.put(requestParam.getTrainId(), ifPresentObj);
//                        // 执行令牌为空时的刷新令牌操作
//                        // 当令牌被取完时，可能需要一个机制来尝试重新加载或刷新令牌，以确保后续请求能够获取到足够的令牌
//                        tokenIsNullRefreshToken(requestParam, tokenResult);
//                    }
//                }
//            }
//            // 抛出服务异常，提示列车站点已无余票
//            throw new ServiceException("列车站点已无余票");
//        }
        // v1版本购票存在4个较为严重的问题，v2版本相比较v1版本更具有业务特点以及性能，整体提升较大
        // 写了详细的v2版本购票升级指南，详情查看：https://nageoffer.com/12306/question
        // 用于存储本地锁的列表
        List<ReentrantLock> localLockList = new ArrayList<>();
        // 用于存储分布式锁的列表
        List<RLock> distributedLockList = new ArrayList<>();
        // 根据座位类型对乘客信息进行分组
        Map<Integer, List<PurchaseTicketPassengerDetailDTO>> seatTypeMap = requestParam.getPassengers().stream()
                .collect(Collectors.groupingBy(PurchaseTicketPassengerDetailDTO::getSeatType));
        // 遍历每个座位类型及其对应的乘客信息列表
        seatTypeMap.forEach((searType, count) -> {
            // 构建锁的键，通过解析占位符生成，包含列车ID和座位类型
            String lockKey = environment
                    .resolvePlaceholders(String.format(LOCK_PURCHASE_TICKETS_V2, requestParam.getTrainId(), searType));
            // 从本地锁缓存中获取锁
            ReentrantLock localLock = localLockMap.getIfPresent(lockKey);
            if (localLock == null) {
                // 使用类锁进行同步
                synchronized (TicketService.class) {
                    // 再次检查本地锁缓存中是否存在该锁
                    if ((localLock = localLockMap.getIfPresent(lockKey)) == null) {
                        // 如果不存在，则创建一个新的可重入锁并放入缓存
                        localLock = new ReentrantLock(true);
                        localLockMap.put(lockKey, localLock);
                    }
                }
            }
            // 将本地锁添加到本地锁列表
            localLockList.add(localLock);
            // 获取Redisson公平分布式锁
            RLock distributedLock = redissonClient.getFairLock(lockKey);
            // 将分布式锁添加到分布式锁列表
            distributedLockList.add(distributedLock);
        });
        try {
            // 锁定所有本地锁
            localLockList.forEach(ReentrantLock::lock);
            // 锁定所有分布式锁
            distributedLockList.forEach(RLock::lock);
            // 执行购票操作
            return ticketService.executePurchaseTickets(requestParam);
        } finally {
            // 释放所有本地锁
            localLockList.forEach(localLock -> {
                try {
                    localLock.unlock();
                } catch (Throwable ignored) {
                }
            });
            // 释放所有分布式锁
            distributedLockList.forEach(distributedLock -> {
                try {
                    distributedLock.unlock();
                } catch (Throwable ignored) {
                }
            });
        }
    }

    /*
     * 此方法实现了完整的购票流程。首先从缓存获取列车信息，然后选择座位并保存车票信息到数据库。接着，构建车票订单相关请求 DTO
     * 并远程调用订单服务创建订单。如果订单创建成功，返回包含订单 ID 和订单详情的响应；若失败，则进行异常处理。整个过程通过事务管理，确保数据的一致性。
     *
     * 首先传入的参数是购票请求实体，包含车次id，乘车人，座位类型，出发到结束站
     * 然后获取车次id后，从缓存中找列车实体traindo，如果缓存中没有就从数据库中加载，设置一个过期时间
     * 一般是15天，因为可以提前15天买票。
     * 然后根据列车类型（动车或高铁）和购票请求参数，调用座位选择器获取购票结果TrainPurchaseTicketRespDTO
     * 再将购票结果转换为车票对象实体ticketdo，用于保存到数据库
     *
     * 然后遍历购票结果，创建车票订单创建远程请求dto，因为要远程调用订单服务创建订单
     * 包括TicketOrderItemCreateRemoteReqDTO和TicketOrderCreateRemoteReqDTO
     * 前者是一个订单中的若干个车票，后者代表一个订单，毕竟一个订单可以买多个车票，在分析订单服务时也是一样的
     * 然后调用远程订单服务 ticketOrderResult = ticketOrderRemoteService.createTicketOrder(orderCreateRemoteReqDTO);
     * 返回的就是订单号
     * 创建订单，这里是通过feign注解，通过http请求调用远程服务，不是rpc框架的远程服务。
     * 最后向上返回TicketPurchaseRespDTO购票响应实体，由订单号和乘车人订单详情组成
     * List<TicketOrderDetailRespDTO> ticketOrderDetailResults和订单号（由远程服务得到的）
     */
    @Override
    @Transactional(rollbackFor = Throwable.class)
    public TicketPurchaseRespDTO executePurchaseTickets(PurchaseTicketReqDTO requestParam) {
        // 用于存储车票订单详情响应DTO的列表
        List<TicketOrderDetailRespDTO> ticketOrderDetailResults = new ArrayList<>();
        // 获取购票请求中的列车ID
        String trainId = requestParam.getTrainId();
        // 节假日高并发购票Redis能扛得住么？详情查看：https://nageoffer.com/12306/question
        // 从分布式缓存中安全获取列车信息，如果缓存中不存在则从数据库加载并缓存，缓存有效期为ADVANCE_TICKET_DAY天
        TrainDO trainDO = distributedCache.safeGet(
                TRAIN_INFO + trainId,
                TrainDO.class,
                () -> trainMapper.selectById(trainId),
                ADVANCE_TICKET_DAY,
                TimeUnit.DAYS);
        // 根据列车类型和购票请求参数，选择座位并获取购票结果
        List<TrainPurchaseTicketRespDTO> trainPurchaseTicketResults = trainSeatTypeSelector
                .select(trainDO.getTrainType(), requestParam);
        // 将购票结果转换为TicketDO对象列表，用于保存到数据库
        List<TicketDO> ticketDOList = trainPurchaseTicketResults.stream()
                .map(each -> TicketDO.builder()
                        .username(UserContext.getUsername())
                        .trainId(Long.parseLong(requestParam.getTrainId()))
                        .carriageNumber(each.getCarriageNumber())
                        .seatNumber(each.getSeatNumber())
                        .passengerId(each.getPassengerId())
                        .ticketStatus(TicketStatusEnum.UNPAID.getCode())
                        .build())
                .toList();
        // 批量保存TicketDO对象到数据库
        /*
         * 这里只是把车票实体存到了数据库中，并没有更改数据库中余票数量
         */
        saveBatch(ticketDOList);
        // 定义用于存储车票订单创建结果的变量
        Result<String> ticketOrderResult;
        try {
            // 用于存储车票订单项目创建远程请求DTO的列表
            List<TicketOrderItemCreateRemoteReqDTO> orderItemCreateRemoteReqDTOList = new ArrayList<>();
            // 遍历购票结果，构建车票订单项目创建远程请求DTO和车票订单详情响应DTO，并添加到相应列表
            trainPurchaseTicketResults.forEach(each -> {
                TicketOrderItemCreateRemoteReqDTO orderItemCreateRemoteReqDTO = TicketOrderItemCreateRemoteReqDTO
                        .builder()
                        .amount(each.getAmount())
                        .carriageNumber(each.getCarriageNumber())
                        .seatNumber(each.getSeatNumber())
                        .idCard(each.getIdCard())
                        .idType(each.getIdType())
                        .phone(each.getPhone())
                        .seatType(each.getSeatType())
                        .ticketType(each.getUserType())
                        .realName(each.getRealName())
                        .build();
                TicketOrderDetailRespDTO ticketOrderDetailRespDTO = TicketOrderDetailRespDTO.builder()
                        .amount(each.getAmount())
                        .carriageNumber(each.getCarriageNumber())
                        .seatNumber(each.getSeatNumber())
                        .idCard(each.getIdCard())
                        .idType(each.getIdType())
                        .seatType(each.getSeatType())
                        .ticketType(each.getUserType())
                        .realName(each.getRealName())
                        .build();
                orderItemCreateRemoteReqDTOList.add(orderItemCreateRemoteReqDTO);
                ticketOrderDetailResults.add(ticketOrderDetailRespDTO);
            });
            // 构建查询条件，查询列车站点关系
            LambdaQueryWrapper<TrainStationRelationDO> queryWrapper = Wrappers.lambdaQuery(TrainStationRelationDO.class)
                    .eq(TrainStationRelationDO::getTrainId, trainId)
                    .eq(TrainStationRelationDO::getDeparture, requestParam.getDeparture())
                    .eq(TrainStationRelationDO::getArrival, requestParam.getArrival());
            // 从数据库中查询符合条件的列车站点关系
            TrainStationRelationDO trainStationRelationDO = trainStationRelationMapper.selectOne(queryWrapper);
            // 构建车票订单创建远程请求DTO
            TicketOrderCreateRemoteReqDTO orderCreateRemoteReqDTO = TicketOrderCreateRemoteReqDTO.builder()
                    .departure(requestParam.getDeparture())
                    .arrival(requestParam.getArrival())
                    .orderTime(new Date())
                    .source(SourceEnum.INTERNET.getCode())
                    .trainNumber(trainDO.getTrainNumber())
                    .departureTime(trainStationRelationDO.getDepartureTime())
                    .arrivalTime(trainStationRelationDO.getArrivalTime())
                    .ridingDate(trainStationRelationDO.getDepartureTime())
                    .userId(UserContext.getUserId())
                    .username(UserContext.getUsername())
                    .trainId(Long.parseLong(requestParam.getTrainId()))
                    .ticketOrderItems(orderItemCreateRemoteReqDTOList)
                    .build();
            // 远程调用车票订单服务创建订单
            ticketOrderResult = ticketOrderRemoteService.createTicketOrder(orderCreateRemoteReqDTO);
            // 如果订单创建失败或返回的数据为空，记录错误日志并抛出服务异常
            if (!ticketOrderResult.isSuccess() || StrUtil.isBlank(ticketOrderResult.getData())) {
                log.error("订单服务调用失败，返回结果：{}", ticketOrderResult.getMessage());
                throw new ServiceException("订单服务调用失败");
            }
        } catch (Throwable ex) {
            // 如果远程调用订单服务创建订单时出现异常，记录详细错误日志并抛出异常
            log.error("远程调用订单服务创建错误，请求参数：{}", JSON.toJSONString(requestParam), ex);
            throw ex;
        }
        // 返回包含订单ID和订单详情的购票响应DTO
        return new TicketPurchaseRespDTO(ticketOrderResult.getData(), ticketOrderDetailResults);
    }

    @Override
    public PayInfoRespDTO getPayInfo(String orderSn) {
        return payRemoteService.getPayInfo(orderSn).getData();
    }

    // 日志记录注解
    @ILog
    // 实现取消车票订单功能的方法
    /*
     * 该方法用于取消车票订单。首先远程调用订单服务取消订单，若取消成功且缓存更新类型非
     * “binlog”，则查询订单详情，解锁座位，将信息回滚到令牌桶，并更新相关站点和座位类型的余票缓存。任何步骤出错都会记录错误日志并抛出异常。
     */
    @Override
    public void cancelTicketOrder(CancelTicketOrderReqDTO requestParam) {
        // 远程调用车票订单服务取消订单
        Result<Void> cancelOrderResult = ticketOrderRemoteService.cancelTicketOrder(requestParam);
        // 如果订单取消成功，并且车票可用性缓存更新类型不是“binlog”
        if (cancelOrderResult.isSuccess() && !StrUtil.equals(ticketAvailabilityCacheUpdateType, "binlog")) {
            // 根据订单号查询车票订单详情
            Result<com.guoxu.ticketservice.remote.dto.TicketOrderDetailRespDTO > ticketOrderDetailResult = ticketOrderRemoteService
                    .queryTicketOrderByOrderSn(requestParam.getOrderSn());
            // 获取车票订单详情数据
            com.guoxu.ticketservice.remote.dto.TicketOrderDetailRespDTO  ticketOrderDetail = ticketOrderDetailResult
                    .getData();
            // 获取列车ID
            String trainId = String.valueOf(ticketOrderDetail.getTrainId());
            // 获取出发站
            String departure = ticketOrderDetail.getDeparture();
            // 获取到达站
            String arrival = ticketOrderDetail.getArrival();
            // 获取乘客详细信息列表
            List<TicketOrderPassengerDetailRespDTO> trainPurchaseTicketResults = ticketOrderDetail
                    .getPassengerDetails();
            try {
                // 调用座位服务解锁座位
                seatService.unlock(trainId, departure, arrival,
                        BeanUtil.convert(trainPurchaseTicketResults, TrainPurchaseTicketRespDTO.class));
            } catch (Throwable ex) {
                // 如果解锁座位失败，记录错误日志并抛出异常
                log.error("[取消订单] 订单号：{} 回滚列车DB座位状态失败", requestParam.getOrderSn(), ex);
                throw ex;
            }
            // 将取消订单的相关信息回滚到令牌桶中
            ticketAvailabilityTokenBucket.rollbackInBucket(ticketOrderDetail);
            try {
                // 从分布式缓存中获取StringRedisTemplate实例，用于操作Redis缓存
                StringRedisTemplate stringRedisTemplate = (StringRedisTemplate) distributedCache.getInstance();
                // 根据座位类型对乘客详细信息进行分组
                Map<Integer, List<TicketOrderPassengerDetailRespDTO>> seatTypeMap = trainPurchaseTicketResults.stream()
                        .collect(Collectors.groupingBy(TicketOrderPassengerDetailRespDTO::getSeatType));
                // 获取需要扣减余票的站点路线信息
                List<RouteDTO> routeDTOList = trainStationService.listTakeoutTrainStationRoute(trainId, departure,
                        arrival);
                // 遍历站点路线信息
                routeDTOList.forEach(each -> {
                    // 构建缓存键后缀
                    String keySuffix = StrUtil.join("_", trainId, each.getStartStation(), each.getEndStation());
                    // 遍历每个座位类型及其对应的乘客详细信息列表
                    seatTypeMap.forEach((seatType, ticketOrderPassengerDetailRespDTOList) -> {
                        // 增加对应站点和座位类型的余票数量
                        stringRedisTemplate.opsForHash()
                                .increment(TRAIN_STATION_REMAINING_TICKET + keySuffix, String.valueOf(seatType),
                                        ticketOrderPassengerDetailRespDTOList.size());
                    });
                });
            } catch (Throwable ex) {
                // 如果回滚列车缓存余票失败，记录错误日志并抛出异常
                log.error("[取消关闭订单] 订单号：{} 回滚列车Cache余票失败", requestParam.getOrderSn(), ex);
                throw ex;
            }
        }
    }

    /*
     * 该方法实现了普通车票退款的功能。首先通过责任链模式验证请求参数，然后查询车票订单详情，根据退款类型构建退款请求
     * DTO，并计算退款金额。接着远程调用支付服务进行退款操作，如果退款失败则抛出异常。目前方法返回空实体，可能后续会补充完整返回数据。
     */
    @Override
    public RefundTicketRespDTO commonTicketRefund(RefundTicketReqDTO requestParam) {
        // 使用责任链模式进行验证：1. 参数必填
        refundReqDTOAbstractChainContext.handler(TicketChainMarkEnum.TRAIN_REFUND_TICKET_FILTER.name(), requestParam);
        // 根据订单号查询车票订单详情，调用远程服务
        Result<com.guoxu.ticketservice.remote.dto.TicketOrderDetailRespDTO > orderDetailRespDTOResult = ticketOrderRemoteService
                .queryTicketOrderByOrderSn(requestParam.getOrderSn());
        // 如果查询失败或订单详情为空，抛出服务异常，提示车票订单不存在
        if (!orderDetailRespDTOResult.isSuccess() && Objects.isNull(orderDetailRespDTOResult.getData())) {
            throw new ServiceException("车票订单不存在");
        }
        // 获取车票订单详情数据
        com.guoxu.ticketservice.remote.dto.TicketOrderDetailRespDTO  ticketOrderDetailRespDTO = orderDetailRespDTOResult
                .getData();
        // 获取乘客详细信息列表
        List<TicketOrderPassengerDetailRespDTO> passengerDetails = ticketOrderDetailRespDTO.getPassengerDetails();
        // 如果乘客详细信息列表为空，抛出服务异常，提示车票子订单不存在
        if (CollectionUtil.isEmpty(passengerDetails)) {
            throw new ServiceException("车票子订单不存在");
        }
        // 创建退款请求DTO对象
        RefundReqDTO refundReqDTO = new RefundReqDTO();
        // 如果退款类型为部分退款
        if (RefundTypeEnum.PARTIAL_REFUND.getType().equals(requestParam.getType())) {
            // 创建车票订单项目查询请求DTO对象
            TicketOrderItemQueryReqDTO ticketOrderItemQueryReqDTO = new TicketOrderItemQueryReqDTO();
            ticketOrderItemQueryReqDTO.setOrderSn(requestParam.getOrderSn());
            ticketOrderItemQueryReqDTO.setOrderItemRecordIds(requestParam.getSubOrderRecordIdReqList());
            // 根据订单号和子订单记录ID列表查询车票订单项目
            Result<List<TicketOrderPassengerDetailRespDTO>> queryTicketItemOrderById = ticketOrderRemoteService
                    .queryTicketItemOrderById(ticketOrderItemQueryReqDTO);
            // 筛选出需要部分退款的乘客详细信息列表
            List<TicketOrderPassengerDetailRespDTO> partialRefundPassengerDetails = passengerDetails.stream()
                    .filter(item -> queryTicketItemOrderById.getData().contains(item))
                    .collect(Collectors.toList());
            // 设置退款请求DTO的退款类型为部分退款
            refundReqDTO.setRefundTypeEnum(RefundTypeEnum.PARTIAL_REFUND);
            // 设置退款请求DTO的退款详情请求DTO列表为部分退款的乘客详细信息列表
            refundReqDTO.setRefundDetailReqDTOList(partialRefundPassengerDetails);
        }
        // 如果退款类型为全额退款
        else if (RefundTypeEnum.FULL_REFUND.getType().equals(requestParam.getType())) {
            // 设置退款请求DTO的退款类型为全额退款
            refundReqDTO.setRefundTypeEnum(RefundTypeEnum.FULL_REFUND);
            // 设置退款请求DTO的退款详情请求DTO列表为所有乘客详细信息列表
            refundReqDTO.setRefundDetailReqDTOList(passengerDetails);
        }
        // 如果乘客详细信息列表不为空，计算部分退款金额并设置到退款请求DTO中
        if (CollectionUtil.isNotEmpty(passengerDetails)) {
            Integer partialRefundAmount = passengerDetails.stream()
                    .mapToInt(TicketOrderPassengerDetailRespDTO::getAmount)
                    .sum();
            refundReqDTO.setRefundAmount(partialRefundAmount);
        }
        // 设置退款请求DTO的订单号
        refundReqDTO.setOrderSn(requestParam.getOrderSn());
        // 远程调用支付服务进行普通退款
        Result<RefundRespDTO> refundRespDTOResult = payRemoteService.commonRefund(refundReqDTO);
        // 如果退款失败或退款响应数据为空，抛出服务异常，提示车票订单退款失败
        if (!refundRespDTOResult.isSuccess() && Objects.isNull(refundRespDTOResult.getData())) {
            throw new ServiceException("车票订单退款失败");
        }
        // 暂时返回空实体，可能后续会完善返回数据
        return null;
    }

    // 构建出发站列表的私有方法
    private List<String> buildDepartureStationList(List<TicketListDTO> seatResults) {
        // 从座位结果列表中提取出发站名称，去重后收集为列表返回
        return seatResults.stream().map(TicketListDTO::getDeparture).distinct().collect(Collectors.toList());
    }

    // 构建到达站列表的私有方法
    private List<String> buildArrivalStationList(List<TicketListDTO> seatResults) {
        // 从座位结果列表中提取到达站名称，去重后收集为列表返回
        return seatResults.stream().map(TicketListDTO::getArrival).distinct().collect(Collectors.toList());
    }

    // 构建座位类型列表的私有方法
    private List<Integer> buildSeatClassList(List<TicketListDTO> seatResults) {
        // 创建一个用于存储座位类型的集合
        Set<Integer> resultSeatClassList = new HashSet<>();
        // 遍历座位结果列表
        for (TicketListDTO each : seatResults) {
            // 遍历每个座位结果中的座位类型列表
            for (SeatClassDTO item : each.getSeatClassList()) {
                // 将座位类型添加到集合中
                resultSeatClassList.add(item.getType());
            }
        }
        // 将集合转换为列表并返回
        return resultSeatClassList.stream().toList();
    }

    // 构建列车品牌列表的私有方法
    private List<Integer> buildTrainBrandList(List<TicketListDTO> seatResults) {
        // 创建一个用于存储列车品牌的集合
        Set<Integer> trainBrandSet = new HashSet<>();
        // 遍历座位结果列表
        for (TicketListDTO each : seatResults) {
            // 如果列车品牌信息不为空
            if (StrUtil.isNotBlank(each.getTrainBrand())) {
                // 将列车品牌字符串按逗号拆分，转换为整数并添加到集合中
                trainBrandSet.addAll(StrUtil.split(each.getTrainBrand(), ",").stream().map(Integer::parseInt).toList());
            }
        }
        // 将集合转换为列表并返回
        return trainBrandSet.stream().toList();
    }

//    // 创建一个单线程的定时任务执行器，用于处理令牌为空时的刷新任务
//    private final ScheduledExecutorService tokenIsNullRefreshExecutor = Executors.newScheduledThreadPool(1);
//
//    // 当令牌为空时刷新令牌的私有方法
//    private void tokenIsNullRefreshToken(PurchaseTicketReqDTO requestParam, TokenResultDTO tokenResult) {
//        // 获取分布式锁，锁的键与列车ID相关
//        RLock lock = redissonClient.getLock(String.format(LOCK_TOKEN_BUCKET_ISNULL, requestParam.getTrainId()));
//        // 尝试获取锁，如果获取失败则直接返回
//        if (!lock.tryLock()) {
//            return;
//        }
//        // 使用定时任务执行器，延迟10秒执行刷新令牌的任务
//        tokenIsNullRefreshExecutor.schedule(() -> {
//            try {
//                // 创建一个用于存储座位类型的列表
//                List<Integer> seatTypes = new ArrayList<>();
//                // 创建一个用于存储座位类型与令牌数量映射的Map
//                Map<Integer, Integer> tokenCountMap = new HashMap<>();
//                // 遍历令牌结果中令牌为空的座位类型及数量字符串，拆分并填充到seatTypes和tokenCountMap中
//                tokenResult.getTokenIsNullSeatTypeCounts().stream()
//                        .map(each -> each.split("_"))
//                        .forEach(split -> {
//                            int seatType = Integer.parseInt(split[0]);
//                            seatTypes.add(seatType);
//                            tokenCountMap.put(seatType, Integer.parseInt(split[1]));
//                        });
//                // 获取指定列车、出发站、到达站和座位类型的座位数量列表
//                List<SeatTypeCountDTO> seatTypeCountDTOList = seatService.listSeatTypeCount(
//                        Long.parseLong(requestParam.getTrainId()), requestParam.getDeparture(),
//                        requestParam.getArrival(), seatTypes);
//                // 遍历座位类型及数量列表
//                for (SeatTypeCountDTO each : seatTypeCountDTOList) {
//                    // 获取该座位类型对应的令牌数量
//                    Integer tokenCount = tokenCountMap.get(each.getSeatType());
//                    // 如果令牌数量小于等于座位数量，则从令牌桶中删除该令牌相关信息
//                    if (tokenCount <= each.getSeatCount()) {
//                        ticketAvailabilityTokenBucket.delTokenInBucket(requestParam);
//                        break;
//                    }
//                }
//            } finally {
//                // 任务执行完毕，释放分布式锁
//                lock.unlock();
//            }
//        }, 10, TimeUnit.SECONDS);
//    }

    // 实现Runnable接口的run方法，用于初始化ticketService
    @Override
    public void run(String... args) throws Exception {
        // 从ApplicationContextHolder中获取TicketService实例并赋值给ticketService成员变量
        ticketService = ApplicationContextHolder.getBean(TicketService.class);
    }
}
