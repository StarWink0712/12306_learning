package com.guoxu.ticketservice.canal;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.guoxu.DistributedCache;
import com.guoxu.strategy.AbstractExecuteStrategy;
import com.guoxu.ticketservice.common.enums.CanalExecuteStrategyMarkEnum;
import com.guoxu.ticketservice.common.enums.SeatStatusEnum;
import com.guoxu.ticketservice.mq.event.CanalBinlogEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.*;

import static com.guoxu.ticketservice.common.constant.RedisKeyConstant.TRAIN_STATION_REMAINING_TICKET;

/**
 * TicketAvailabilityCacheUpdateHandler
 * 列车余票缓存更新组件
 * @author 执笔画棠
 * @date 2025/11/13 22:27
 **/
@Component
@RequiredArgsConstructor
public class TicketAvailabilityCacheUpdateHandler implements AbstractExecuteStrategy<CanalBinlogEvent, Void> {
    // 分布式缓存实例，用于操作Redis等缓存系统
    private final DistributedCache distributedCache;

    // 核心执行方法，处理Canal binlog事件
    @Override
    public void execute(CanalBinlogEvent message) {
        // 创建消息数据列表，用于存储变更后的数据
        List<Map<String, Object>> messageDataList = new ArrayList<>();
        // 创建实际旧数据列表，用于存储变更前的有效数据
        List<Map<String, Object>> actualOldDataList = new ArrayList<>();

        // 遍历binlog事件中的所有旧数据记录
        for (int i = 0; i < message.getOld().size(); i++) {
            // 获取当前索引位置的旧数据映射
            Map<String, Object> oldDataMap = message.getOld().get(i);
            // 检查座位状态字段是否存在且不为空
            if (oldDataMap.get("seat_status") != null && StrUtil.isNotBlank(oldDataMap.get("seat_status").toString())) {
                // 获取对应索引位置的新数据映射
                Map<String, Object> currentDataMap = message.getData().get(i);
                // 检查新数据的座位状态是否为可用(0)或锁定(1)状态
                if (StrUtil.equalsAny(currentDataMap.get("seat_status").toString(),
                        String.valueOf(SeatStatusEnum.AVAILABLE.getCode()),
                        String.valueOf(SeatStatusEnum.LOCKED.getCode()))) {
                    // 将符合条件的旧数据添加到实际旧数据列表
                    actualOldDataList.add(oldDataMap);
                    // 将对应的新数据添加到消息数据列表
                    messageDataList.add(currentDataMap);
                }
            }
        }

        // 如果筛选后的数据列表为空，则直接返回，不进行后续处理
        if (CollUtil.isEmpty(messageDataList) || CollUtil.isEmpty(actualOldDataList)) {
            return;
        }

        // 创建缓存变更键映射，用于记录每个缓存键对应的座位类型数量变化
        Map<String, Map<Integer, Integer>> cacheChangeKeyMap = new HashMap<>();

        // 遍历筛选后的消息数据列表
        for (int i = 0; i < messageDataList.size(); i++) {
            // 获取当前索引的新数据记录
            Map<String, Object> each = messageDataList.get(i);
            // 获取对应的实际旧数据记录
            Map<String, Object> actualOldData = actualOldDataList.get(i);
            // 获取旧数据的座位状态
            String seatStatus = actualOldData.get("seat_status").toString();
            // 计算增量：如果旧状态是可用(0)，则减1；否则加1
            int increment = Objects.equals(seatStatus, "0") ? -1 : 1;
            // 获取列车ID
            String trainId = each.get("train_id").toString();
            // 构建缓存哈希键：组合列车ID、起始站和终点站
            String hashCacheKey = TRAIN_STATION_REMAINING_TICKET + trainId + "_" + each.get("start_station") + "_"
                    + each.get("end_station");
            // 从缓存变更映射中获取该缓存键对应的座位类型映射
            Map<Integer, Integer> seatTypeMap = cacheChangeKeyMap.get(hashCacheKey);
            // 如果座位类型映射为空，则创建新的映射
            if (CollUtil.isEmpty(seatTypeMap)) {
                seatTypeMap = new HashMap<>();
            }
            // 获取座位类型并转换为整数
            Integer seatType = Integer.parseInt(each.get("seat_type").toString());
            // 获取该座位类型当前的数量变化值
            Integer num = seatTypeMap.get(seatType);
            // 更新座位类型映射：如果当前值为空则设置为增量，否则累加增量
            seatTypeMap.put(seatType, num == null ? increment : num + increment);
            // 将更新后的座位类型映射放回缓存变更映射
            cacheChangeKeyMap.put(hashCacheKey, seatTypeMap);
        }

        // 获取分布式缓存实例，并转换为StringRedisTemplate类型
        StringRedisTemplate instance = (StringRedisTemplate) distributedCache.getInstance();
        // 遍历缓存变更映射，逐个更新Redis中的哈希值
        cacheChangeKeyMap.forEach((cacheKey, cacheVal) -> cacheVal
                // 对每个座位类型，使用increment方法原子性地更新哈希字段的值
                .forEach((seatType, num) -> instance.opsForHash().increment(cacheKey, String.valueOf(seatType), num)));
    }

    // 返回该处理器的标识标记，用于路由到对应的表处理逻辑
    @Override
    public String mark() {
        return CanalExecuteStrategyMarkEnum.T_SEAT.getActualTable();
    }
}
