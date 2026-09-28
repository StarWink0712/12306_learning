package com.guoxu.userservice.controller;

import com.guoxu.Results;
import com.guoxu.annotation.Idempotent;
import com.guoxu.enums.IdempotentTypeEnum;
import com.guoxu.result.Result;
import com.guoxu.userservice.dto.req.PassengerRemoveReqDTO;
import com.guoxu.userservice.dto.req.PassengerReqDTO;
import com.guoxu.userservice.dto.resp.PassengerActualRespDTO;
import com.guoxu.userservice.dto.resp.PassengerRespDTO;
import com.guoxu.userservice.service.PassengerService;
import core.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * PassengerController
 * 乘车人控制层
 * @author 执笔画棠
 * @date 2025/11/18 16:05
 **/
@RestController
@RequiredArgsConstructor
public class PassengerController {
    private final PassengerService passengerService;

    /**
     * 根据用户名查询乘车人列表
     */
    @GetMapping("/api/user-service/passenger/query")
    public Result<List<PassengerRespDTO>> listPassengerQueryByUsername() {
        return Results.success(passengerService.listPassengerQueryByUsername(UserContext.getUsername()));
    }

    /**
     * 根据乘车人 ID 集合查询乘车人列表
     */
    @GetMapping("/api/user-service/inner/passenger/actual/query/ids")
    public Result<List<PassengerActualRespDTO>> listPassengerQueryByIds(@RequestParam("username") String username,
                                                                        @RequestParam("ids") List<Long> ids) {
        return Results.success(passengerService.listPassengerQueryByIds(username, ids));
    }

    /**
     * 新增乘车人
     */
    @Idempotent(uniqueKeyPrefix = "index12306-user:lock_passenger-alter:", key = "T(org.opengoofy.index12306.frameworks.starter.user.core.UserContext).getUsername()", type = IdempotentTypeEnum.SPEL, scene = IdempotentSceneEnum.RESTAPI, message = "正在新增乘车人，请稍后再试...")
    @PostMapping("/api/user-service/passenger/save")
    public Result<Void> savePassenger(@RequestBody PassengerReqDTO requestParam) {
        passengerService.savePassenger(requestParam);
        return Results.success();
    }

    /**
     * 修改乘车人
     */
    @Idempotent(uniqueKeyPrefix = "index12306-user:lock_passenger-alter:", key = "T(org.opengoofy.index12306.frameworks.starter.user.core.UserContext).getUsername()", type = IdempotentTypeEnum.SPEL, scene = IdempotentSceneEnum.RESTAPI, message = "正在修改乘车人，请稍后再试...")
    @PostMapping("/api/user-service/passenger/update")
    public Result<Void> updatePassenger(@RequestBody PassengerReqDTO requestParam) {
        passengerService.updatePassenger(requestParam);
        return Results.success();
    }

    /**
     * 移除乘车人
     */
    @PostMapping("/api/user-service/passenger/remove")
    public Result<Void> removePassenger(@RequestBody PassengerRemoveReqDTO requestParam) {
        passengerService.removePassenger(requestParam);
        return Results.success();
    }
}
