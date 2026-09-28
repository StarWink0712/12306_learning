package com.guoxu.ticketservice.job.base;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.guoxu.ApplicationContextHolder;
import com.guoxu.ticketservice.dao.entity.TrainDO;
import com.guoxu.ticketservice.dao.mapper.TrainMapper;
import com.guoxu.toolkit.EnvironmentUtil;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.IJobHandler;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;
import java.util.Optional;

/**
 * AbstractTrainStationJobHandlerTemplate
    抽象列车&车票相关定时任务
 * 已通过运行时判断缓存不存在实时读取数据库获取完成，该定时任务不在主流程中
 * @author 执笔画棠
 * @date 2025/11/12 21:40
 **/
@Deprecated
// 定义一个抽象类AbstractTrainStationJobHandlerTemplate，它继承自IJobHandler，用于处理与火车站相关的定时任务
public abstract class AbstractTrainStationJobHandlerTemplate extends IJobHandler {

    /**
     * 模板方法模式中具体实现子类执行定时任务的抽象方法
     *
     * @param trainDOPageRecords 列车信息分页记录列表，由子类具体实现该方法来处理这些记录
     */
    protected abstract void actualExecute(List<TrainDO> trainDOPageRecords);

    // 重写IJobHandler接口中的execute方法，定义定时任务的执行逻辑
    @Override
    public void execute() {
        // 定义当前页码，初始值为1
        var currentPage = 1L;
        // 定义每页的记录数，这里设置为1000
        var size = 1000L;
        // 获取定时任务的请求参数
        var requestParam = getJobRequestParam();
        // 如果请求参数不为空，则将其解析为日期；否则获取明天的日期
        var dateTime = StrUtil.isNotBlank(requestParam)? DateUtil.parse(requestParam, "yyyy - MM - dd")
                : DateUtil.tomorrow();
        // 从Spring应用上下文中获取TrainMapper实例，用于数据库查询操作
        var trainMapper = ApplicationContextHolder.getBean(TrainMapper.class);
        // 无限循环，每次循环处理一页数据
        for (;; currentPage++) {
            // 构建查询条件，查询指定日期范围内的列车信息
            var queryWrapper = Wrappers.lambdaQuery(TrainDO.class)
                    .between(TrainDO::getDepartureTime, DateUtil.beginOfDay(dateTime), DateUtil.endOfDay(dateTime));
            // 使用TrainMapper进行分页查询，获取当前页的列车信息
            var trainDOPage = trainMapper.selectPage(new Page<>(currentPage, size), queryWrapper);
            // 如果查询结果为空或者当前页没有记录，则跳出循环
            if (trainDOPage == null || CollUtil.isEmpty(trainDOPage.getRecords())) {
                break;
            }
            // 获取当前页的列车信息记录列表
            var trainDOPageRecords = trainDOPage.getRecords();
            // 调用抽象方法actualExecute，由子类具体实现对这些记录的处理逻辑
            actualExecute(trainDOPageRecords);
        }
    }

    // 获取定时任务请求参数的私有方法
    private String getJobRequestParam() {
        // 如果是开发环境
        return EnvironmentUtil.isDevEnvironment()
                // 尝试从请求头中获取名为"requestParam"的参数
                ? Optional.ofNullable(((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()))
                .map(ServletRequestAttributes::getRequest).map(each -> each.getHeader("requestParam"))
                .orElse(null)
                // 否则，从XXL - Job框架中获取任务参数
                : XxlJobHelper.getJobParam();
    }
}
