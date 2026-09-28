package com.guoxu.ticketservice.service.handler.ticket.base;

import com.guoxu.DistributedCache;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * BitMapCheckSeat
 * 抽象的验证座位实体类
 * @author 执笔画棠
 * @date 2025/11/13 19:31
 **/
public interface  BitMapCheckSeat {
    /**
     * 座位是否存在检查方法
     *
     * @param key              缓存Key
     * @param convert          座位统计Map
     * @param distributedCache 分布式缓存接口
     * @return 判断座位是否存在 true or false
     */
    boolean checkSeat(String key, HashMap<Integer, Integer> convert, DistributedCache distributedCache);

    /**
     * 检查座位是否存在 v2 版本
     *
     * @param chooseSeatList 选择座位
     * @param actualSeats    座位状态数组
     * @param SEAT_Y_INT     坐标转换 Map
     * @return
     */
    boolean checkChooseSeat(List<String> chooseSeatList, int[][] actualSeats, Map<Character, Integer> SEAT_Y_INT);
}
