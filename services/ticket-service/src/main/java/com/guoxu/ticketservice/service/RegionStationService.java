package com.guoxu.ticketservice.service;

import com.guoxu.ticketservice.dto.req.RegionStationQueryReqDTO;
import com.guoxu.ticketservice.dto.resp.RegionStationQueryRespDTO;
import com.guoxu.ticketservice.dto.resp.StationQueryRespDTO;

import java.util.List;

/**
 * RegionStationService 地区及车站接口层
 *
 * @author 执笔画棠
 * @version 2025/11/13 20:27
 **/
public interface RegionStationService {
    /**
     * 查询车站&城市站点集合信息
     *
     * @param requestParam 车站&站点查询参数
     * @return 车站&站点返回数据集合
     */
    List<RegionStationQueryRespDTO> listRegionStation(RegionStationQueryReqDTO requestParam);

    /**
     * 查询所有车站&城市站点集合信息
     *
     * @return 车站返回数据集合
     */
    List<StationQueryRespDTO> listAllStation();
}