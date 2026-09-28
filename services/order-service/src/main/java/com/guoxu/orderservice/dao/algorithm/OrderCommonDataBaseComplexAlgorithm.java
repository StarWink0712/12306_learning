package com.guoxu.orderservice.dao.algorithm;

import cn.hutool.core.collection.CollUtil;
import org.apache.shardingsphere.infra.util.exception.ShardingSpherePreconditions;
import org.apache.shardingsphere.sharding.api.sharding.complex.ComplexKeysShardingAlgorithm;
import org.apache.shardingsphere.sharding.api.sharding.complex.ComplexKeysShardingValue;
import org.apache.shardingsphere.sharding.exception.algorithm.sharding.ShardingAlgorithmInitializationException;

import java.security.Principal;
import java.util.*;

/**
 * OrderCommonDataBaseComplexAlgorithm
 *
 * @author 执笔画棠
 * @date 2025/11/10 14:35
 **/
/**
 * 订单通用数据分库复杂分片算法
 * 实现了ShardingSphere的ComplexKeysShardingAlgorithm接口
 * 用于根据多个分片键（用户ID或订单号）进行分库路由
 */
public class OrderCommonDataBaseComplexAlgorithm implements ComplexKeysShardingAlgorithm {

    //配置属性对象，用于存储分片算法配置
    private Properties props;

    //总分片数量（即数据库实例的数量）
    private int shardingCount;

    //表分片数量，一个数据库中表的数量
    private int tableShardingCount;

    //配置文件中总分片数量的键名
    private static final String SHARDING_COUNT_KEY = "sharding-count";

    //配置文件中表分片数量的键名
    private static final String TABLE_SHARDING_COUNT_KEY = "table-sharding-count";

    /**
     * 执行分片逻辑，根据分片值确定目标数据源
     *
     * @param availableTargetNames 所有可用的目标数据源名称集合，即所有数据库实例的名称
     * @param shardingValue        包含分片列和分片值的复杂分片值对象
     * @return 经过分片计算后命中的数据源名称集合
     */
    @Override
    public Collection<String> doSharding(Collection availableTargetNames, ComplexKeysShardingValue shardingValue) {

        //从分片值对象中获取分片列名和对应分片值的映射关系 即sql语句中where子句的分片键和对应的值，
        //例如：user_id=123 and order_id=456 中，user_id和order_id就是分片键，123和456就是对应的分片值
        Map<String,Collection<Comparable<?>>> columnNameAndShardingValuesMap=shardingValue.getColumnNameAndShardingValuesMap();

        //创建结果集合，使用linkedlist保持插入顺序，size初始化为分片数量，即有多少个数据库实例
        Collection<String> result=new LinkedHashSet<>(availableTargetNames.size());

        //检查分片值映射是否为空，
        if(CollUtil.isNotEmpty(columnNameAndShardingValuesMap)){
            //定义用户id分片列名
            String userId="user_id";

            //从映射中获取用户id的分片值集合，取出分片值，即用户id的值，例如123
            Collection<Comparable<?>> customerUserIdCollection=columnNameAndShardingValuesMap.get(userId);

            //检查用户ID分片值集合是否为空，如果不为空
            if(CollUtil.isNotEmpty(customerUserIdCollection)){
                //数据库后缀字符串
                String dbSuffix;

                //从用户id集合中获取第一个分片值，即用户ID的值，例如123
                Comparable<?> comparable=customerUserIdCollection.stream().findFirst().get();

                //判断分片值类型是不是字符串
                if(comparable instanceof String){
                    //将分片值转变成字符串
                    String actualUserId=comparable.toString();

                    //计算数据库后缀
                    dbSuffix=String.valueOf(hashShardingValue(actualUserId.substring(Math.max(actualUserId.length()-6,0)))
                    % shardingCount/tableShardingCount);
                }else {
                    //对应非字符串类型，取模后六位，再进行哈希和分片运算
                    dbSuffix=String.valueOf(hashShardingValue((Long) comparable % 1000000) % shardingCount / tableShardingCount);
                }

                //将计算出的数据库名称添加到结果集合中
                result.add("ds_"+dbSuffix);
            }
        }


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

        // 从配置中获取表分片数量
        tableShardingCount = getTableShardingCount(props);
    }

    /**
     * 从配置属性中获取总分片数量
     *
     * @param props 配置属性
     * @return 总分片数量
     * @throws ShardingAlgorithmInitializationException 如果配置中缺少分片数量配置时抛出
     */
    private int getShardingCount(final Properties props) {
        // 检查配置中是否包含总分片数量键
        ShardingSpherePreconditions.checkState(props.containsKey(SHARDING_COUNT_KEY),
                () -> new ShardingAlgorithmInitializationException(getType(), "Sharding count cannot be null."));

        // 从配置中获取并解析总分片数量
        return Integer.parseInt(props.getProperty(SHARDING_COUNT_KEY));
    }

    /**
     * 从配置属性中获取表分片数量
     *
     * @param props 配置属性
     * @return 表分片数量
     * @throws ShardingAlgorithmInitializationException 如果配置中缺少表分片数量配置时抛出
     */
    private int getTableShardingCount(final Properties props) {
        // 检查配置中是否包含表分片数量键
        ShardingSpherePreconditions.checkState(props.containsKey(TABLE_SHARDING_COUNT_KEY),
                () -> new ShardingAlgorithmInitializationException(getType(), "Table sharding count cannot be null."));

        // 从配置中获取并解析表分片数量
        return Integer.parseInt(props.getProperty(TABLE_SHARDING_COUNT_KEY));
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
    /**
     * 获取分片算法类型
     *
     * @return 分片算法类型字符串
     */
    @Override
    public String getType() {
        // 返回算法类型标识
        return "CLASS_BASED";
    }
}
