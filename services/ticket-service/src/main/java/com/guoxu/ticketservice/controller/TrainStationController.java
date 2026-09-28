package com.guoxu.ticketservice.controller;

import com.guoxu.Results;
import com.guoxu.result.Result;
import com.guoxu.ticketservice.dto.resp.TrainStationQueryRespDTO;
import com.guoxu.ticketservice.service.TrainStationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * TrainStationController
 * 火车站点控制器
 * @author 执笔画棠
 * @date 2025/11/12 20:25
 **/
@RestController
@RequiredArgsConstructor
public class TrainStationController {

    private final TrainStationService trainStationService;

    /**
     * 根据列车 ID 查询站点信息
     */
    @GetMapping("/api/ticket-service/train-station/query")
    public Result<List<TrainStationQueryRespDTO>> listTrainStationQuery(String trainId) {
        return Results.success(trainStationService.listTrainStationQuery(trainId));
    }
}
