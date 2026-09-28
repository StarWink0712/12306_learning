package com.guoxu.strategy;

import com.guoxu.ApplicationContextHolder;
import com.guoxu.init.ApplicationInitializingEvent;
import org.opengoofy.index12306.framework.starter.convention.exception.ServiceException;
import org.springframework.context.ApplicationListener;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * AbstractStrategyChoose
 *
 * @author 执笔画棠
 * @date 2025/11/06 19:13
 **/
//这里就对上了，ApplicationInitializingEvent是在应用初始化完成后发布的事件
//在bizs包下自定义的事件，这里继承这个事件，接收到事件通知后就会进行初始化
public class AbstractStrategyChoose implements ApplicationListener<ApplicationInitializingEvent> {

    //执行策略集合
    private final Map<String,AbstractExecuteStrategy> abstractExecuteStrategyMap=new HashMap<>();

    //根据mark查询具体策略
    public AbstractExecuteStrategy choose(String mark,Boolean predicateFlag){
        //如果匹配标识为true，则根据正则表达式匹配策略标识
        if(predicateFlag!=null && predicateFlag){
            return abstractExecuteStrategyMap.values().stream()
                    .filter(each -> StringUtils.hasText(each.patternMatchMark()))
                    .filter(each -> Pattern.compile(each.patternMatchMark()).matcher(mark).matches())
                    .findFirst()
                    .orElseThrow(() -> new ServiceException("未匹配到执行策略"));
        }

        //返回匹配到的执行策略
        return Optional.ofNullable(abstractExecuteStrategyMap.get(mark))
                .orElseThrow(() -> new ServiceException("未匹配到执行策略"));
    }



    /**
     * 根据 mark 查询具体策略并执行
     *
     * @param mark         策略标识
     * @param requestParam 执行策略入参
     * @param <REQUEST>    执行策略入参范型
     */
    public <REQUEST> void chooseAndExecute(String mark, REQUEST requestParam) {
        AbstractExecuteStrategy executeStrategy = choose(mark, null);
        executeStrategy.execute(requestParam);
    }

    /**
     * 根据 mark 查询具体策略并执行 无返回参数
     *
     * @param mark          策略标识
     * @param requestParam  执行策略入参
     * @param predicateFlag 匹配范解析标识
     * @param <REQUEST>     执行策略入参范型
     */
    public <REQUEST> void chooseAndExecute(String mark, REQUEST requestParam, Boolean predicateFlag) {
        AbstractExecuteStrategy executeStrategy = choose(mark, predicateFlag);
        executeStrategy.execute(requestParam);
    }

    /**
     * 根据 mark 查询具体策略并执行，带返回结果 有返回参数
     *
     * @param mark         策略标识
     * @param requestParam 执行策略入参
     * @param <REQUEST>    执行策略入参范型
     * @param <RESPONSE>   执行策略出参范型
     * @return
     */
    public <REQUEST, RESPONSE> RESPONSE chooseAndExecuteResp(String mark, REQUEST requestParam) {
        AbstractExecuteStrategy executeStrategy = choose(mark, null);
        // 执行策略并返回结果
        return (RESPONSE) executeStrategy.executeResp(requestParam);
    }

    @Override
    public void onApplicationEvent(ApplicationInitializingEvent event) {
        // 从 Spring 容器中获取所有实现了 AbstractExecuteStrategy 接口的 bean
        Map<String, AbstractExecuteStrategy> actual = ApplicationContextHolder
                .getBeansOfType(AbstractExecuteStrategy.class);
        // 检查是否有重复的策略标识
        actual.forEach((beanName, bean) -> {
            // 检查是否有重复的策略标识
            AbstractExecuteStrategy beanExist = abstractExecuteStrategyMap.get(bean.mark());

            if (beanExist != null) {
                throw new ServiceException(String.format("[%s] Duplicate execution policy", bean.mark()));
            }
            // 将策略标识和策略 bean 放入集合中
            abstractExecuteStrategyMap.put(bean.mark(), bean);
        });
    }
}
