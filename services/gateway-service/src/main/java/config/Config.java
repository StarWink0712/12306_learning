package config;

import lombok.Data;

import java.util.List;

/**
 * Config
 * 网关过滤器工厂配置类
 * @author 执笔画棠
 * @date 2025/11/08 20:49
 **/
@Data
public class Config {

    //黑名单前缀路径
    private List<String> blackPathPre;
}
