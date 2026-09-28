package com.guoxu.ticketservice.job;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.guoxu.DistributedCache;
import com.guoxu.ticketservice.dao.entity.RegionDO;
import com.guoxu.ticketservice.dao.entity.TrainStationRelationDO;
import com.guoxu.ticketservice.dao.mapper.RegionMapper;
import com.guoxu.ticketservice.dao.mapper.TrainStationRelationMapper;
import com.guoxu.toolkit.EnvironmentUtil;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.IJobHandler;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.guoxu.ticketservice.common.constant.Index12306Constant.ADVANCE_TICKET_DAY;
import static com.guoxu.ticketservice.common.constant.RedisKeyConstant.REGION_TRAIN_STATION;

/**
 * RegionTrainStationJobHandler
 * 地区站点查询定时任务
 * @author 执笔画棠
 * @date 2025/11/12 19:27
 **/
// RegionTrainStationJobHandler类实现了IJobHandler接口，用于处理与地区火车站相关的定时任务
    @RequiredArgsConstructor
    @Deprecated
    @RestController
public class RegionTrainStationJobHandler extends IJobHandler {

    // 注入RegionMapper，用于操作地区相关的数据访问
    private final RegionMapper regionMapper;
    // 注入TrainStationRelationMapper，用于操作火车站关系相关的数据访问
    private final TrainStationRelationMapper trainStationRelationMapper;
    // 注入DistributedCache，用于操作分布式缓存
    private final DistributedCache distributedCache;

    // 使用XxlJob注解标记该方法为XXL - Job任务，任务名称为"regionTrainStationJobHandler"
    // 同时使用GetMapping注解定义一个HTTP GET请求的接口路径
    @XxlJob(value = "regionTrainStationJobHandler")
    @GetMapping("/api/ticket - service/region - train - station/job/cache - init/execute")
    // 重写IJobHandler接口的execute方法，定义定时任务的执行逻辑
    @Override
    public void execute() {
        // 从数据库中查询所有地区名称，并转换为列表
        List<String> regionList = regionMapper.selectList(Wrappers.emptyWrapper())
                .stream()
                .map(RegionDO::getName)
                .collect(Collectors.toList());
        // 获取定时任务的请求参数
        String requestParam = getJobRequestParam();
        // 如果请求参数不为空，则使用该参数作为日期字符串；否则使用明天的日期字符串
        var dateTime = StrUtil.isNotBlank(requestParam)? requestParam : DateUtil.tomorrow().toDateStr();
        // 嵌套循环遍历地区列表，生成所有可能的地区对（出发地和目的地）
        for (int i = 0; i < regionList.size(); i++) {
            for (int j = 0; j < regionList.size(); j++) {
                // 跳过出发地和目的地相同的情况
                if (i != j) {
                    // 获取当前出发地区名称
                    String startRegion = regionList.get(i);
                    // 获取当前目的地区名称
                    String endRegion = regionList.get(j);
                    // 构建查询条件，查询出发地和目的地匹配的火车站关系记录
                    LambdaQueryWrapper<TrainStationRelationDO> relationQueryWrapper = Wrappers
                            .lambdaQuery(TrainStationRelationDO.class)
                            .eq(TrainStationRelationDO::getStartRegion, startRegion)
                            .eq(TrainStationRelationDO::getEndRegion, endRegion);
                    // 根据查询条件从数据库中获取火车站关系记录列表
                    List<TrainStationRelationDO> trainStationRelationDOList = trainStationRelationMapper
                            .selectList(relationQueryWrapper);
                    // 如果查询结果为空，则跳过本次循环，继续下一对地区的处理
                    if (CollUtil.isEmpty(trainStationRelationDOList)) {
                        continue;
                    }
                    // 创建一个Set集合，用于存储ZSet的元组数据
                    Set<ZSetOperations.TypedTuple<String>> tuples = new HashSet<>();
                    // 遍历火车站关系记录列表，构建ZSet元组数据
                    for (TrainStationRelationDO item : trainStationRelationDOList) {
                        // 构建ZSet的键，由列车ID、出发站和到达站拼接而成
                        String zSetKey = StrUtil.join("_", item.getTrainId(), item.getDeparture(), item.getArrival());
                        // 创建ZSet元组，包含键和对应的排序分数（出发时间的时间戳）
                        ZSetOperations.TypedTuple<String> tuple = ZSetOperations.TypedTuple.of(zSetKey,
                                Double.valueOf(item.getDepartureTime().getTime()));
                        // 将元组添加到Set集合中
                        tuples.add(tuple);
                    }
                    // 从DistributedCache获取StringRedisTemplate实例，用于操作Redis缓存
                    StringRedisTemplate stringRedisTemplate = (StringRedisTemplate) distributedCache.getInstance();
                    // 构建缓存键，由地区火车站缓存前缀、出发地、目的地和日期拼接而成
                    String buildCacheKey = REGION_TRAIN_STATION + StrUtil.join("_", startRegion, endRegion, dateTime);
                    // 将ZSet元组数据添加到Redis的ZSet中
                    stringRedisTemplate.opsForZSet().add(buildCacheKey, tuples);
                    // 设置缓存键的过期时间为提前售票天数（ADVANCE_TICKET_DAY），单位为天
                    stringRedisTemplate.expire(buildCacheKey, ADVANCE_TICKET_DAY, TimeUnit.DAYS);
                }
            }
        }
    }

    // 获取定时任务请求参数的私有方法
    private String getJobRequestParam() {
        // 如果是开发环境
        return EnvironmentUtil.isDevEnvironment()
                // 从请求头中获取名为"requestParam"的参数
                ? ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest()
                .getHeader("requestParam")
                // 否则，从XXL - Job框架中获取任务参数
                : XxlJobHelper.getJobParam();
    }
}
