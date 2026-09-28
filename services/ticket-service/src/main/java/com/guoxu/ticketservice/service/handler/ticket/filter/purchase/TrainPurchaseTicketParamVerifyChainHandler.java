package com.guoxu.ticketservice.service.handler.ticket.filter.purchase;

import cn.hutool.core.collection.CollUtil;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.guoxu.DistributedCache;
import com.guoxu.exception.ClientException;
import com.guoxu.ticketservice.common.constant.Index12306Constant;
import com.guoxu.ticketservice.dao.entity.TrainDO;
import com.guoxu.ticketservice.dao.entity.TrainStationDO;
import com.guoxu.ticketservice.dao.mapper.TrainMapper;
import com.guoxu.ticketservice.dao.mapper.TrainStationMapper;
import com.guoxu.ticketservice.dto.req.PurchaseTicketReqDTO;
import com.guoxu.toolkit.EnvironmentUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import static com.guoxu.ticketservice.common.constant.Index12306Constant.ADVANCE_TICKET_DAY;
import static com.guoxu.ticketservice.common.constant.RedisKeyConstant.TRAIN_INFO;
import static com.guoxu.ticketservice.common.constant.RedisKeyConstant.TRAIN_STATION_STOPOVER_DETAIL;

/**
 * TrainPurchaseTicketParamVerifyChainHandler
 * 购票流程过滤器之验证参数是否有效
 * 验证参数有效这个流程会大量交互缓存，为了优化性能需要使用 Lua。为了方便大家理解流程，这里使用多次调用缓存
 * @author 执笔画棠
 * @date 2025/11/13 19:49
 **/
@Component
@RequiredArgsConstructor
public class TrainPurchaseTicketParamVerifyChainHandler implements TrainPurchaseTicketChainFilter<PurchaseTicketReqDTO> {
    // TrainMapper：用于操作车次表（如查询 TrainDO）
    private final TrainMapper trainMapper;

    // TrainStationMapper：用于操作车站表（如查询 TrainStationDO）
    private final TrainStationMapper trainStationMapper;

    // 分布式缓存（如 Redis），用于高性能缓存车次信息、车站停靠信息等，避免频繁查库
    private final DistributedCache distributedCache;

    /**
     * 责任链模式中的核心处理方法：对用户购票请求参数进行校验
     */
    @Override
    public void handler(PurchaseTicketReqDTO requestParam) {
        // 1. 查询车次是否存在（通过 trainId）
        // 使用分布式缓存的 safeGet 方法，安全地获取缓存中的 TrainDO，如果缓存没有，则通过 Lambda 表达式从数据库查询并缓存起来
        // 缓存 key: TRAIN_INFO + trainId，缓存时间：ADVANCE_TICKET_DAY 天，缓存类型：TrainDO
        TrainDO trainDO = distributedCache.safeGet(
                TRAIN_INFO + requestParam.getTrainId(), // 缓存 key
                TrainDO.class, // 缓存对象类型
                () -> trainMapper.selectById(requestParam.getTrainId()), // 如果缓存没有，则通过该 Lambda 从数据库加载
                ADVANCE_TICKET_DAY, // 缓存时间（常量，比如 30 天？）
                TimeUnit.DAYS // 时间单位
        );

        // 如果车次不存在（null），抛出客户端异常，提示用户检查车次
        if (Objects.isNull(trainDO)) {
            // TODO：实际项目中应该记录用户 ID 并接入风控系统，多次异常可封号
            throw new ClientException("请检查车次是否存在");
        }

        // TODO：当前列车数据并非由定时任务每日生成，因此暂时屏蔽部分校验，待定时任务上线后再删除此判断
        if (!EnvironmentUtil.isDevEnvironment()) { // 仅在非开发环境进行以下校验
            // 2. 校验车次是否已经发售（即当前时间是否早于开售时间）
            if (new Date().before(trainDO.getSaleTime())) {
                throw new ClientException("列车车次暂未发售");
            }

            // 3. 校验车次是否已经出发（即当前时间是否晚于发车时间）
            if (new Date().after(trainDO.getDepartureTime())) {
                throw new ClientException("列车车次已出发禁止购票");
            }
        }

        // 4. 校验用户选择的【出发站】和【到达站】是否在车次的停靠站点中，且出发站在到达站之前

        // 4.1 先查询该车次的所有停靠站点信息（只查 departure 字段，即车站名）
        // 缓存 key: TRAIN_STATION_STOPOVER_DETAIL + trainId
        // 缓存类型: String（其实是 JSON 格式的 List<TrainStationDO>）
        // 如果缓存没有，则从数据库查询该 trainId 对应的所有车站，并只选取 departure 字段，然后转为 JSON 串存入缓存
        String trainStationStopoverDetailStr = distributedCache.safeGet(
                TRAIN_STATION_STOPOVER_DETAIL + requestParam.getTrainId(),
                String.class,
                () -> {
                    // 构造查询条件：查询指定车次的所有车站信息
                    LambdaQueryWrapper<TrainStationDO> queryWrapper = Wrappers.lambdaQuery(TrainStationDO.class)
                            .eq(TrainStationDO::getTrainId, requestParam.getTrainId())
                            .select(TrainStationDO::getDeparture); // 只查车站名（departure）

                    // 执行查询，获取车站列表
                    List<TrainStationDO> actualTrainStationList = trainStationMapper.selectList(queryWrapper);

                    // 如果查询结果非空，则将 List<TrainStationDO> 转为 JSON 字符串；否则返回 null
                    return CollUtil.isNotEmpty(actualTrainStationList)
                            ? JSON.toJSONString(actualTrainStationList)
                            : null;
                },
                Index12306Constant.ADVANCE_TICKET_DAY, // 缓存时间
                TimeUnit.DAYS // 时间单位
        );

        // 4.2 将缓存的 JSON 字符串，反序列化为 List<TrainStationDO>
        List<TrainStationDO> trainDOList = JSON.parseArray(trainStationStopoverDetailStr, TrainStationDO.class);

        // 4.3 自定义方法校验：用户传入的出发站和到达站，是否都存在于车站列表中，且出发站在到达站之前（按车站顺序）
        boolean validateStation = validateStation(
                trainDOList.stream().map(TrainStationDO::getDeparture).toList(), // 提取所有车站名，组成 List<String>
                requestParam.getDeparture(), // 用户选择的出发站
                requestParam.getArrival() // 用户选择的到达站
        );

        // 如果校验失败，说明车站选择有误，抛出异常
        if (!validateStation) {
            throw new ClientException("列车车站数据错误");
        }
    }

    /**
     * 责任链模式中，用于定义当前 Filter 的执行顺序，数值越小越先执行
     */
    @Override
    public int getOrder() {
        return 10; // 本 Filter 的执行顺序为 10
    }

    /**
     * 自定义校验方法：判断用户选择的出发站和到达站是否合法
     * 即：两个站都必须存在于车站列表中，且出发站的索引 <= 到达站的索引（出发站在到达站之前或相同，但一般应该之前）
     *
     * @param stationList  车次的所有车站名列表（按顺序）
     * @param startStation 用户选择的出发站
     * @param endStation   用户选择的到达站
     * @return 校验通过返回 true，否则返回 false
     */
    public boolean validateStation(List<String> stationList, String startStation, String endStation) {
        // 获取出发站和到达站在车站列表中的索引位置
        int index1 = stationList.indexOf(startStation);
        int index2 = stationList.indexOf(endStation);

        // 如果任一车站不存在于列表中，返回校验失败
        if (index1 == -1 || index2 == -1) {
            return false;
        }

        // 校验到达站是否在出发站之后（或同一位置，但通常应该严格在前）
        return index2 >= index1;
    }
}
