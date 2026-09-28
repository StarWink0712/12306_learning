package com.guoxu.toolkit;
import com.guoxu.ApplicationContextHolder;
import org.springframework.core.env.ConfigurableEnvironment;

import java.util.ArrayList;
import java.util.List;
/**
 * EnvironmentUtil
 *
 * @author 执笔画棠
 * @date 2025/11/04 18:19
 **/
public class EnvironmentUtil {

    private static List<String> ENVIRONMENT_LIST = new ArrayList<>();

    static {
        ENVIRONMENT_LIST.add("dev");
        ENVIRONMENT_LIST.add("test");
    }

    /**
     * 判断当前是否为开发环境
     *
     * @return
     */
    public static boolean isDevEnvironment() {
        //自定义应用上下文，在base模块里面
        ConfigurableEnvironment configurableEnvironment = ApplicationContextHolder
                .getBean(ConfigurableEnvironment.class);
        String propertyActive = configurableEnvironment.getProperty("spring.profiles.active", "dev");
        return ENVIRONMENT_LIST.stream().filter(each -> propertyActive.contains(each)).findFirst().isPresent();
    }

    /**
     * 判断当前是否为正式环境
     *
     * @return
     */
    public static boolean isProdEnvironment() {
        ConfigurableEnvironment configurableEnvironment = ApplicationContextHolder
                .getBean(ConfigurableEnvironment.class);
        String propertyActive = configurableEnvironment.getProperty("spring.profiles.active", "dev");
        return !ENVIRONMENT_LIST.stream().filter(each -> propertyActive.contains(each)).findFirst().isPresent();
    }
}
