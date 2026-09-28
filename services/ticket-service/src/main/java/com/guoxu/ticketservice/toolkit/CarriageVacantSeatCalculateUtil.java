package com.guoxu.ticketservice.toolkit;

import cn.hutool.core.lang.Pair;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

/**
 * CarriageVacantSeatCalculateUtil
 *
 * @author 执笔画棠
 * @date 2025/11/12 22:32
 **/
// 定义一个不可继承的最终类CarriageVacantSeatCalculateUtil，用于计算车厢内的空余座位
public final class CarriageVacantSeatCalculateUtil {

    /**
     * 座位统计方法
     *
     * @param actualSeats 座位选座情况二维数组，0 表示座位空闲，1 表示座位已被占用
     * @param n           车厢座位的列数
     * @param m           车厢座位的行数
     * @return 空余座位集合小根堆，按照每个空余座位列表的长度从小到大排序
     */
    public static PriorityQueue<List<Pair<Integer, Integer>>> buildCarriageVacantSeatList(int[][] actualSeats, int n,
                                                                                          int m) {
        // 创建一个小根堆，用于存储空余座位集合，按照每个空余座位列表的长度从小到大排序
        PriorityQueue<List<Pair<Integer, Integer>>> vacantSeatQueue = new PriorityQueue<>(
                Comparator.comparingInt(List::size));
        // 遍历车厢座位的每一行
        for (int i = 0; i < n; i++) {
            // 遍历车厢座位的每一列
            for (int j = 0; j < m; j++) {
                // 如果当前座位空闲
                if (actualSeats[i][j] == 0) {
                    // 创建一个列表用于存储连续的空闲座位
                    List<Pair<Integer, Integer>> res = new ArrayList<>();
                    int k = j;
                    // 从当前列开始往后查找连续的空闲座位
                    for (; k < m; k++) {
                        // 如果遇到已被占用的座位则停止查找
                        if (actualSeats[i][k] == 1)
                            break;
                        // 将空闲座位的行列坐标加入列表
                        res.add(new Pair<>(i, k));
                    }
                    // 更新列索引，跳过已处理的座位
                    j = k;
                    // 将包含连续空闲座位的列表加入小根堆
                    vacantSeatQueue.add(res);
                }
            }
        }
        // 返回包含所有连续空闲座位列表的小根堆
        return vacantSeatQueue;
    }

    /**
     * 空余座位统计方法
     *
     * @param actualSeats 座位状态数组，0 表示座位空闲，1 表示座位已被占用
     * @param n           车厢座位的列数
     * @param m           车厢座位的行数
     * @return 空余座位集合，包含所有空闲座位的坐标
     */
    public static List<Pair<Integer, Integer>> buildCarriageVacantSeatList2(int[][] actualSeats, int n, int m) {
        // 创建一个列表用于存储所有空闲座位
        List<Pair<Integer, Integer>> vacantSeatList = new ArrayList<>(16);
        // 遍历车厢座位的每一行
        for (int i = 0; i < n; i++) {
            // 遍历车厢座位的每一列
            for (int j = 0; j < m; j++) {
                // 如果当前座位空闲
                if (actualSeats[i][j] == 0) {
                    // 将空闲座位的行列坐标加入列表
                    vacantSeatList.add(new Pair<>(i, j));
                }
            }
        }
        // 返回包含所有空闲座位坐标的列表
        return vacantSeatList;
    }
}