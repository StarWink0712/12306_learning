package com.guoxu.payservice.dao.algorithm;

/**
 * PayDataBaseComplexAlgorithm
 *
 * @author 执笔画棠
 * @date 2025/11/11 19:56
 **/

import cn.hutool.core.collection.CollUtil;
import lombok.Getter;
import org.apache.shardingsphere.infra.util.exception.ShardingSpherePreconditions;
import org.apache.shardingsphere.sharding.api.sharding.complex.ComplexKeysShardingAlgorithm;
import org.apache.shardingsphere.sharding.api.sharding.complex.ComplexKeysShardingValue;
import org.apache.shardingsphere.sharding.exception.algorithm.sharding.ShardingAlgorithmInitializationException;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Properties;

/**
 * 支付数据库复合分片算法配置
 * 公众号：马丁玩编程，回复：加群，添加马哥微信（备注：12306）获取项目资料
 */
// 支付数据库复合分片算法类，实现ShardingSphere的复合键分片算法接口
public class PayDataBaseComplexAlgorithm implements ComplexKeysShardingAlgorithm {

    // 配置属性
    @Getter
    private Properties props;

    // 分片总数（数据库数量 * 每库表数量）
    private int shardingCount;
    // 每个数据库中的表分片数量
    private int tableShardingCount;

    // 配置键常量
    private static final String SHARDING_COUNT_KEY = "sharding-count";
    private static final String TABLE_SHARDING_COUNT_KEY = "table-sharding-count";

    // 分片算法核心方法，根据分片值确定数据应该路由到哪个数据源
    @Override
    public Collection<String> doSharding(Collection availableTargetNames, ComplexKeysShardingValue shardingValue) {
        // 获取分片列和对应的分片值映射
        Map<String, Collection<Comparable<Long>>> columnNameAndShardingValuesMap = shardingValue
                .getColumnNameAndShardingValuesMap();
        // 使用LinkedHashSet保存结果，保证顺序且去重
        Collection<String> result = new LinkedHashSet<>(availableTargetNames.size());

        // 检查分片值映射是否非空
        if (CollUtil.isNotEmpty(columnNameAndShardingValuesMap)) {
            // 优先使用order_sn作为分片键
            String userId = "order_sn";
            Collection<Comparable<Long>> customerUserIdCollection = columnNameAndShardingValuesMap.get(userId);

            if (CollUtil.isNotEmpty(customerUserIdCollection)) {
                // 如果order_sn分片值存在，使用order_sn进行分片计算
                String dbSuffix;
                // 获取第一个分片值（通常复合分片情况下只有一个值）
                Comparable<?> comparable = customerUserIdCollection.stream().findFirst().get();

                if (comparable instanceof String) {
                    // 如果分片值是字符串类型（如订单号）
                    String actualOrderSn = comparable.toString();
                    // 取订单号最后6位进行哈希分片计算，确保分布均匀
                    dbSuffix = String
                            .valueOf(hashShardingValue(actualOrderSn.substring(Math.max(actualOrderSn.length() - 6, 0)))
                                    % shardingCount / tableShardingCount);
                } else {
                    // 如果分片值是Long类型，取模后6位进行分片计算
                    dbSuffix = String.valueOf(
                            hashShardingValue((Long) comparable % 1000000) % shardingCount / tableShardingCount);
                }
                // 构建数据源名称，格式为"ds_分片后缀"
                result.add("ds_" + dbSuffix);
            } else {
                // 如果order_sn不存在，则使用pay_sn作为备选分片键
                String dbSuffix;
                String orderSn = "pay_sn";
                Collection<Comparable<Long>> orderSnCollection = columnNameAndShardingValuesMap.get(orderSn);
                // 获取pay_sn的第一个分片值
                Comparable<?> comparable = orderSnCollection.stream().findFirst().get();

                if (comparable instanceof String) {
                    // 字符串类型的pay_sn处理：取最后6位进行分片
                    String actualOrderSn = comparable.toString();
                    dbSuffix = String
                            .valueOf(hashShardingValue(actualOrderSn.substring(Math.max(actualOrderSn.length() - 6, 0)))
                                    % shardingCount / tableShardingCount);
                } else {
                    // Long类型的pay_sn处理：取模后6位进行分片
                    dbSuffix = String.valueOf(
                            hashShardingValue((Long) comparable % 1000000) % shardingCount / tableShardingCount);
                }
                // 构建数据源名称
                result.add("ds_" + dbSuffix);
            }
        }
        return result;
    }

    // 初始化方法，在算法实例创建时调用
    @Override
    public void init(Properties props) {
        // 保存配置属性
        this.props = props;
        // 从配置中获取分片总数
        shardingCount = getShardingCount(props);
        // 从配置中获取表分片数量
        tableShardingCount = getTableShardingCount(props);
    }

    // 获取分片总数配置
    private int getShardingCount(final Properties props) {
        // 检查配置中是否包含分片总数键
        ShardingSpherePreconditions.checkState(props.containsKey(SHARDING_COUNT_KEY),
                () -> new ShardingAlgorithmInitializationException(getType(), "Sharding count cannot be null."));
        // 解析并返回分片总数
        return Integer.parseInt(props.getProperty(SHARDING_COUNT_KEY));
    }

    // 获取表分片数量配置
    private int getTableShardingCount(final Properties props) {
        // 检查配置中是否包含表分片数量键
        ShardingSpherePreconditions.checkState(props.containsKey(TABLE_SHARDING_COUNT_KEY),
                () -> new ShardingAlgorithmInitializationException(getType(), "Table sharding count cannot be null."));
        // 解析并返回表分片数量
        return Integer.parseInt(props.getProperty(TABLE_SHARDING_COUNT_KEY));
    }

    // 哈希分片值计算方法，确保分片值均匀分布
    private long hashShardingValue(final Comparable<?> shardingValue) {
        // 取哈希码的绝对值，确保结果为非负数
        return Math.abs((long) shardingValue.hashCode());
    }
}
