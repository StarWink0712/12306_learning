package com.guoxu.ticketservice.service.handler.ticket.base;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.guoxu.ApplicationContextHolder;
import com.guoxu.DistributedCache;
import com.guoxu.strategy.AbstractExecuteStrategy;
import com.guoxu.ticketservice.dto.domain.RouteDTO;
import com.guoxu.ticketservice.dto.domain.TrainSeatBaseDTO;
import com.guoxu.ticketservice.service.TrainStationService;
import com.guoxu.ticketservice.service.handler.ticket.dto.SelectSeatDTO;
import com.guoxu.ticketservice.service.handler.ticket.dto.TrainPurchaseTicketRespDTO;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.List;

import static com.guoxu.ticketservice.common.constant.RedisKeyConstant.TRAIN_STATION_REMAINING_TICKET;

/**
 * AbstractTrainPurchaseTicketTemplate
 * 抽象高铁购票模板基础服务
 * @author 执笔画棠
 * @date 2025/11/13 15:25
 **/
// 定义一个抽象类，实现了IPurchaseTicket、CommandLineRunner和AbstractExecuteStrategy接口
// 作为火车购票模板的抽象基类，为具体的购票处理器提供通用的方法和结构
public abstract class AbstractTrainPurchaseTicketTemplate implements IPurchaseTicket, CommandLineRunner,
        AbstractExecuteStrategy<SelectSeatDTO, List<TrainPurchaseTicketRespDTO>> {
    // 分布式缓存对象，用于操作缓存
    private DistributedCache distributedCache;
    // 车票可用性缓存更新类型，用于判断是否通过binlog更新缓存
    private String ticketAvailabilityCacheUpdateType;
    // 列车站点服务对象，用于获取列车站点相关信息
    private TrainStationService trainStationService;

    /**
     * 选择座位的抽象方法，具体的座位选择逻辑由子类实现
     *调用子类的selectSeats方法选择座位，并在满足条件时扣减余票缓存
     *      *       执行具体的座位选择逻辑，各个子类处理自己的选择逻辑
     * @param requestParam 购票请求入参，包含列车ID、出发站、到达站、乘客座位详细信息等
     * @return 乘车人座位列表，包含每个乘客所选座位的详细信息
     */
    protected abstract List<TrainPurchaseTicketRespDTO> selectSeats(SelectSeatDTO requestParam);

    /**
     * 构建列车座位基础数据传输对象（TrainSeatBaseDTO）的方法
     *
     * @param requestParam 购票请求入参
     * @return 构建好的TrainSeatBaseDTO对象，包含列车ID、出发站、到达站、用户选择的座位列表和乘客座位详细信息
     */
    protected TrainSeatBaseDTO buildTrainSeatBaseDTO(SelectSeatDTO requestParam) {
        return TrainSeatBaseDTO.builder()
                .trainId(requestParam.getRequestParam().getTrainId())
                .departure(requestParam.getRequestParam().getDeparture())
                .arrival(requestParam.getRequestParam().getArrival())
                .chooseSeatList(requestParam.getRequestParam().getChooseSeats())
                .passengerSeatDetails(requestParam.getPassengerSeatDetails())
                .build();
    }

    /**
     * 执行购票并返回结果的方法，
     * @param requestParam 购票请求入参
     * @return 乘车人座位列表，包含每个乘客所选座位的详细信息
     */
    @Override
    public List<TrainPurchaseTicketRespDTO> executeResp(SelectSeatDTO requestParam) {
        // 调用子类的selectSeats方法选择座位
        List<TrainPurchaseTicketRespDTO> actualResult = selectSeats(requestParam);
        // 如果选择的座位列表不为空，且车票可用性缓存更新类型不是"binlog"
        if (CollUtil.isNotEmpty(actualResult) && !StrUtil.equals(ticketAvailabilityCacheUpdateType, "binlog")) {
            // 获取请求参数中的列车ID
            String trainId = requestParam.getRequestParam().getTrainId();
            // 获取请求参数中的出发站
            String departure = requestParam.getRequestParam().getDeparture();
            // 获取请求参数中的到达站
            String arrival = requestParam.getRequestParam().getArrival();
            // 从分布式缓存中获取StringRedisTemplate实例，用于操作Redis缓存
            StringRedisTemplate stringRedisTemplate = (StringRedisTemplate) distributedCache.getInstance();
            // 获取列车在指定区间内需要扣减余票的站点路线信息
            List<RouteDTO> routeDTOList = trainStationService.listTakeoutTrainStationRoute(trainId, departure, arrival);
            // 遍历每个站点路线
            routeDTOList.forEach(each -> {
                // 构建缓存键的后缀
                String keySuffix = StrUtil.join("_", trainId, each.getStartStation(), each.getEndStation());
                // 在Redis哈希表中，对相应的座位类型余票数量进行扣减
                stringRedisTemplate.opsForHash().increment(TRAIN_STATION_REMAINING_TICKET + keySuffix,
                        String.valueOf(requestParam.getSeatType()), -actualResult.size());
            });
        }
        // 返回选择的座位列表
        return actualResult;
    }

    /**
     * Spring Boot应用启动时执行的方法，用于初始化相关的Bean和配置参数
     *
     * @param args 命令行参数
     * @throws Exception 如果在初始化过程中发生异常
     */
    @Override
    public void run(String... args) throws Exception {
        // 从Spring应用上下文中获取DistributedCache实例
        distributedCache = ApplicationContextHolder.getBean(DistributedCache.class);
        // 从Spring应用上下文中获取TrainStationService实例
        trainStationService = ApplicationContextHolder.getBean(TrainStationService.class);
        // 从Spring应用上下文中获取ConfigurableEnvironment实例，用于获取配置参数
        ConfigurableEnvironment configurableEnvironment = ApplicationContextHolder
                .getBean(ConfigurableEnvironment.class);
        // 获取车票可用性缓存更新类型的配置参数，默认为空字符串
        ticketAvailabilityCacheUpdateType = configurableEnvironment.getProperty("ticket.availability.cache-update.type",
                "");
    }

}
