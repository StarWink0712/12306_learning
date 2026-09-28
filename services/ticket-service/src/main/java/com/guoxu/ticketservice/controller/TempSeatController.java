package com.guoxu.ticketservice.controller;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.guoxu.DistributedCache;
import com.guoxu.Results;
import com.guoxu.result.Result;
import com.guoxu.ticketservice.common.enums.SeatStatusEnum;
import com.guoxu.ticketservice.dao.entity.SeatDO;
import com.guoxu.ticketservice.dao.entity.TrainStationRelationDO;
import com.guoxu.ticketservice.dao.mapper.SeatMapper;
import com.guoxu.ticketservice.dao.mapper.TrainStationRelationMapper;
import com.guoxu.toolkit.ThreadUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static com.guoxu.ticketservice.common.constant.RedisKeyConstant.TICKET_AVAILABILITY_TOKEN_BUCKET;
import static com.guoxu.ticketservice.common.constant.RedisKeyConstant.TRAIN_STATION_REMAINING_TICKET;

/**
 * TempSeatController 联调临时解决方案
 *
 * @author 执笔画棠
 * @date 2025/11/12 20:23
 **/
//deprecated注解是为了提醒开发者，该类是临时解决方案，不应该在生产环境中使用
@Deprecated
@RequiredArgsConstructor
@RestController
// TempSeatController类用于处理与临时座位相关的控制逻辑
// 该控制器类提供了一个用于座位重置的接口。
// 首先，它将指定列车 ID 的座位状态更新为可用。
// 然后，线程休眠 5 秒，接着删除与该列车相关的火车站点剩余车票缓存以及车票可用性令牌桶缓存。
public class TempSeatController {

    // 注入SeatMapper，用于对座位数据进行数据库操作
    private final SeatMapper seatMapper;
    // 注入TrainStationRelationMapper，用于对火车站点关系数据进行数据库操作
    private final TrainStationRelationMapper trainStationRelationMapper;
    // 注入DistributedCache，用于操作分布式缓存
    private final DistributedCache distributedCache;

    /**
     * 处理座位重置的接口方法
     *
     * @param trainId 列车ID，通过请求参数获取
     * @return 返回一个Result<Void>类型的结果，表示操作是否成功
     */
    @PostMapping("/api/ticket - service/temp/seat/reset")
    public Result<Void> purchaseTickets(@RequestParam String trainId) {
        // 创建一个SeatDO对象，用于设置座位状态
        SeatDO seatDO = new SeatDO();
        // 设置座位状态为可用状态（通过SeatStatusEnum.AVAILABLE获取对应的状态码）
        seatDO.setSeatStatus(SeatStatusEnum.AVAILABLE.getCode());
        // 使用seatMapper更新数据库中指定列车ID的座位状态为可用
        seatMapper.update(seatDO, Wrappers.lambdaUpdate(SeatDO.class).eq(SeatDO::getTrainId, trainId));
        // 线程休眠5000毫秒，即5秒
        ThreadUtil.sleep(5000);
        // 从DistributedCache获取StringRedisTemplate实例，用于操作Redis缓存
        StringRedisTemplate stringRedisTemplate = (StringRedisTemplate) distributedCache.getInstance();
        // 使用trainStationRelationMapper从数据库中查询指定列车ID的火车站点关系数据列表
        List<TrainStationRelationDO> trainStationRelationDOList = trainStationRelationMapper
                .selectList(Wrappers.lambdaQuery(TrainStationRelationDO.class)
                        .eq(TrainStationRelationDO::getTrainId, trainId));
        // 遍历火车站点关系数据列表
        for (TrainStationRelationDO each : trainStationRelationDOList) {
            // 构建缓存键的后缀，由列车ID、出发站和到达站拼接而成
            String keySuffix = StrUtil.join("_", each.getTrainId(), each.getDeparture(), each.getArrival());
            // 删除与该火车站点关系对应的缓存键（TRAIN_STATION_REMAINING_TICKET + keySuffix）
            stringRedisTemplate.delete(TRAIN_STATION_REMAINING_TICKET + keySuffix);
        }
        // 删除与列车ID对应的车票可用性令牌桶缓存键（TICKET_AVAILABILITY_TOKEN_BUCKET + trainId）
        stringRedisTemplate.delete(TICKET_AVAILABILITY_TOKEN_BUCKET + trainId);
        // 返回操作成功的结果
        return Results.success();
    }
}
