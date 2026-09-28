package com.guoxu.ticketservice.service.handler.ticket.base;

import com.guoxu.DistributedCache;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * TrainSecondCheckSeat 二等座验座
 *
 * @author 执笔画棠
 * @date 2025/11/13 19:36
 **/
// TrainSecondCheckSeat类实现了TrainBitMapCheckSeat接口，
// 主要用于检查高铁二等座的座位情况，包括是否存在符合条件的座位以及选择的座位是否被占用
public class TrainSecondCheckSeat implements TrainBitMapCheckSeat {

    /**
     * 高铁二等座是否存在检查方法
     *
     * @param key              缓存Key，通过该Key从分布式缓存获取座位相关信息
     * @param convert          座位统计Map，存储了不同类型座位的数量需求
     * @param distributedCache 分布式缓存接口，用于对缓存进行操作
     * @return 判断是否存在满足条件的座位，存在返回true，否则返回false
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
        // 遍历二等座可能的座位类型（这里假设为5种类型）
        for (int i = 0; i < 5; i++) {
            // 初始化当前座位类型的匹配计数
            int cnt = 0;
            // 如果座位统计Map中包含当前座位类型
            if (convert.containsKey(i)) {
                // 遍历每一排座位（这里假设二等座每排有18个座位）
                for (int j = 0; j < 18; j++) {
                    // 通过位操作从缓存中获取对应座位的占用状态
                    Boolean bit = opsForValue.getBit(key, i + j * 5);
                    // 如果座位被占用（bit为true），则增加匹配计数
                    if (null != bit && bit) {
                        cnt = cnt + 1;
                    }
                    // 当匹配计数达到该座位类型在统计Map中的数量时，增加匹配计数并跳出当前循环
                    if (cnt == convert.get(i)) {
                        matchCount.getAndIncrement();
                        break;
                    }
                }
                // 如果匹配计数不等于该座位类型在统计Map中的数量，则跳出外层循环
                if (cnt != convert.get(i)) {
                    break;
                }
            }
            // 当所有座位类型都匹配时，设置标志位为true并跳出循环
            if (matchCount.get() == convert.size()) {
                flag = true;
                break;
            }
        }
        // 返回是否存在符合条件的座位
        return flag;
    }

    /**
     * 高铁二等座选择座位是否被占用的检查方法
     *
     * @param chooseSeatList 选择座位列表，包含用户选择的座位信息
     * @param actualSeats    座位状态数组，用于表示车厢内座位的占用情况
     * @param SEAT_Y_INT     坐标转换Map，用于将座位字母标识转换为整数索引
     * @return 判断用户选择的座位是否被占用，当前简单返回false，实际逻辑可能需进一步完善
     */
    @Override
    public boolean checkChooseSeat(List<String> chooseSeatList, int[][] actualSeats,
                                   Map<Character, Integer> SEAT_Y_INT) {
        return false;
    }
}