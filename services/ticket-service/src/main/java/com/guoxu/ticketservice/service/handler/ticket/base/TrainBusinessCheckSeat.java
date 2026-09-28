package com.guoxu.ticketservice.service.handler.ticket.base;

import com.guoxu.DistributedCache;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * TrainBusinessCheckSeat 商务座验证座位
 *
 * @author 执笔画棠
 * @date 2025/11/13 19:34
 **/
// TrainBusinessCheckSeat类实现了TrainBitMapCheckSeat接口，
// 用于检查高铁商务座的座位情况，包括是否存在符合条件的座位以及用户选择的座位是否被占用
public class TrainBusinessCheckSeat implements TrainBitMapCheckSeat{
    /**
     * 高铁商务座是否存在检查方法
     *
     * @param key              缓存Key，用于从分布式缓存中获取座位相关信息
     * @param convert          座位统计Map，包含不同座位类型的数量
     * @param distributedCache 分布式缓存接口，用于操作缓存
     * @return 判断座位是否存在，存在返回true，否则返回false
     */
    @Override
    public boolean checkSeat(final String key, HashMap<Integer, Integer> convert, DistributedCache distributedCache) {
        // 初始化标志位，表示是否存在符合条件的座位
        boolean flag = false;
        // 获取用于操作字符串类型缓存值的ValueOperations对象
        ValueOperations<String, String> opsForValue = ((StringRedisTemplate) distributedCache.getInstance())
                .opsForValue();
        // 使用AtomicInteger来统计匹配的座位类型数量
        AtomicInteger matchCount = new AtomicInteger(0);
        // 遍历商务座可能的座位类型（这里假设为3种类型）
        for (int i = 0; i < 3; i++) {
            // 初始化当前座位类型的匹配计数
            int cnt = 0;
            // 如果座位统计Map中包含当前座位类型
            if (convert.containsKey(i)) {
                // 遍历每一排座位（这里假设商务座每排有2个座位）
                for (int j = 0; j < 2; j++) {
                    // 获取缓存中对应座位的占用状态（通过位操作）
                    Boolean bit = opsForValue.getBit(key, i + j * 3);
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
     * 高铁商务座选择座位是否被占用的检查方法
     *
     * @param chooseSeatList 选择座位列表，包含用户选择的座位信息
     * @param actualSeats    座位状态数组，用于表示车厢内座位的占用情况
     * @param SEAT_Y_INT     坐标转换Map，用于将座位字母标识转换为整数索引
     * @return 用户选择的座位是否都可用，都可用返回true，否则返回false
     */
    @Override
    public boolean checkChooseSeat(List<String> chooseSeatList, int[][] actualSeats,
                                   Map<Character, Integer> SEAT_Y_INT) {
        // 初始化标志位，表示用户选择的座位是否都可用
        boolean isExists = true;
        // 遍历用户选择的座位列表
        for (int i = 0; i < chooseSeatList.size(); i++) {
            // 如果用户只选择了一个座位
            if (chooseSeatList.size() == 1) {
                // 获取用户选择的座位编号
                String chooseSeat = chooseSeatList.get(i);
                // 获取座位的行号
                int seatX = Integer.parseInt(chooseSeat.substring(1));
                // 获取座位的列号（通过SEAT_Y_INT映射）
                int seatY = SEAT_Y_INT.get(chooseSeat.charAt(0));
                // 如果该座位和同一列的另一行座位都被占用，则设置标志位为false并跳出循环
                if (actualSeats[seatX][seatY] != 0 && actualSeats[1][seatY] != 0) {
                    break;
                }
            } else {
                // 如果用户选择了多个座位
                String chooseSeat = chooseSeatList.get(i);
                int seatX = Integer.parseInt(chooseSeat.substring(1));
                int seatY = SEAT_Y_INT.get(chooseSeat.charAt(0));
                // 如果该座位被占用，则设置标志位为false并跳出循环
                if (actualSeats[seatX][seatY] != 0) {
                    isExists = false;
                    break;
                }
            }
        }
        // 返回用户选择的座位是否都可用
        return isExists;
    }
}
