package com.guoxu.ticketservice.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.guoxu.ticketservice.dao.entity.TrainStationDO;
import com.guoxu.ticketservice.dao.mapper.TrainStationMapper;
import com.guoxu.ticketservice.dto.domain.RouteDTO;
import com.guoxu.ticketservice.dto.resp.TrainStationQueryRespDTO;
import com.guoxu.ticketservice.service.TrainStationService;
import com.guoxu.ticketservice.toolkit.StationCalculateUtil;
import com.guoxu.toolkit.BeanUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * TrainStationServiceImpl 列车站点接口实现层
 *
 * @author 执笔画棠
 * @date 2025/11/13 20:37
 **/
@Service
@RequiredArgsConstructor
public class TrainStationServiceImpl implements TrainStationService {
    // 注入火车站点数据访问对象，用于数据库操作
    private final TrainStationMapper trainStationMapper;

    // 根据列车ID列出该列车的站点查询响应数据
    @Override
    public List<TrainStationQueryRespDTO> listTrainStationQuery(String trainId) {
        // 构建查询条件，查询指定列车ID的火车站点数据
        LambdaQueryWrapper<TrainStationDO> queryWrapper = Wrappers.lambdaQuery(TrainStationDO.class)
                .eq(TrainStationDO::getTrainId, trainId);
        // 从数据库中查询符合条件的火车站点数据列表
        List<TrainStationDO> trainStationDOList = trainStationMapper.selectList(queryWrapper);
        // 将火车站点数据列表转换为TrainStationQueryRespDTO类型的列表并返回
        return BeanUtil.convert(trainStationDOList, TrainStationQueryRespDTO.class);
    }

    // 根据列车ID、出发站和到达站列出列车经过的站点路线数据
    @Override
    public List<RouteDTO> listTrainStationRoute(String trainId, String departure, String arrival) {
        // 构建查询条件，查询指定列车ID的火车站点数据，并只选择出发站字段
        LambdaQueryWrapper<TrainStationDO> queryWrapper = Wrappers.lambdaQuery(TrainStationDO.class)
                .eq(TrainStationDO::getTrainId, trainId)
                .select(TrainStationDO::getDeparture);
        // 从数据库中查询符合条件的火车站点数据列表
        List<TrainStationDO> trainStationDOList = trainStationMapper.selectList(queryWrapper);
        // 提取所有出发站名称并收集到一个列表中
        List<String> trainStationAllList = trainStationDOList.stream().map(TrainStationDO::getDeparture)
                .collect(Collectors.toList());
        // 通过StationCalculateUtil工具类计算列车经过的站点路线，并返回结果
        return StationCalculateUtil.throughStation(trainStationAllList, departure, arrival);
    }

    // 根据列车ID、出发站和到达站列出需要扣减余票的火车站点路线数据
    @Override
    public List<RouteDTO> listTakeoutTrainStationRoute(String trainId, String departure, String arrival) {
        // 构建查询条件，查询指定列车ID的火车站点数据，并只选择出发站字段
        LambdaQueryWrapper<TrainStationDO> queryWrapper = Wrappers.lambdaQuery(TrainStationDO.class)
                .eq(TrainStationDO::getTrainId, trainId)
                .select(TrainStationDO::getDeparture);
        // 从数据库中查询符合条件的火车站点数据列表
        List<TrainStationDO> trainStationDOList = trainStationMapper.selectList(queryWrapper);
        // 提取所有出发站名称并收集到一个列表中
        List<String> trainStationAllList = trainStationDOList.stream().map(TrainStationDO::getDeparture)
                .collect(Collectors.toList());
        // 通过StationCalculateUtil工具类计算需要扣减余票的站点路线，并返回结果
        return StationCalculateUtil.takeoutStation(trainStationAllList, departure, arrival);
    }
}
