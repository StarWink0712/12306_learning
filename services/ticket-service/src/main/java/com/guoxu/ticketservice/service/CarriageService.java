package com.guoxu.ticketservice.service;

import java.util.List;

/**
 * CarriageService 列车车厢接口层
 *
 * @author 执笔画棠
 * @version 2025/11/13 20:27
 **/
public interface CarriageService {
    /**
     * 查询列车车厢号集合
     *
     * @param trainId      列车 ID
     * @param carriageType 车厢类型
     * @return 车厢号集合
     */
    List<String> listCarriageNumber(String trainId, Integer carriageType);
}