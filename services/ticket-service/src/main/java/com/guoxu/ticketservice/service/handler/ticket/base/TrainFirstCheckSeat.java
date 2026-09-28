package com.guoxu.ticketservice.service.handler.ticket.base;

import com.guoxu.DistributedCache;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * TrainFirstCheckSeat 一等座验座
 *
 * @author 执笔画棠
 * @date 2025/11/13 19:35
 **/
// TrainFirstCheckSeat类实现了TrainBitMapCheckSeat接口，
// 专门用于检查高铁一等座的座位情况，包括是否存在符合条件的座位以及选择的座位是否被占用
public class TrainFirstCheckSeat implements TrainBitMapCheckSeat {

    /**
     * 高铁一等座是否存在检查方法
     *
     * @param key              缓存Key，用于从分布式缓存获取座位相关数据
     * @param convert          座位统计Map，包含不同类型座位的数量信息
     * @param distributedCache 分布式缓存接口，用于操作缓存
     * @return 判断是否存在符合条件的座位，存在则返回true，否则返回false
     */
    @Override
    public boolean checkSeat(String key, HashMap<Integer, Integer> convert, DistributedCache distributedCache) {
        // 初始化标志位，用于表示是否存在符合条件的座位
        boolean flag = false;
        // 获取用于操作字符串类型缓存值的ValueOperations对象
        ValueOperations<String, String> opsForValue = ((StringRedisTemplate) distributedCache.getInstance())
                .opsForValue();
        // 使用AtomicInteger来统计匹配的座位类型数量
        AtomicInteger matchCount = new AtomicInteger(0);
        // 遍历一等座可能的座位类型（这里假设为4种类型）
        for (int i = 0; i < 4; i++) {
            // 初始化当前座位类型的匹配计数
            int cnt = 0;
            // 如果座位统计Map中包含当前座位类型
            if (convert.containsKey(i)) {
                // 遍历每一排座位（这里假设一等座每排有7个座位）
                for (int j = 0; j < 7; j++) {
                    // 获取缓存中对应座位的占用状态（通过位操作）
                    Boolean bit = opsForValue.getBit(key, i + j * 4);
                    // 如果座位被占用（bit为true），增加匹配计数
                    if (null != bit && bit) {
                        cnt = cnt + 1;
                    }
                    // 如果匹配计数达到该座位类型的统计数量，则增加匹配计数并跳出当前循环
                    if (cnt == convert.get(i)) {
                        matchCount.getAndIncrement();
                        break;
                    }
                }
                // 如果匹配计数不等于该座位类型的统计数量，则跳出外层循环
                if (cnt != convert.get(i)) {
                    break;
                }
            }
            // 如果所有座位类型都匹配，则设置标志位为true并跳出循环
            if (matchCount.get() == convert.size()) {
                flag = true;
                break;
            }
        }
        // 返回是否存在符合条件的座位
        return flag;
    }

    /**
     * 高铁一等座选择座位是否被占用的检查方法
     *
     * @param chooseSeatList 选择座位列表，包含用户选择的座位信息
     * @param actualSeats    座位状态数组，用于表示车厢内座位的占用情况
     * @param SEAT_Y_INT     坐标转换Map，用于将座位字母标识转换为整数索引
     * @return 判断用户选择的座位是否被占用，这里简单返回false，实际可能需要进一步实现具体逻辑
     */
    @Override
    public boolean checkChooseSeat(List<String> chooseSeatList, int[][] actualSeats,
                                   Map<Character, Integer> SEAT_Y_INT) {
        return false;
    }
}