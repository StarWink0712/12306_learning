package com.guoxu.base;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;

import java.util.Date;

/**
 * BaseDo
 * 数据持久层基础属性
 * 通过让项目里其他的 DO 类（如 UserDO, OrderDO 等）继承 BaseDo，可以达到以下目的：
 * 代码复用：避免在每一个 DO 类中重复定义 createTime, updateTime, delFlag 这三个几乎所有表都会用到的字段。
 * 统一规范：确保项目中所有表都遵循统一的创建时间、更新时间和逻辑删除规范。
 * 自动化处理：利用 MyBatis-Plus 的自动填充功能，实现这些字段值的自动设置，无需在业务代码中手动干预。
 * @author 执笔画棠
 * @date 2025/11/06 12:48
 **/
@Data
public class BaseDo {

    @TableField(fill = FieldFill.INSERT)
    private Date createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;

    @TableField(fill = FieldFill.INSERT)
    private Integer delFlag;
}
