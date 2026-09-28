package com.guoxu.ticketservice.toolkit;

import com.guoxu.ticketservice.dto.domain.RouteDTO;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * StationCalculateUtil
 * 站点计算工具
 * 该类提供了两个静态方法throughStation和takeoutStation，
 * 分别用于计算出发站和终点站之间的经过站点以及需要扣减余票的站点。
 * throughStation方法生成从出发站到终点站之间所有可能的站点对。
 * takeoutStation方法不仅生成出发站到终点站之间的站点对，
 * 还会考虑出发站之前的站点与出发站及之后站点的组合。
 * 主方法main用于测试takeoutStation方法。
 * @author 执笔画棠
 * @date 2025/11/12 22:35
 **/
// 定义一个不可继承的最终类StationCalculateUtil，用于处理与站点计算相关的工具方法
public final class StationCalculateUtil {

    /**
     * 计算出发站和终点站中间的站点（包含出发站和终点站）
     *
     * @param stations     所有站点数据，是一个包含站点名称的列表
     * @param startStation 出发站名称
     * @param endStation   终点站名称
     * @return 包含出发站和终点站中间站点（包含出发站和终点站）的RouteDTO列表
     */
    public static List<RouteDTO> throughStation(List<String> stations, String startStation, String endStation) {
        // 创建一个列表用于存储经过的站点信息
        List<RouteDTO> routesToDeduct = new ArrayList<>();
        // 获取出发站在站点列表中的索引
        int startIndex = stations.indexOf(startStation);
        // 获取终点站在站点列表中的索引
        int endIndex = stations.indexOf(endStation);
        // 如果出发站或终点站索引无效，或者出发站索引大于等于终点站索引，则返回空列表
        if (startIndex < 0 || endIndex < 0 || startIndex >= endIndex) {
            return routesToDeduct;
        }
        // 遍历从出发站到终点站之间的站点
        for (int i = startIndex; i < endIndex; i++) {
            for (int j = i + 1; j <= endIndex; j++) {
                // 获取当前站点和下一个站点的名称
                String currentStation = stations.get(i);
                String nextStation = stations.get(j);
                // 创建一个RouteDTO对象表示这两个站点之间的路线，并添加到列表中
                RouteDTO routeDTO = new RouteDTO(currentStation, nextStation);
                routesToDeduct.add(routeDTO);
            }
        }
        // 返回包含经过站点信息的列表
        return routesToDeduct;
    }

    /**
     * 计算出发站和终点站需要扣减余票的站点（包含出发站和终点站）
     *
     * @param stations     所有站点数据，是一个包含站点名称的列表
     * @param startStation 出发站名称
     * @param endStation   终点站名称
     * @return 包含出发站和终点站需要扣减余票站点（包含出发站和终点站）的RouteDTO列表
     */
    public static List<RouteDTO> takeoutStation(List<String> stations, String startStation, String endStation) {
        // 创建一个列表用于存储需要扣减余票的站点信息
        List<RouteDTO> takeoutStationList = new ArrayList<>();
        // 获取出发站在站点列表中的索引
        int startIndex = stations.indexOf(startStation);
        // 获取终点站在站点列表中的索引
        int endIndex = stations.indexOf(endStation);
        // 如果出发站或终点站索引无效，或者出发站索引大于等于终点站索引，则返回空列表
        if (startIndex == -1 || endIndex == -1 || startIndex >= endIndex) {
            return takeoutStationList;
        }
        // 如果出发站不是第一个站点
        if (startIndex != 0) {
            // 遍历出发站之前的站点
            for (int i = 0; i < startIndex; i++) {
                // 遍历出发站及之后的站点
                for (int j = 1; j < stations.size() - startIndex; j++) {
                    // 创建一个RouteDTO对象表示这两个站点之间的路线，并添加到列表中
                    takeoutStationList.add(new RouteDTO(stations.get(i), stations.get(startIndex + j)));
                }
            }
        }
        // 遍历从出发站到终点站之间的站点
        for (int i = startIndex; i <= endIndex; i++) {
            // 遍历当前站点之后的站点
            for (int j = i + 1; j < stations.size() && i < endIndex; j++) {
                // 创建一个RouteDTO对象表示这两个站点之间的路线，并添加到列表中
                takeoutStationList.add(new RouteDTO(stations.get(i), stations.get(j)));
            }
        }
        // 返回包含需要扣减余票站点信息的列表
        return takeoutStationList;
    }

    // 主方法，用于测试takeoutStation方法
    public static void main(String[] args) {
        // 定义站点列表
        List<String> stations = Arrays.asList("北京南", "济南西", "南京南", "杭州东", "宁波");
        // 定义出发站
        String startStation = "北京南";
        // 定义终点站
        String endStation = "南京南";
        // 调用takeoutStation方法并打印结果
        StationCalculateUtil.takeoutStation(stations, startStation, endStation).forEach(System.out::println);
    }
}