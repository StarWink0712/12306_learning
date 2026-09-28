package config;

import core.UserTransmitFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;

import java.awt.image.RGBImageFilter;

import static com.guoxu.constant.FilterOrderConstant.USER_TRANSMIT_FILTER_ORDER;

/**
 * UserAutoConfiguration
 *用户配置自动装配
 *  * 用户配置自动装配类用于在应用程序中自动配置用户相关的组件。
 *  * 它主要负责注册用户信息传递过滤器，确保在请求处理过程中能够正确传递用户上下文。
 * @author 执笔画棠
 * @date 2025/11/04 21:44
 **/
public class UserAutoConfiguration {

    @Bean
    public FilterRegistrationBean<UserTransmitFilter> globalUserTransmitFilter(){
        //注册用户信息传递过滤器
        FilterRegistrationBean<UserTransmitFilter> registration=new FilterRegistrationBean<>();
        //设置过滤器名称
        registration.setFilter(new UserTransmitFilter());
        //拦截所有路径请求
        registration.addUrlPatterns("/*");
        registration.setOrder(USER_TRANSMIT_FILTER_ORDER);
        return registration;
    }
}
