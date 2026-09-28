package com.guoxu.userservice.service;

import com.guoxu.userservice.dto.req.PassengerRemoveReqDTO;
import com.guoxu.userservice.dto.req.PassengerReqDTO;
import com.guoxu.userservice.dto.resp.PassengerActualRespDTO;
import com.guoxu.userservice.dto.resp.PassengerRespDTO;

import java.util.List;

/**
 * PassengerService
 * 乘车人接口层
 * @author 执笔画棠
 * @version 2025/11/18 16:34
 **/
public interface PassengerService {
    /**
     * 根据用户名查询乘车人列表
     *
     * @param username 用户名
     * @return 乘车人返回列表
     */
    List<PassengerRespDTO> listPassengerQueryByUsername(String username);

    /**
     * 根据乘车人 ID 集合查询乘车人列表
     *
     * @param username 用户名
     * @param ids      乘车人 ID 集合
     * @return 乘车人返回列表
     */
    List<PassengerActualRespDTO> listPassengerQueryByIds(String username, List<Long> ids);

    /**
     * 新增乘车人
     *
     * @param requestParam 乘车人信息
     */
    void savePassenger(PassengerReqDTO requestParam);

    /**
     * 修改乘车人
     *
     * @param requestParam 乘车人信息
     */
    void updatePassenger(PassengerReqDTO requestParam);

    /**
     * 移除乘车人
     *
     * @param requestParam 移除乘车人信息
     */
    void removePassenger(PassengerRemoveReqDTO requestParam);
}