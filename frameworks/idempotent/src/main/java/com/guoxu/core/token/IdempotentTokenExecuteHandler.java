package com.guoxu.core.token;

import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.StrUtil;
import com.google.common.base.Strings;
import com.guoxu.DistributedCache;
import com.guoxu.config.IdempotentProperties;
import com.guoxu.core.AbstractIdempotentExecuteHandler;
import com.guoxu.core.IdempotentParamWrapper;
import com.guoxu.errorcode.BaseErrorCode;
import com.guoxu.exception.ClientException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Optional;


/**
 * IdempotentTokenExecuteHandler
 *
 * @author 执笔画棠
 * @date 2025/11/06 20:17
 **/
@RequiredArgsConstructor
public class IdempotentTokenExecuteHandler extends AbstractIdempotentExecuteHandler implements IdempotentTokenService {

    //分布式缓存组件，存储token,实现分布式环境下的token共享与校验
    private final DistributedCache distributedCache;

    //幂等属性配置类,存储token前缀，过期时间等配置信息
    private final IdempotentProperties idempotentProperties;

    //token在请求头参数头中的键名
    private static final String TOKEN_KEY="token";

    //token在分布式缓存中的默认键前缀
    private static final String TOKEN_PREFIX_KEY="idempotent_token:";

    //token的默认过期时间
    private static final long TOKEN_EXPIRED_TIME=6000;


    /**
     * 重写父类方法，构建幂等参数包装器
     * 幂等参数包装器用于封装幂等处理过程中需要的参数（如注解信息、业务数据等）
     *
     * @param joinPoint AOP连接点，包含目标方法信息
     * @return 构建好的幂等参数包装器实例
     */
    @Override
    protected IdempotentParamWrapper buildWrapper(ProceedingJoinPoint joinPoint) {
        // 创建并返回一个新的幂等参数包装器（此处为基础实现，可根据需要扩展）
        return new IdempotentParamWrapper();
    }

    /**
     * 实现IdempotentTokenService接口的方法，生成并返回一个新的幂等Token
     * 流程：生成Token字符串 → 存入分布式缓存 → 返回Token
     *
     * @return 生成的幂等Token
     */
    @Override
    public String createToken() {
        // 生成Token：前缀（优先使用配置中的前缀，若为空则用默认前缀）+ UUID随机字符串
        String token = Optional.ofNullable(Strings.emptyToNull(idempotentProperties.getPrefix()))
                .orElse(TOKEN_PREFIX_KEY) + UUID.randomUUID();
        // 将Token存入分布式缓存，值为空（仅需存在性校验），过期时间取配置值或默认6000毫秒
        distributedCache.put(token, "",
                Optional.ofNullable(idempotentProperties.getTimeout()).orElse(TOKEN_EXPIRED_TIME));
        // 返回生成的Token（前端需保存此Token，后续请求时携带）
        return token;
    }

    /**
     * 重写父类方法，实现基于Token的幂等核心处理逻辑
     * 核心逻辑：校验请求中的Token → 验证Token有效性（是否存在且未被使用）→ 处理重复请求
     *
     * @param wrapper 幂等参数包装器，包含幂等处理所需的上下文信息
     */
    @Override
    public void handler(IdempotentParamWrapper wrapper) {
        // 获取当前HTTP请求对象（通过Spring的RequestContextHolder获取当前线程绑定的请求）
        HttpServletRequest request = ((ServletRequestAttributes) (RequestContextHolder.currentRequestAttributes()))
                .getRequest();
        // 从请求头中获取Token（优先从Header获取）
        String token = request.getHeader(TOKEN_KEY);
        // 若请求头中无Token，则从请求参数中获取Token
        if (StrUtil.isBlank(token)) {
            token = request.getParameter(TOKEN_KEY);
            // 若参数中也无Token，抛出客户端异常（Token为空错误）
            if (StrUtil.isBlank(token)) {
                throw new ClientException(BaseErrorCode.IDEMPOTENT_TOKEN_NULL_ERROR);
            }
        }
        // 尝试从分布式缓存中删除Token（一次性Token：第一次请求删除成功，后续请求删除失败）
        Boolean tokenDelFlag = distributedCache.delete(token);
        // 若删除失败（说明Token不存在或已被删除，即重复请求）
        if (!tokenDelFlag) {
            // 确定错误信息：优先使用注解中配置的消息，否则使用默认错误消息
            String errMsg = StrUtil.isNotBlank(wrapper.getIdempotent().message())
                    ? wrapper.getIdempotent().message()
                    : BaseErrorCode.IDEMPOTENT_TOKEN_DELETE_ERROR.message();
            // 抛出客户端异常（Token删除失败，即重复请求）
            throw new ClientException(errMsg, BaseErrorCode.IDEMPOTENT_TOKEN_DELETE_ERROR);
        }
    }
}
