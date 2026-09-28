package com.guoxu.userservice.dao.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guoxu.base.BaseDo;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * UserDeletionDO
 *
 * @author 执笔画棠
 * @date 2025/11/18 16:12
 **/
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@TableName("t_user_deletion")
public class UserDeletionDO extends BaseDo {
    /**
     * id
     */
    private Long id;

    /**
     * 证件类型
     */
    private Integer idType;

    /**
     * 证件号
     */
    private String idCard;
}
