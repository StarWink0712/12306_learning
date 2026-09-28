package com.guoxu.ticketservice.service.handler.ticket.filter.purchase;

import cn.hutool.core.util.StrUtil;
import com.guoxu.DistributedCache;
import com.guoxu.exception.ClientException;
import com.guoxu.ticketservice.dto.domain.PurchaseTicketPassengerDetailDTO;
import com.guoxu.ticketservice.dto.req.PurchaseTicketReqDTO;
import com.guoxu.ticketservice.service.cache.SeatMarginCacheLoader;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.guoxu.ticketservice.common.constant.RedisKeyConstant.TRAIN_STATION_REMAINING_TICKET;

/**
 * TrainPurchaseTicketParamStockChainHandler
 * 购票流程过滤器之验证列车站点库存是否充足
 * @author 执笔画棠
 * @date 2025/11/13 19:47
 **/
@Component
@RequiredArgsConstructor
// TrainPurchaseTicketParamStockChainHandler类实现了TrainPurchaseTicketChainFilter接口，
// 用于在购票流程中检查车次站点的余票情况
public class TrainPurchaseTicketParamStockChainHandler implements TrainPurchaseTicketChainFilter<PurchaseTicketReqDTO> {

    // 注入座位余量缓存加载器，用于加载座位余量数据
    private final SeatMarginCacheLoader seatMarginCacheLoader;
    // 注入分布式缓存对象，用于操作缓存
    private final DistributedCache distributedCache;

    // 处理购票请求，检查车次站点的余票情况
    @Override
    public void handler(PurchaseTicketReqDTO requestParam) {
        // 构建缓存键的后缀，格式为“列车ID_出发站_到达站”
        String keySuffix = StrUtil.join("_", requestParam.getTrainId(), requestParam.getDeparture(),
                requestParam.getArrival());
        // 从分布式缓存中获取StringRedisTemplate实例，用于操作Redis缓存
        StringRedisTemplate stringRedisTemplate = (StringRedisTemplate) distributedCache.getInstance();
        // 获取购票请求中的乘客详细信息列表，包含乘客姓名、座位类型
        List<PurchaseTicketPassengerDetailDTO> passengerDetails = requestParam.getPassengers();
        // 根据座位类型对乘客详细信息进行分组
        Map<Integer, List<PurchaseTicketPassengerDetailDTO>> seatTypeMap = passengerDetails.stream()
                .collect(Collectors.groupingBy(PurchaseTicketPassengerDetailDTO::getSeatType));
        // 遍历每个座位类型及其对应的乘客详细信息列表
        seatTypeMap.forEach((seatType, passengerSeatDetails) -> {
            // 从缓存中获取指定座位类型在该车次站点的余票数量
            /*
             * 这里的流程和ticketservice里面的查询余票缓存的流程是一样的
             * 都是TRAIN_STATION_REMAINING_TICKET+keysuffix
             * suffix是由列车id+出发站到达站，哈希型数据结构，seattype就是valua中键值对的健
             * 查出来就是余票数量
             * 从缓存中获取指定座位类型在该车次站点的余票数量
             * 如果缓存中没有，从缓存加载器中加载
             */
            Object stockObj = stringRedisTemplate.opsForHash().get(TRAIN_STATION_REMAINING_TICKET + keySuffix,
                    String.valueOf(seatType));
            // 将获取到的余票数量转换为整数，如果为空则从缓存加载器中获取
            int stock = Optional.ofNullable(stockObj).map(each -> Integer.parseInt(each.toString())).orElseGet(() -> {
                Map<String, String> seatMarginMap = seatMarginCacheLoader.load(
                        String.valueOf(requestParam.getTrainId()), String.valueOf(seatType),
                        requestParam.getDeparture(), requestParam.getArrival());
                return Optional.ofNullable(seatMarginMap.get(String.valueOf(seatType))).map(Integer::parseInt)
                        .orElse(0);
            });
            // 如果余票数量大于等于该座位类型的乘客数量，则继续处理下一种座位类型
            if (stock >= passengerSeatDetails.size()) {
                return;
            }
            // 如果余票不足，抛出异常提示列车站点已无余票
            throw new ClientException("列车站点已无余票");
        });
    }

    // 获取该过滤器在过滤链中的执行顺序
    @Override
    public int getOrder() {
        return 20;
    }
}