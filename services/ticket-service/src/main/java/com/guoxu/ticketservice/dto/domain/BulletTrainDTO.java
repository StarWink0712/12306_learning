package com.guoxu.ticketservice.dto.domain;

import lombok.Data;

/**
 * BulletTrainDTO
 * 动车实体
 * @author 执笔画棠
 * @date 2025/11/12 21:09
 **/
@Data
public class BulletTrainDTO {

    /**
     * 动卧数量
     */
    private Integer sleeperQuantity;

    /**
     * 动卧候选标识
     */
    private Boolean sleeperCandidate;

    /**
     * 动卧价格
     */
    private Integer sleeperPrice;

    /**
     * 一等卧数量
     */
    private Integer firstSleeperQuantity;

    /**
     * 一等卧候选标识
     */
    private Boolean firstSleeperCandidate;

    /**
     * 一等卧价格
     */
    private Integer firstSleeperPrice;

    /**
     * 二等卧数量
     */
    private Integer secondSleeperQuantity;

    /**
     * 二等卧候选标识
     */
    private Boolean secondSleeperCandidate;

    /**
     * 二等卧价格
     */
    private Integer secondSleeperPrice;

    /**
     * 二等座数量
     */
    private Integer secondClassQuantity;

    /**
     * 二等座候选标识
     */
    private Boolean secondClassCandidate;

    /**
     * 二等座价格
     */
    private Integer secondClassPrice;

    /**
     * 无座数量
     */
    private Integer noSeatQuantity;

    /**
     * 无座候选标识
     */
    private Boolean noSeatCandidate;

    /**
     * 无座价格
     */
    private Integer noSeatPrice;
}
