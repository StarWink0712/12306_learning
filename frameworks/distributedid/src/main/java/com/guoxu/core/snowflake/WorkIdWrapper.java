package com.guoxu.core.snowflake;

import lombok.*;
import lombok.experimental.FieldNameConstants;

/**
 * WorkIdWrapper workid包装器
 *
 * @author 执笔画棠
 * @date 2025/11/06 17:22
 **/
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WorkIdWrapper {

    private Long workId;

    private Long DataCenterId;
}
