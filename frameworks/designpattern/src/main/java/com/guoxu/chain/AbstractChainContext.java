package com.guoxu.chain;

import com.guoxu.ApplicationContextHolder;
import org.springframework.boot.CommandLineRunner;
import org.springframework.util.CollectionUtils;

import java.io.PushbackInputStream;
import java.util.*;
import java.util.stream.Collectors;

/**
 * AbstractChainContext
 * 抽象责任链上下文
 * @author 执笔画棠
 * @date 2025/11/06 18:50
 **/
public final class AbstractChainContext<T> implements CommandLineRunner {
    //责任链组件容器,用key存储不同标识的责任链组件，key是唯一标识，即mark
    //value是责任链组件列表，因为一个责任链标识可能对应多个责任链组件，每个责任链组件都实现了AbstractChainHandler接口
    private final Map<String, List<AbstractChainHandler>>  abstractChainHandlerContainer=new HashMap<>();

    /*
     * 责任链组件执行
     * 责任链的入口方法，处理实际的业务逻辑
     * 外部调用hanlder方法，并传入一个mark
     */
    public void handler(String mark,T requestParam){
        //从责任链容器中获取指定标识的责任链组件列表
        List<AbstractChainHandler> abstractChainHandlers=abstractChainHandlerContainer.get(mark);

        //如果没有这个责任链列表就抛出异常
        if(CollectionUtils.isEmpty(abstractChainHandlers)){
            throw new RuntimeException("责任链组件"+mark+"不存在");
        }

        //如果找到了，就使用foreach循环，按照定义好的顺序，依次调用列表中每个处理器的handler方法
        abstractChainHandlers.forEach(each ->each.handler(requestParam));
    }

    //实现commandlinerunner接口的run方法，在springboot启动后，初始化责任链组件
    @Override
    public void run(String... args){
        //从springboot容器中获取所有实现了abstractchainhandler接口的bean,从应用上下文持有者中获取自己封装的应用上下文
        Map<String,AbstractChainHandler> chainFilterMap= ApplicationContextHolder.getBeansOfType(AbstractChainHandler.class);

        //对获取到的责任链进行排序
        chainFilterMap.forEach((beanName,bean) ->{
            //从责任链容器中获取指定标识的责任链组件列表
            List<AbstractChainHandler> abstractChainHandlers=abstractChainHandlerContainer.get(bean.mark());

            //如果责任链容器中没有这个标识的责任链列表，就创建一个新的
            if(CollectionUtils.isEmpty(abstractChainHandlers)){
                abstractChainHandlers=new ArrayList<>();
            }
            //把当前的责任链列表组件添加到责任链组件列表中
            abstractChainHandlers.add(bean);
            //对责任链组件列表进行排序，按照ordered接口的order属性进行排序
            //这样在执行责任链组件时，就会按照order属性的顺序执行
            List<AbstractChainHandler> actualAbstractChainHandlers=abstractChainHandlers.stream()
                    .sorted(Comparator.comparing(AbstractChainHandler::getOrder))
                    .collect(Collectors.toList());
            //把排序后的责任链组件列表放到责任链组件容器中
            abstractChainHandlerContainer.put(bean.mark(),actualAbstractChainHandlers);

        });
    }
}
