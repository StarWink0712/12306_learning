package com.guoxu.ticketservice.remote;

import com.guoxu.result.Result;
import com.guoxu.ticketservice.remote.dto.PassengerRespDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * UserRemoteService
 * 用户远程服务调用
 * @author 执笔画棠
 * @version 2025/11/12 22:26
 **/
@FeignClient(value = "index12306-user${unique-name:}-service", url = "${aggregation.remote-url:}")
public interface UserRemoteService {
    /**
     * 根据乘车人 ID 集合查询乘车人列表
     */
    @GetMapping("/api/user-service/inner/passenger/actual/query/ids")
    Result<List<PassengerRespDTO>> listPassengerQueryByIds(@RequestParam("username") String username,
                                                           @RequestParam("ids") List<String> ids);
}