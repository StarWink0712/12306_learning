package com.guoxu.ticketservice.job;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.guoxu.DistributedCache;
import com.guoxu.ticketservice.dao.entity.TrainDO;
import com.guoxu.ticketservice.dao.entity.TrainStationRelationDO;
import com.guoxu.ticketservice.dao.mapper.TrainStationRelationMapper;
import com.guoxu.ticketservice.job.base.AbstractTrainStationJobHandlerTemplate;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static cn.hutool.core.date.DatePattern.NORM_DATETIME_MINUTE_FORMAT;
import static com.guoxu.ticketservice.common.constant.Index12306Constant.ADVANCE_TICKET_DAY;
import static com.guoxu.ticketservice.common.constant.RedisKeyConstant.TRAIN_STATION_DETAIL;

/**
 * TrainStationDetailJobHandler
 *
 * @author 执笔画棠
 * @date 2025/11/12 21:47
 **/
@Deprecated
@RestController
@RequiredArgsConstructor
// TrainStationDetailJobHandler类继承自AbstractTrainStationJobHandlerTemplate，用于处理火车站详细信息的定时任务
public class TrainStationDetailJobHandler extends AbstractTrainStationJobHandlerTemplate {

    // 注入TrainStationRelationMapper，用于操作火车站关系相关的数据访问
    private final TrainStationRelationMapper trainStationRelationMapper;
    // 注入DistributedCache，用于操作分布式缓存
    private final DistributedCache distributedCache;

    // 使用XxlJob注解标记该方法为XXL - Job任务，任务名称为"trainStationDetailJobHandler"
    // 同时使用GetMapping注解定义一个HTTP GET请求的接口路径
    @XxlJob(value = "trainStationDetailJobHandler")
    @GetMapping("/api/ticket - service/train - station - detail/job/cache - init/execute")
    // 重写execute方法，调用父类的execute方法，触发定时任务执行流程
    @Override
    public void execute() {
        super.execute();
    }

    // 实现父类的抽象方法actualExecute，处理具体的定时任务逻辑
    @Override
    protected void actualExecute(List<TrainDO> trainDOPageRecords) {
        // 遍历列车信息分页记录列表
        for (TrainDO each : trainDOPageRecords) {
            // 构建查询条件，查询与当前列车ID匹配的火车站关系记录
            LambdaQueryWrapper<TrainStationRelationDO> relationQueryWrapper = Wrappers
                    .lambdaQuery(TrainStationRelationDO.class)
                    .eq(TrainStationRelationDO::getTrainId, each.getId());
            // 根据查询条件从数据库中获取火车站关系记录列表
            List<TrainStationRelationDO> trainStationRelationDOList = trainStationRelationMapper
                    .selectList(relationQueryWrapper);
            // 如果查询结果为空，则返回，不进行后续处理
            if (CollUtil.isEmpty(trainStationRelationDOList)) {
                return;
            }
            // 遍历火车站关系记录列表
            for (TrainStationRelationDO item : trainStationRelationDOList) {
                // 构建要缓存的实际哈希值Map，包含列车车次、出发标志、到达标志、出发时间、到达时间、售票时间和列车标签等信息
                Map<String, String> actualCacheHashValue = MapUtil.builder("trainNumber", each.getTrainNumber())
                        .put("departureFlag", BooleanUtil.toStringTrueFalse(item.getDepartureFlag()))
                        .put("arrivalFlag", BooleanUtil.toStringTrueFalse(item.getArrivalFlag()))
                        .put("departureTime", DateUtil.format(item.getDepartureTime(), "HH:mm"))
                        .put("arrivalTime", DateUtil.format(item.getArrivalTime(), "HH:mm"))
                        .put("saleTime", DateUtil.format(each.getSaleTime(), NORM_DATETIME_MINUTE_FORMAT))
                        .put("trainTag", each.getTrainTag().toString())
                        .build();
                // 从DistributedCache获取StringRedisTemplate实例，用于操作Redis缓存
                StringRedisTemplate stringRedisTemplate = (StringRedisTemplate) distributedCache.getInstance();
                // 构建缓存键，由火车站详细信息缓存前缀、列车ID、出发站和到达站拼接而成
                String buildCacheKey = TRAIN_STATION_DETAIL
                        + StrUtil.join("_", each.getId(), item.getDeparture(), item.getArrival());
                // 将构建好的哈希值Map存入Redis的哈希表中
                stringRedisTemplate.opsForHash().putAll(buildCacheKey, actualCacheHashValue);
                // 设置缓存键的过期时间为提前售票天数（ADVANCE_TICKET_DAY），单位为天
                stringRedisTemplate.expire(buildCacheKey, ADVANCE_TICKET_DAY, TimeUnit.DAYS);
            }
        }
    }
}
