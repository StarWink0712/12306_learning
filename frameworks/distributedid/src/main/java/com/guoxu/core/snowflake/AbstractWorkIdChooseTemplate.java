package com.guoxu.core.snowflake;

import com.guoxu.toolkit.SnowflakeIdUtil;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;

/**
 * AbstractWorkIdChooseTemplate 雪花算法生成器模板
 *
 * @author 执笔画棠
 * @date 2025/11/06 17:20
 **/
@Slf4j
public abstract class AbstractWorkIdChooseTemplate {
    /**
     * 是否使用 {@link SystemClock} 获取当前时间戳
     * 读取配置文件中的 framework.distributed.id.snowflake.is-use-system-clock 属性值,默认值为 false
     * 该属性用于配置是否使用 {@link SystemClock} 获取当前时间戳。
     * 如果设置为 true,则使用 {@link SystemClock} 获取当前时间戳,否则使用 {@link System#currentTimeMillis()} 获取当前时间戳。
     * System.currentTimeMillis()	标准系统时间，每次调用都进行系统调用	相对较慢
     * SystemClock.now()	Hutool优化的时钟，缓存时间戳，定时更新	性能更好
     */
    //@Value("${framework.distributed.id.snowflake.is-use-system-clock:false}")
    private boolean isUseSystemClock;

    /**
     * 根据自定义策略获取 WorkId 生成器
     *
     * @return
     */
    protected abstract WorkIdWrapper chooseWorkId();

    /**
     * 选择 WorkId 并初始化雪花
     * 获取 WorkId 和 DataCenterId。
     * 使用这些 ID 创建一个 Snowflake 实例。
     * 打印日志。
     * 将创建好的 Snowflake 实例注册到全局的 SnowflakeIdUtil 工具类中。
     */
    public void chooseAndInit() {
        // 模板方法模式: 通过抽象方法获取 WorkId 包装器创建雪花算法
        WorkIdWrapper workIdWrapper = chooseWorkId();

        // 获取 WorkId 生成器中的 WorkId
        long workId = workIdWrapper.getWorkId();

        // 获取 WorkId 生成器中的数据中心 ID
        long dataCenterId = workIdWrapper.getDataCenterId();

        // 初始化 Snowflake 算法
        Snowflake snowflake = new Snowflake(workId, dataCenterId, isUseSystemClock);

        log.info("Snowflake type: {}, workId: {}, dataCenterId: {}", this.getClass().getSimpleName(), workId,
                dataCenterId);
        // 将创建好的 Snowflake 实例注册到全局的 SnowflakeIdUtil 工具类中,这是整个流程的闭环
        // 从此以后，应用程序的任何地方都可以通过 SnowflakeIdUtil.nextId() 来使用这个已经初始化好的 ID 生成器了。
        SnowflakeIdUtil.initSnowflake(snowflake);
    }

}
