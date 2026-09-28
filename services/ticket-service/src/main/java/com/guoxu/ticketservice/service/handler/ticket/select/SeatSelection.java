package com.guoxu.ticketservice.service.handler.ticket.select;

import cn.hutool.core.collection.CollUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * SeatSelection 座位选择器
 *
 * @author 执笔画棠
 * @date 2025/11/13 20:02
 **/
/*
 * 这个类主要实现了座位选择的逻辑。adjacent方法用于在给定的座位布局中选择相邻的座位，nonAdjacent方法用于选择不相邻的座位。
 * convertToActualSeat方法是一个辅助方法，用于将从 0 开始计数的座位坐标转换为从 1
 * 开始计数的实际座位位置。main方法提供了简单的测试，展示了如何使用这些方法来选择相邻和不相邻的座位
 */
// 包括选择相邻座位和不相邻座位，并包含一个main方法用于测试这些功能
public class SeatSelection {

    // 选择相邻座位的方法
    public static int[][] adjacent(int numSeats, int[][] seatLayout) {
        // 获取座位布局的行数
        int numRows = seatLayout.length;
        // 获取座位布局的列数
        int numCols = seatLayout[0].length;
        // 用于存储已选座位的列表
        List<int[]> selectedSeats = new ArrayList<>();
        // 遍历每一行
        for (int i = 0; i < numRows; i++) {
            // 遍历每一列
            for (int j = 0; j < numCols; j++) {
                // 如果当前座位为空（值为0）
                if (seatLayout[i][j] == 0) {
                    // 连续空座位计数器
                    int consecutiveSeats = 0;
                    // 从当前列开始往后检查连续空座位
                    for (int k = j; k < numCols; k++) {
                        if (seatLayout[i][k] == 0) {
                            consecutiveSeats++;
                            // 如果连续空座位数量达到所需座位数
                            if (consecutiveSeats == numSeats) {
                                // 将这连续的空座位添加到已选座位列表
                                for (int l = k - numSeats + 1; l <= k; l++) {
                                    selectedSeats.add(new int[] { i, l });
                                }
                                break;
                            }
                        } else {
                            // 如果遇到非空座位，重置计数器
                            consecutiveSeats = 0;
                        }
                    }
                    // 如果已选座位列表不为空，说明找到了足够的相邻座位，跳出内层循环
                    if (!selectedSeats.isEmpty()) {
                        break;
                    }
                }
            }
            // 如果已选座位列表不为空，说明找到了足够的相邻座位，跳出外层循环
            if (!selectedSeats.isEmpty()) {
                break;
            }
        }
        // 如果没有找到足够的相邻座位，返回null
        if (CollUtil.isEmpty(selectedSeats)) {
            return null;
        }
        // 创建一个数组来存储实际的座位位置（从1开始计数）
        int[][] actualSeat = new int[numSeats][2];
        int i = 0;
        // 将已选座位的坐标转换为从1开始计数，并存储到actualSeat数组中
        for (int[] seat : selectedSeats) {
            int row = seat[0] + 1;
            int col = seat[1] + 1;
            actualSeat[i][0] = row;
            actualSeat[i][1] = col;
            i++;
        }
        // 返回实际的座位位置数组
        return actualSeat;
    }

    // 选择不相邻座位的方法
    public static int[][] nonAdjacent(int numSeats, int[][] seatLayout) {
        // 获取座位布局的行数
        int numRows = seatLayout.length;
        // 获取座位布局的列数
        int numCols = seatLayout[0].length;
        // 用于存储已选座位的列表
        List<int[]> selectedSeats = new ArrayList<>();
        // 遍历每一行
        for (int i = 0; i < numRows; i++) {
            // 遍历每一列
            for (int j = 0; j < numCols; j++) {
                // 如果当前座位为空（值为0）
                if (seatLayout[i][j] == 0) {
                    // 将该座位添加到已选座位列表
                    selectedSeats.add(new int[] { i, j });
                    // 如果已选座位数量达到所需座位数，跳出内层循环
                    if (selectedSeats.size() == numSeats) {
                        break;
                    }
                }
            }
            // 如果已选座位数量达到所需座位数，跳出外层循环
            if (selectedSeats.size() == numSeats) {
                break;
            }
        }
        // 将已选座位的坐标转换为从1开始计数，并返回实际的座位位置数组
        return convertToActualSeat(selectedSeats);
    }

    // 将已选座位列表转换为实际座位位置数组（从1开始计数）的辅助方法
    private static int[][] convertToActualSeat(List<int[]> selectedSeats) {
        int[][] actualSeat = new int[selectedSeats.size()][2];
        for (int i = 0; i < selectedSeats.size(); i++) {
            int[] seat = selectedSeats.get(i);
            int row = seat[0] + 1;
            int col = seat[1] + 1;
            actualSeat[i][0] = row;
            actualSeat[i][1] = col;
        }
        return actualSeat;
    }

    // main方法，用于测试相邻座位和不相邻座位选择功能
    public static void main(String[] args) {
        // 定义一个座位布局示例
        int[][] seatLayout = {
                { 1, 1, 1, 1 },
                { 1, 1, 1, 0 },
                { 1, 1, 1, 0 },
                { 0, 0, 0, 0 }
        };
        // 调用adjacent方法选择2个相邻座位
        int[][] select = adjacent(2, seatLayout);
        System.out.println("成功预订相邻座位，座位位置为：");
        // 确保select不为null，然后打印座位位置
        assert select != null;
        for (int[] ints : select) {
            System.out.printf("第 %d 排，第 %d 列%n", ints[0], ints[1]);
        }

        // 定义另一个座位布局示例
        int[][] seatLayoutTwo = {
                { 1, 0, 1, 1 },
                { 1, 1, 0, 0 },
                { 1, 1, 1, 0 },
                { 0, 0, 0, 0 }
        };
        // 调用nonAdjacent方法选择3个不相邻座位
        int[][] selectTwo = nonAdjacent(3, seatLayoutTwo);
        System.out.println("成功预订不相邻座位，座位位置为：");
        // 打印不相邻座位的位置
        for (int[] ints : selectTwo) {
            System.out.printf("第 %d 排，第 %d 列%n", ints[0], ints[1]);
        }
    }
}