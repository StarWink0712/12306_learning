package com.guoxu.core.snowflake;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;

/**
 * RandomWorkIdChoose 随机id生成器
 *
 * @author 执笔画棠
 * @date 2025/11/06 17:30
 **/
@Slf4j
public class RandomWorkIdChoose extends AbstractWorkIdChooseTemplate implements InitializingBean {
    @Override
    protected WorkIdWrapper chooseWorkId() {
        int start = 0, end = 31;
        // 生成随机的 WorkId 和 DataCenterId
        return new WorkIdWrapper(getRandom(start, end), getRandom(start, end));
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        //直接用AbstractWorkIdChooseTemplate的chooseAndInit方法
        chooseAndInit();
    }

    private static long getRandom(int start, int end) {
        // 生成随机数
        long random = (long) (Math.random() * (end - start + 1) + start);
        return random;
    }
}
