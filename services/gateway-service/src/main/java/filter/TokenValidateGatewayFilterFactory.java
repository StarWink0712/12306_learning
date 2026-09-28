package filter;

import com.alibaba.nacos.common.utils.CollectionUtils;
import com.guoxu.constant.UserConstant;
import config.Config;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import toolkit.JWTUtil;
import toolkit.UserInfoDTO;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;

/**
 * TokenValidateGatewayFilterFactory
 * 网关过滤器工厂，用于验证token令牌
 * @author 执笔画棠
 * @date 2025/11/08 20:48
 **/
@Component
public class TokenValidateGatewayFilterFactory extends AbstractGatewayFilterFactory<Config> {

    //构造方法
    public TokenValidateGatewayFilterFactory() {
        //指定配置类为Config.class
        //调用父类的构造器，传递配置类
        super(Config.class);
    }

    //注销用户接口路径常量
    //该接口路径在注销时，需要传入原始token令牌，用于后续业务，如销毁token
    public static final String DELETION_PATH="/api/user-service/deletion";

    /*
     * 应用网关过滤器,创建并返回实际的网关过滤器
     * 核心逻辑是拦截指定前缀的请求，验证token有效性，处理用户信息传递
     *
     * @param config 配置类，包含黑名单前缀路径
     * @return 网关过滤器
     */
    @Override
    public GatewayFilter apply(Config config) {
        //返回gatewayfilter函数式接口实例，包含exchange(请求响应上下文)和chain(过滤器链)参数
        return (exchange,chain) ->{
            //从上下文对象中获取当前请求对象
            ServerHttpRequest request=exchange.getRequest();
            //获取完整的请求路径
            String requestPath=request.getPath().toString();

            //判断当前请求路径是否在配置的黑名单路径前缀列表中（需要token验证的路径）
            if(isPathInBlackPreList(requestPath,config.getBlackPathPre())){
                //从请求头中获取authoeization字段值，（这个是前端传过来的字段，可以自定义，包含token和token前缀等信息）
                String token=request.getHeaders().getFirst("Authorization");
                //调用jwtutil工具类解析token
                UserInfoDTO userInfo= JWTUtil.parseJwtToken(token);
                if(!validateToken(userInfo)){
                    //token无效，返回401未授权响应
                    //从上下文对象中获取响应对象
                    ServerHttpResponse response=exchange.getResponse();
                    //设置响应码为401，未授权
                    response.setStatusCode(HttpStatus.UNAUTHORIZED);
                    //完成响应，不再指向后续过滤器
                    return response.setComplete();
                }

                //构建新的请求对象构建器，向请求头中添加用户信息(供下游服务获取)
                ServerHttpRequest.Builder builder=exchange.getRequest().mutate().headers(httpHeaders -> {
                    //向请求头中添加用户id
                    httpHeaders.set(UserConstant.USER_ID_KEY, userInfo.getUserId());
                    // 向请求头中添加用户名（键为UserConstant.USER_NAME_KEY常量）
                    httpHeaders.set(UserConstant.USER_NAME_KEY, userInfo.getUsername());
                    // 向请求头中添加真实姓名，使用UTF-8编码（避免中文乱码）
                    httpHeaders.set(UserConstant.REAL_NAME_KEY,
                            URLEncoder.encode(userInfo.getRealName(), StandardCharsets.UTF_8));

                    //如果是注销路径，额外向请求头中添加用户token（用于注销业务验证token）
                    if(Objects.equals(requestPath,DELETION_PATH)){
                        httpHeaders.set(UserConstant.USER_TOKEN_KEY,token);
                    }
                });

                //构建新的请求对象，继续执行后续过滤链（将携带用户信息的请求传递给下游服务）
                return chain.filter(exchange.mutate().request(builder.build()).build());
            }
            //若请求路径不在黑名单路径前缀列表中，直接放行
            return chain.filter(exchange);
        };
    }


    /**
     * 判断请求路径是否在黑名单路径前缀列表中
     * 逻辑：检查请求路径是否以列表中的任意一个前缀开头
     *
     * @param requestPath  当前请求的路径字符串
     * @param blackPathPre 配置的黑名单路径前缀列表（需要Token验证的路径前缀）
     * @return true-请求路径在黑名单前缀列表中（需要验证Token）；false-不在（无需验证）
     */
    private boolean isPathInBlackPreList(String requestPath, List<String> blackPathPre){
        //如果黑名单验证逻辑为空，直接返回false，即不需要验证
        if(CollectionUtils.isEmpty(blackPathPre)){
            return false;
        }

        //流式遍历前缀列表，判断请求路径是否以任意前缀开头
        return blackPathPre.stream().anyMatch(requestPath::startsWith);
    }

    //token有效性验证
    private boolean validateToken(UserInfoDTO userInfoDTO){
        return userInfoDTO!=null;
    }
}
