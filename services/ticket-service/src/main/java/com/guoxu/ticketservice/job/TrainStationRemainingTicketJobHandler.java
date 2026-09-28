package com.guoxu.ticketservice.job;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.guoxu.DistributedCache;
import com.guoxu.ticketservice.dao.entity.TrainDO;
import com.guoxu.ticketservice.dao.entity.TrainStationRelationDO;
import com.guoxu.ticketservice.dao.mapper.TrainMapper;
import com.guoxu.ticketservice.dao.mapper.TrainStationRelationMapper;
import com.guoxu.ticketservice.job.base.AbstractTrainStationJobHandlerTemplate;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.guoxu.ticketservice.common.constant.Index12306Constant.ADVANCE_TICKET_DAY;
import static com.guoxu.ticketservice.common.constant.RedisKeyConstant.TRAIN_STATION_REMAINING_TICKET;

/**
 * TrainStationRemainingTicketJobHandler
 *
 * @author 执笔画棠
 * @date 2025/11/12 21:49
 **/
@Deprecated
@RestController
@RequiredArgsConstructor
// TrainStationRemainingTicketJobHandler类继承自AbstractTrainStationJobHandlerTemplate，
// 用于处理火车站剩余车票缓存初始化的定时任务
public class TrainStationRemainingTicketJobHandler extends AbstractTrainStationJobHandlerTemplate {

    // 注入TrainStationRelationMapper，用于查询火车站关系数据
    private final TrainStationRelationMapper trainStationRelationMapper;
    // 注入DistributedCache，用于操作分布式缓存
    private final DistributedCache distributedCache;
    // 注入TrainMapper，用于查询列车数据
    private final TrainMapper trainMapper;

    /**
     * 为了方便大家使用项目启动时初始化缓存
     * 注意：生产环境不会这么操作，因为生产环境一般采用滚动发布，如果直接赋值可能会出现问题
     */
    // 使用XxlJob注解标记该方法为XXL - Job任务，任务名称为"trainStationRemainingTicketJobHandler"
    // 同时使用GetMapping注解定义一个HTTP GET请求的接口路径
    @XxlJob(value = "trainStationRemainingTicketJobHandler")
    @GetMapping("/api/ticket - service/train - station - remaining - ticket/job/cache - init/execute")
    // 重写execute方法，调用父类的execute方法，启动定时任务执行流程
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
                // 获取当前火车站关系记录对应的列车ID
                Long trainId = item.getTrainId();
                // 使用TrainMapper根据列车ID查询列车信息
                TrainDO trainDO = trainMapper.selectById(trainId);
                // 创建一个HashMap用于存储剩余车票信息
                Map<String, String> trainStationRemainingTicket = new HashMap<>();
                // 根据列车类型设置不同的剩余车票数量
                switch (trainDO.getTrainType()) {
                    case 0 -> {
                        // 列车类型为0时，设置不同座位类型的剩余车票数量
                        trainStationRemainingTicket.put("0", "10");
                        trainStationRemainingTicket.put("1", "140");
                        trainStationRemainingTicket.put("2", "810");
                    }
                    case 1 -> {
                        // 列车类型为1时，设置不同座位类型的剩余车票数量
                        trainStationRemainingTicket.put("3", "96");
                        trainStationRemainingTicket.put("4", "192");
                        trainStationRemainingTicket.put("5", "216");
                        trainStationRemainingTicket.put("13", "216");
                    }
                    case 2 -> {
                        // 列车类型为2时，设置不同座位类型的剩余车票数量
                        trainStationRemainingTicket.put("6", "96");
                        trainStationRemainingTicket.put("7", "192");
                        trainStationRemainingTicket.put("8", "216");
                        trainStationRemainingTicket.put("13", "216");
                    }
                }
                // 从DistributedCache获取StringRedisTemplate实例，用于操作Redis缓存
                StringRedisTemplate stringRedisTemplate = (StringRedisTemplate) distributedCache.getInstance();
                // 构建缓存键，由火车站剩余车票缓存前缀、列车ID、出发站和到达站拼接而成
                String buildCacheKey = TRAIN_STATION_REMAINING_TICKET
                        + StrUtil.join("_", each.getId(), item.getDeparture(), item.getArrival());
                // 将剩余车票信息存入Redis的哈希表中
                stringRedisTemplate.opsForHash().putAll(buildCacheKey, trainStationRemainingTicket);
                // 设置缓存键的过期时间为提前售票天数（ADVANCE_TICKET_DAY），单位为天
                stringRedisTemplate.expire(buildCacheKey, ADVANCE_TICKET_DAY, TimeUnit.DAYS);
            }
        }
    }
}
