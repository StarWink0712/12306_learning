package com.guoxu.core;

import com.guoxu.annotation.Idempotent;
import com.guoxu.core.param.IdempotentParamExecuteHandler;
import com.guoxu.core.param.IdempotentParamService;
import com.guoxu.core.spel.IdempotentSpELByMQExecuteHandler;
import com.guoxu.core.spel.IdempotentSpELByRestAPIExecuteHandler;
import com.guoxu.core.token.IdempotentTokenService;
import com.guoxu.enums.IdempotentSceneEnum;
import com.guoxu.enums.IdempotentTypeEnum;
import com.guoxu.ApplicationContextHolder;
/**
 * IdempotentExecuteHandlerFactory
 *
 * @author 执笔画棠
 * @date 2025/11/06 20:17
 **/
public final class IdempotentExecuteHandlerFactory {

    /**
     * 获取幂等执行处理器实例的方法
     * 核心逻辑：根据传入的场景枚举和类型枚举，匹配并返回对应的处理器实现类
     *
     * @param scene 指定幂等验证的场景类型（如RESTAPI接口、消息队列MQ等，由IdempotentSceneEnum枚举定义）
     * @param type  指定幂等的处理类型（如基于参数、Token、SPEL表达式等，由IdempotentTypeEnum枚举定义）
     * @return 匹配到的幂等执行处理器实例（IdempotentExecuteHandler接口的实现类），未匹配到则返回null
     */
    public static IdempotentExecuteHandler getInstance(IdempotentSceneEnum scene, IdempotentTypeEnum type) {
        // 声明结果变量，用于存储最终匹配到的幂等处理器实例，初始化为null
        IdempotentExecuteHandler result = null;
        // 根据场景枚举（scene）进行分支判断，不同场景对应不同的处理器选择逻辑
        switch (scene) {
            // 分支1：处理RESTAPI场景（即HTTP接口类的幂等验证）
            case RESTAPI -> {
                // 在RESTAPI场景下，再根据处理类型（type）进一步选择具体处理器
                switch (type) {
                    // 子分支1：当处理类型为PARAM（基于请求参数的幂等）
                    case PARAM ->
                        // 从Spring容器中获取IdempotentParamService类型的Bean作为处理器
                        // ApplicationContextHolder是Spring上下文工具类，用于获取容器中的Bean
                            result = ApplicationContextHolder.getBean(IdempotentParamService.class);
                    // 子分支2：当处理类型为TOKEN（基于Token的幂等）
                    case TOKEN ->
                        // 从Spring容器中获取IdempotentTokenService类型的Bean作为处理器
                            result = ApplicationContextHolder.getBean(IdempotentTokenService.class);
                    // 子分支3：当处理类型为SPEL（基于SPEL表达式的幂等，适用于RESTAPI场景）
                    case SPEL ->
                        // 从Spring容器中获取IdempotentSpELByRestAPIExecuteHandler类型的Bean作为处理器
                            result = ApplicationContextHolder.getBean(IdempotentSpELByRestAPIExecuteHandler.class);
                    // 其他未定义的处理类型：不做任何操作（保持result为null）
                    default -> {
                    }
                }
            }
            // 分支2：处理MQ场景（即消息队列消费的幂等验证）
            case MQ ->
                // MQ场景下通常使用SPEL表达式生成幂等键，因此直接获取对应的处理器Bean
                    result = ApplicationContextHolder.getBean(IdempotentSpELByMQExecuteHandler.class);
            // 其他未定义的场景：不做任何操作（保持result为null）
            default -> {
            }
        }
        // 返回匹配到的处理器实例（可能为null，需调用方处理未匹配的情况）
        return result;
    }
}
