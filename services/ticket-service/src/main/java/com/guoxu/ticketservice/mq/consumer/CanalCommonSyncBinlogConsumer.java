package com.guoxu.ticketservice.mq.consumer;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.guoxu.annotation.Idempotent;
import com.guoxu.enums.IdempotentSceneEnum;
import com.guoxu.enums.IdempotentTypeEnum;
import com.guoxu.strategy.AbstractStrategyChoose;
import com.guoxu.ticketservice.common.constant.TicketRocketMQConstant;
import com.guoxu.ticketservice.common.enums.CanalExecuteStrategyMarkEnum;
import com.guoxu.ticketservice.mq.event.CanalBinlogEvent;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * CanalCommonSyncBinlogConsumer
 *
 * @author 执笔画棠
 * @date 2025/11/12 22:07
 **/
@Slf4j
@Component
@RequiredArgsConstructor
//这个注解是用来监听指定的topic和consumerGroup的消息
@RocketMQMessageListener(topic = TicketRocketMQConstant.CANAL_COMMON_SYNC_TOPIC_KEY, consumerGroup = TicketRocketMQConstant.CANAL_COMMON_SYNC_CG_KEY )
// CanalCommonSyncBinlogConsumer类实现了RocketMQListener接口，用于消费Canal生成的Binlog事件
// 并根据事件更新车票可用性缓存
public class CanalCommonSyncBinlogConsumer implements RocketMQListener<CanalBinlogEvent> {

    // 注入AbstractStrategyChoose，用于根据条件选择并执行相应的策略
    private final AbstractStrategyChoose abstractStrategyChoose;

    // 从配置文件中获取ticket.availability.cache - update.type属性值，
    // 该属性用于确定是否通过Binlog更新车票可用性缓存，默认值为空字符串
    @Value("${ticket.availability.cache - update.type:}")
    private String ticketAvailabilityCacheUpdateType;

    // 幂等性注解，用于确保MQ消息消费的幂等性
    // uniqueKeyPrefix指定唯一键前缀为"index12306 - ticket:binlog_sync:"
    // key通过SpEL表达式生成，结合消息的ID和哈希码
    // type指定幂等性类型为SPEL表达式方式
    // scene指定幂等性场景为MQ
    // keyTimeout设置唯一键的过期时间为7200秒
    @Idempotent(uniqueKeyPrefix = "index12306 - ticket:binlog_sync:", key = "#message.getId()+'_'+#message.hashCode()", type = IdempotentTypeEnum.SPEL, scene = IdempotentSceneEnum.MQ, keyTimeout = 7200L)
    // 重写RocketMQListener接口的onMessage方法，处理接收到的CanalBinlogEvent消息
    @Override
    public void onMessage(CanalBinlogEvent message) {
        // 注释提到了余票Binlog更新延迟问题及查看解决方案的链接
        // 如果消息是DDL语句，或者旧数据为空，或者消息类型不是"UPDATE"，
        // 或者配置的缓存更新类型不是"binlog"，则直接返回，不进行后续处理
        if (message.getIsDdl()
                || CollUtil.isEmpty(message.getOld())
                || !Objects.equals("UPDATE", message.getType())
                || !StrUtil.equals(ticketAvailabilityCacheUpdateType, "binlog")) {
            return;
        }
        // 根据消息中的表名，选择并执行相应的策略来处理Binlog事件
        // CanalExecuteStrategyMarkEnum.isPatternMatch(message.getTable())用于判断表名是否匹配特定模式
        abstractStrategyChoose.chooseAndExecute(
                message.getTable(),
                message,
                CanalExecuteStrategyMarkEnum.isPatternMatch(message.getTable()));
    }
}

