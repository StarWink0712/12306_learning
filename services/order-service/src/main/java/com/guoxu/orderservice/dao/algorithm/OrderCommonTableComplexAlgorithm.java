package com.guoxu.orderservice.dao.algorithm;

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
 * OrderCommonTableComplexAlgorithm
 *
 * @author 执笔画棠
 * @date 2025/11/10 17:10
 **/
/**
 * 订单通用表分片复杂分片算法
 * 实现了ShardingSphere的ComplexKeysShardingAlgorithm接口
 * 用于根据多个分片键（用户ID或订单号）进行分表路由
 */
public class OrderCommonTableComplexAlgorithm implements ComplexKeysShardingAlgorithm {
    // 配置属性对象，用于存储分片算法配置
    @Getter
    private Properties props;

    // 总分片数量（表数量）
    private int shardingCount;

    // 配置文件中总分片数量的键名
    private static final String SHARDING_COUNT_KEY = "sharding-count";

    /**
     * 执行分片逻辑，根据分片值确定目标表
     *
     * @param availableTargetNames 所有可用的目标表名称集合
     * @param shardingValue        包含分片列和分片值的复杂分片值对象
     * @return 经过分片计算后命中的表名称集合
     */
    @Override
    public Collection<String> doSharding(Collection availableTargetNames, ComplexKeysShardingValue shardingValue) {
        // 从分片值对象中获取分片列名和对应分片值的映射关系
        Map<String, Collection<Comparable<?>>> columnNameAndShardingValuesMap = shardingValue
                .getColumnNameAndShardingValuesMap();

        // 创建结果集合，使用LinkedHashSet保持插入顺序，初始容量设为可用目标名称的大小
        Collection<String> result = new LinkedHashSet<>(availableTargetNames.size());

        // 检查分片值映射是否非空
        if (CollUtil.isNotEmpty(columnNameAndShardingValuesMap)) {
            // 定义用户ID分片列名
            String userId = "user_id";

            // 从映射中获取用户ID的分片值集合
            Collection<Comparable<?>> customerUserIdCollection = columnNameAndShardingValuesMap.get(userId);

            // 检查用户ID分片值集合是否非空（优先使用user_id进行分片）
            if (CollUtil.isNotEmpty(customerUserIdCollection)) {
                // 从用户ID集合中获取第一个分片值
                Comparable<?> comparable = customerUserIdCollection.stream().findFirst().get();

                // 判断分片值类型是否为字符串
                if (comparable instanceof String) {
                    // 将分片值转换为字符串
                    String actualUserId = comparable.toString();

                    // 构建分表名称：
                    // 1. 取逻辑表名
                    // 2. 取用户ID的后6位（如果长度不足6位则从开头取）
                    // 3. 对取出的字符串进行哈希计算
                    // 4. 对总分片数取模得到表后缀
                    result.add(shardingValue.getLogicTableName() + "_"
                            + hashShardingValue(actualUserId.substring(Math.max(actualUserId.length() - 6, 0)))
                            % shardingCount);
                } else {
                    // 对于非字符串类型（假设为Long），取模1000000后6位，再进行哈希和分片计算
                    String dbSuffix = String.valueOf(hashShardingValue((Long) comparable % 1000000) % shardingCount);

                    // 构建分表名称：逻辑表名 + 计算出的表后缀
                    result.add(shardingValue.getLogicTableName() + "_" + dbSuffix);
                }
            } else {
                // 如果没有用户ID分片值，则使用订单号进行分片
                String orderSn = "order_sn";

                // 从映射中获取订单号的分片值集合
                Collection<Comparable<?>> orderSnCollection = columnNameAndShardingValuesMap.get(orderSn);

                // 从订单号集合中获取第一个分片值
                Comparable<?> comparable = orderSnCollection.stream().findFirst().get();

                // 判断分片值类型是否为字符串
                if (comparable instanceof String) {
                    // 将分片值转换为字符串
                    String actualOrderSn = comparable.toString();

                    // 构建分表名称：
                    // 1. 取逻辑表名
                    // 2. 取订单号的后6位（如果长度不足6位则从开头取）
                    // 3. 对取出的字符串进行哈希计算
                    // 4. 对总分片数取模得到表后缀
                    result.add(shardingValue.getLogicTableName() + "_"
                            + hashShardingValue(actualOrderSn.substring(Math.max(actualOrderSn.length() - 6, 0)))
                            % shardingCount);
                } else {
                    // 对于非字符串类型（假设为Long），取模1000000后6位，再进行哈希和分片计算
                    String dbSuffix = String.valueOf(hashShardingValue((Long) comparable % 1000000) % shardingCount);

                    // 构建分表名称：逻辑表名 + 计算出的表后缀
                    result.add(shardingValue.getLogicTableName() + "_" + dbSuffix);
                }
            }
        }

        // 返回命中的表名称集合
        return result;
    }

    /**
     * 初始化分片算法
     *
     * @param props 配置属性，包含分片算法需要的参数
     */
    @Override
    public void init(Properties props) {
        // 保存配置属性
        this.props = props;

        // 从配置中获取总分片数量
        shardingCount = getShardingCount(props);
    }

    /**
     * 从配置属性中获取总分片数量
     *
     * @param props 配置属性
     * @return 总分片数量
     * @throws IllegalArgumentException 如果配置中缺少分片数量配置时抛出
     */
    private int getShardingCount(final Properties props) {
        // 检查配置中是否包含总分片数量键，如果不包含则抛出异常
        Preconditions.checkArgument(props.containsKey(SHARDING_COUNT_KEY), "Sharding count cannot be null.");

        // 从配置中获取并解析总分片数量
        return Integer.parseInt(props.getProperty(SHARDING_COUNT_KEY));
    }

    /**
     * 对分片值进行哈希计算
     *
     * @param shardingValue 分片值
     * @return 分片值的哈希绝对值（转为long类型）
     */
    private long hashShardingValue(final Comparable<?> shardingValue) {
        // 计算分片值的哈希码，取绝对值并转为long类型，确保为正数
        return Math.abs((long) shardingValue.hashCode());
    }
}
