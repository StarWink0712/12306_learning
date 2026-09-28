package com.guoxu.core.snowflake;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * SnowflakeIdInfo 雪花算法组成
 *
 * @author 执笔画棠
 * @date 2025/11/06 16:56
 **/
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SnowflakeIdInfo {

    /**
     * 时间戳
     */
    private Long timestamp;

    /**
     * 工作机器节点 ID
     */
    private Integer workerId;

    /**
     * 数据中心 ID
     */
    private Integer dataCenterId;

    /**
     * 自增序号，当高频模式下时，同一毫秒内生成 N 个 ID，则这个序号在同一毫秒下，自增以避免 ID 重复
     */
    private Integer sequence;

    /**
     * 通过基因法生成的序号，会和 {@link SnowflakeIdInfo#sequence} 共占 12 bit
     */
    private Integer gene;


}
