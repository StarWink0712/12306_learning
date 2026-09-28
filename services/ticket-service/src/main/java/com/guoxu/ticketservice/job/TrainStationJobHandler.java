package com.guoxu.ticketservice.job;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.guoxu.DistributedCache;
import com.guoxu.ticketservice.common.constant.Index12306Constant;
import com.guoxu.ticketservice.dao.entity.TrainDO;
import com.guoxu.ticketservice.dao.entity.TrainStationDO;
import com.guoxu.ticketservice.dao.mapper.TrainStationMapper;
import com.guoxu.ticketservice.job.base.AbstractTrainStationJobHandlerTemplate;
import com.xxl.job.core.handler.annotation.XxlJob;
import jdk.jfr.Registered;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.guoxu.ticketservice.common.constant.RedisKeyConstant.TRAIN_STATION_STOPOVER_DETAIL;

/**
 * TrainStationJobHandler
 *
 * @author 执笔画棠
 * @date 2025/11/12 21:48
 **/
@Deprecated
@RequiredArgsConstructor
@RestController
// 列车站点任务处理器类，继承自抽象的列车站点任务处理器模板
public class TrainStationJobHandler extends AbstractTrainStationJobHandlerTemplate {

    // 列车站点数据访问对象，用于数据库操作
    private final TrainStationMapper trainStationMapper;
    // 分布式缓存实例，用于操作Redis等缓存系统
    private final DistributedCache distributedCache;

    // XXL-Job任务注解，标记该方法为一个XXL-Job定时任务，任务名称为"trainStationJobHandler"
    @XxlJob(value = "trainStationJobHandler")
    // Spring MVC的GetMapping注解，提供HTTP GET接口，用于手动触发任务执行
    @GetMapping("/api/ticket-service/train-station/job/cache-init/execute")
    // 重写execute方法，作为任务执行的入口点
    @Override
    public void execute() {
        // 调用父类的execute方法，执行模板中定义的通用逻辑
        super.execute();
    }

    // 重写实际执行方法，包含具体的业务逻辑实现
    @Override
    protected void actualExecute(List<TrainDO> trainDOPageRecords) {
        // 遍历传入的列车数据对象列表
        for (TrainDO each : trainDOPageRecords) {
            // 创建Lambda查询条件包装器，用于构建查询条件
            LambdaQueryWrapper<TrainStationDO> queryWrapper = Wrappers.lambdaQuery(TrainStationDO.class)
                    // 设置查询条件：列车站点表的train_id字段等于当前列车对象的id
                    .eq(TrainStationDO::getTrainId, each.getId());
            // 执行数据库查询，获取该列车对应的所有站点信息列表
            List<TrainStationDO> trainStationDOList = trainStationMapper.selectList(queryWrapper);
            // 将查询结果存入分布式缓存
            distributedCache.put(
                    // 构建缓存键：使用常量前缀加上列车ID
                    TRAIN_STATION_STOPOVER_DETAIL + each.getId(),
                    // 将站点列表转换为JSON字符串格式存储
                    JSON.toJSONString(trainStationDOList),
                    // 设置缓存过期时间：使用预定义的提前售票天数常量
                    Index12306Constant.ADVANCE_TICKET_DAY,
                    // 设置时间单位：天
                    TimeUnit.DAYS);
        }
    }
}
