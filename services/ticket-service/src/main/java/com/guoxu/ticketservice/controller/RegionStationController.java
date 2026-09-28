package com.guoxu.ticketservice.controller;

import com.guoxu.Results;
import com.guoxu.result.Result;
import com.guoxu.ticketservice.dto.req.RegionStationQueryReqDTO;
import com.guoxu.ticketservice.dto.resp.RegionStationQueryRespDTO;
import com.guoxu.ticketservice.dto.resp.StationQueryRespDTO;
import com.guoxu.ticketservice.service.RegionStationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * RegionStationController 地区以及车站查询控制层
 *
 * @author 执笔画棠
 * @date 2025/11/12 20:20
 **/
@RestController
@RequiredArgsConstructor
public class RegionStationController {

    private final RegionStationService regionStationService;

    /**
     * 查询车站&城市站点集合信息
     */
    @GetMapping("/api/ticket-service/region-station/query")
    public Result<List<RegionStationQueryRespDTO>> listRegionStation(RegionStationQueryReqDTO requestParam) {
        return Results.success(regionStationService.listRegionStation(requestParam));
    }

    /**
     * 查询车站站点集合信息
     */
    @GetMapping("/api/ticket-service/station/all")
    public Result<List<StationQueryRespDTO>> listAllStation() {
        return Results.success(regionStationService.listAllStation());
    }
}
