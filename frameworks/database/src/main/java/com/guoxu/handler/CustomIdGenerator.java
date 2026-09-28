package com.guoxu.handler;

import com.baomidou.mybatisplus.core.incrementer.IdentifierGenerator;
import com.guoxu.toolkit.SnowflakeIdUtil;

/**
 * CustomIdGenerator
 *
 * @author 执笔画棠
 * @date 2025/11/06 13:01
 **/
public class CustomIdGenerator implements IdentifierGenerator {
    @Override
    public Number nextId(Object entity) {
        //从distributedid模块导入自定义的雪花算法
        //AbstractWorkIdChooseTemplate中的chooseAndInit雪花算法初始化实例，
        //对应上了，程序的任何地方都可以通过 SnowflakeIdUtil.nextId()
        //来使用这个已经初始化好的 ID 生成器
        return SnowflakeIdUtil.nextId();
    }
}
