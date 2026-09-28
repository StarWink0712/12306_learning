package com.guoxu.ticketservice.toolkit;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.lang.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.PriorityQueue;

/**
 * SurplusNeedMatchSeatUtil
 *
 * @author 执笔画棠
 * @date 2025/11/12 22:37
 **/
// 空余座位匹配工具类，声明为final表示不可被继承
public final class SurplusNeedMatchSeatUtil {

    /**
     * 匹配指定数量的空余座位方法
     *
     * @param chooseSeatSize  选择座位数量
     * @param vacantSeatQueue 空余座位集合，使用优先队列存储
     * @return 获取选择座位数量的空余座位集合 (获取数量可能小于选择座位数量)
     */
    // 静态工具方法：根据需要的座位数量从空余座位队列中匹配座位
    public static List<Pair<Integer, Integer>> getSurplusNeedMatchSeat(int chooseSeatSize,
                                                                       PriorityQueue<List<Pair<Integer, Integer>>> vacantSeatQueue) {
        // 使用并行流在空余座位队列中查找第一个座位数量足够的选择
        Optional<List<Pair<Integer, Integer>>> optionalList = vacantSeatQueue.parallelStream()
                // 过滤条件：每个座位列表的大小要大于等于需要的座位数量
                .filter(each -> each.size() >= chooseSeatSize).findFirst();
        // 判断是否找到了符合条件的座位列表
        if (optionalList.isPresent()) {
            // 如果找到了，直接返回该列表的前chooseSeatSize个座位
            return optionalList.get().subList(0, chooseSeatSize);
        }
        // 如果没有找到足够数量的连续座位，创建结果列表，初始容量为需要的座位数量
        List<Pair<Integer, Integer>> result = new ArrayList<>(chooseSeatSize);
        // 循环处理，直到空余座位队列为空或者已经收集到足够的座位
        while (CollUtil.isNotEmpty(vacantSeatQueue)) {
            // 从优先队列中取出一个座位列表（通常是座位数量最多的列表）
            List<Pair<Integer, Integer>> pairList = vacantSeatQueue.poll();
            // 情况1：当前结果大小加上新列表大小仍小于需要的座位数量
            if (result.size() + pairList.size() < chooseSeatSize) {
                // 将整个列表添加到结果中
                result.addAll(pairList);
            }
            // 情况2：当前结果大小加上新列表大小大于等于需要的座位数量
            else if (result.size() + pairList.size() >= chooseSeatSize) {
                // 计算需要从当前列表中取出的座位数量
                int needPairListLen = pairList.size() - (result.size() + pairList.size() - chooseSeatSize);
                // 从当前列表中取出需要的部分座位添加到结果中
                result.addAll(pairList.subList(0, needPairListLen));
                // 检查是否已经收集到足够的座位数量
                if (result.size() == chooseSeatSize) {
                    // 如果已经足够，跳出循环
                    break;
                }
            }
        }
        // 返回最终的结果列表（可能小于需要的座位数量）
        return result;
    }
}