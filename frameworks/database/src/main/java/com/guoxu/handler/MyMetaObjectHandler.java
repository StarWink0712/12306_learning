package com.guoxu.handler;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.guoxu.enmus.DelEnum;
import org.apache.ibatis.reflection.MetaObject;

import java.util.Date;

/**
 * MyMetaObjectHandler
 * 元数据处理器
 * MyMetaObjectHandler 类通过实现 MyBatis-Plus 提供的 MetaObjectHandler 接口，
 * 充当一个拦截器或处理器。每当 MyBatis-Plus 执行 INSERT 或 UPDATE SQL 操作时，
 * 它都会自动调用这个类中对应的方法，让开发者有机会在持久化到数据库之前，对实体对象（Entity/DO）进行最后的修改。
 * @author 执笔画棠
 * @date 2025/11/06 13:01
 **/
public class MyMetaObjectHandler implements MetaObjectHandler {

    /**
     * 数据新增时填充
     *
     * @param metaObject
     */
    @Override
    public void insertFill(MetaObject metaObject) {
        this.strictInsertFill(metaObject, "createTime", Date.class, new Date());
        this.strictInsertFill(metaObject, "updateTime", Date.class, new Date());
        this.strictInsertFill(metaObject, "delFlag", Integer.class, DelEnum.NORMAL.code());
    }

    /**
     * 数据修改时填充
     *
     * @param metaObject
     */
    @Override
    public void updateFill(MetaObject metaObject) {
        this.strictInsertFill(metaObject, "updateTime", Date.class, new Date());
    }
}
