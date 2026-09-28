package com.guoxu.orderservice.remote;

import com.guoxu.orderservice.remote.dto.UserQueryActualRespDTO;
import com.guoxu.result.Result;
import jakarta.validation.constraints.NotEmpty;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * UserRemoteService
 * 用户远程服务调用
 * @author 执笔画棠
 * @version 2025/11/09 19:42
 **/
//这个FeignClient注解是用来定义一个Feign客户端的，value属性指定了客户端的名称，url属性指定了服务的URL。
//feign客户端是用来调用其他服务的，这里指定了调用index12306-user-service服务。
 @Service
@FeignClient(value = "index12306-user${unique-name:}-service", url = "${aggregation.remote-url:}")
public interface UserRemoteService {

    /**
     * 根据乘车人 ID 集合查询乘车人列表
     * 即根据用户名查询当前登录用户的实际信息
     */
    @GetMapping("/api/user-service/actual/query")
    Result<UserQueryActualRespDTO> queryActualUserByUsername(@RequestParam("username") @NotEmpty String username);

}