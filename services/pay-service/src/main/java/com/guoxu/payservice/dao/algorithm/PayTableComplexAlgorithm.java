package com.guoxu.payservice.dao.algorithm;

/**
 * PayTableComplexAlgorithm
 *
 * @author 执笔画棠
 * @date 2025/11/11 19:57
 **/

import cn.hutool.core.collection.CollUtil;
import com.google.common.base.Preconditions;
import lombok.Getter;
import org.apache.shardingsphere.sharding.api.sharding.complex.ComplexKeysShardingAlgorithm;
import org.apache.shardingsphere.sharding.api.sharding.complex.ComplexKeysShardingValue;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Properties;

/**
 * 支付表相关复合分片算法配置
 * 公众号：马丁玩编程，回复：加群，添加马哥微信（备注：12306）获取项目资料
 */
// PayTableComplexAlgorithm类实现了ComplexKeysShardingAlgorithm接口，用于实现支付表的复杂分片算法
public class PayTableComplexAlgorithm implements ComplexKeysShardingAlgorithm {


    // 用于存储配置属性，通过@Getter注解自动生成getProps()方法获取该属性
    @Getter
    private Properties props;

    // 表示分片数量
    private int shardingCount;

    // 配置文件中用于指定分片数量的键
    private static final String SHARDING_COUNT_KEY = "sharding-count";

    // 执行分片操作的方法
    @Override
    public Collection<String> doSharding(Collection availableTargetNames, ComplexKeysShardingValue shardingValue) {
        // 获取包含列名和分片值的映射集合
        Map<String, Collection<Comparable<?>>> columnNameAndShardingValuesMap = shardingValue
                .getColumnNameAndShardingValuesMap();
        // 用于存储分片结果的集合，初始容量为可用目标表名集合的大小
        Collection<String> result = new LinkedHashSet<>(availableTargetNames.size());
        // 如果列名和分片值的映射集合不为空
        if (CollUtil.isNotEmpty(columnNameAndShardingValuesMap)) {
            // 定义要获取分片值的列名为"order_sn"
            String userId = "order_sn";
            // 获取"order_sn"对应的分片值集合
            Collection<Comparable<?>> customerUserIdCollection = columnNameAndShardingValuesMap.get(userId);
            // 如果"order_sn"的分片值集合不为空
            if (CollUtil.isNotEmpty(customerUserIdCollection)) {
                // 获取集合中的第一个分片值
                Comparable<?> comparable = customerUserIdCollection.stream().findFirst().get();
                // 如果分片值是字符串类型
                if (comparable instanceof String) {
                    // 将分片值转换为字符串
                    String actualOrderSn = comparable.toString();
                    // 取字符串最后6位，计算哈希值并对分片数量取模，拼接逻辑表名和结果后缀
                    result.add(shardingValue.getLogicTableName() + "_"
                            + hashShardingValue(actualOrderSn.substring(Math.max(actualOrderSn.length() - 6, 0)))
                            % shardingCount);
                } else {
                    // 如果分片值是Long类型，对Long值取模1000000后计算哈希值，再对分片数量取模得到表后缀
                    String dbSuffix = String.valueOf(hashShardingValue((Long) comparable % 1000000) % shardingCount);
                    // 拼接逻辑表名和表后缀
                    result.add(shardingValue.getLogicTableName() + "_" + dbSuffix);
                }
            } else {
                // 如果"order_sn"的分片值集合为空，定义要获取分片值的列名为"pay_sn"
                String orderSn = "pay_sn";
                // 获取"pay_sn"对应的分片值集合
                Collection<Comparable<?>> orderSnCollection = columnNameAndShardingValuesMap.get(orderSn);
                // 获取集合中的第一个分片值
                Comparable<?> comparable = orderSnCollection.stream().findFirst().get();
                // 如果分片值是字符串类型
                if (comparable instanceof String) {
                    // 将分片值转换为字符串
                    String actualOrderSn = comparable.toString();
                    // 取字符串最后6位，计算哈希值并对分片数量取模，拼接逻辑表名和结果后缀
                    result.add(shardingValue.getLogicTableName() + "_"
                            + hashShardingValue(actualOrderSn.substring(Math.max(actualOrderSn.length() - 6, 0)))
                            % shardingCount);
                } else {
                    // 如果分片值是Long类型，对Long值取模1000000后计算哈希值，再对分片数量取模得到表后缀
                    String dbSuffix = String.valueOf(hashShardingValue((Long) comparable % 1000000) % shardingCount);
                    // 拼接逻辑表名和表后缀
                    result.add(shardingValue.getLogicTableName() + "_" + dbSuffix);
                }
            }
        }
        // 返回分片结果集合
        return result;
    }

    // 初始化方法，在算法初始化时被调用
    @Override
    public void init(Properties props) {
        // 保存传入的配置属性
        this.props = props;
        // 通过getShardingCount方法获取分片数量
        shardingCount = getShardingCount(props);
    }

    // 获取分片数量的方法
    private int getShardingCount(final Properties props) {
        // 检查配置属性中是否包含SHARDING_COUNT_KEY，若不包含则抛出异常
        Preconditions.checkArgument(props.containsKey(SHARDING_COUNT_KEY), "Sharding count cannot be null.");
        // 从配置属性中获取分片数量并转换为整数返回
        return Integer.parseInt(props.getProperty(SHARDING_COUNT_KEY));
    }

    // 计算分片值哈希值的方法
    private long hashShardingValue(final Comparable<?> shardingValue) {
        // 计算传入分片值的哈希值，并取绝对值返回
        return Math.abs((long) shardingValue.hashCode());
    }
}
