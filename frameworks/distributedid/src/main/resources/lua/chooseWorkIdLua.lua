-- 定义Redis键名和哈希字段名
local hashKey = 'snowflake_work_id_key'       -- 存储工作节点信息的哈希表主键
local dataCenterIdKey = 'dataCenterId'        -- 哈希表中表示数据中心ID的字段名
local workIdKey = 'workId'                     -- 哈希表中表示工作节点ID的字段名

-- 检查哈希表是否存在
if (redis.call('exists', hashKey) == 0) then
    -- 哈希表不存在时初始化字段（设置为0）
    redis.call('hincrby', hashKey, dataCenterIdKey, 0)  -- 初始化数据中心ID为0
    redis.call('hincrby', hashKey, workIdKey, 0)         -- 初始化工作节点ID为0
    return { 0, 0 }  -- 返回初始值(0,0)
end

-- 从哈希表读取当前值并转为数字
local dataCenterId = tonumber(redis.call('hget', hashKey, dataCenterIdKey))  -- 获取数据中心ID
local workId = tonumber(redis.call('hget', hashKey, workIdKey))              -- 获取工作节点ID

-- 定义ID最大值（5位二进制最大值）
local max = 31  -- Snowflake算法中通常占5位，范围0-31

-- 初始化结果变量
local resultWorkId = 0       -- 最终输出的工作节点ID
local resultDataCenterId = 0  -- 最终输出的数据中心ID

-- 情况1：当两个ID都达到最大值时（31）
if (dataCenterId == max and workId == max) then
    -- 重置两个ID为0
    redis.call('hset', hashKey, dataCenterIdKey, '0')  -- 设置数据中心ID为0
    redis.call('hset', hashKey, workIdKey, '0')         -- 设置工作节点ID为0
    -- 注意：此处未显式设置result变量，将返回初始值(0,0)

-- 情况2：当工作节点ID未满时（<31）
elseif (workId ~= max) then
    -- 工作节点ID自增1，保持数据中心ID不变
    resultWorkId = redis.call('hincrby', hashKey, workIdKey, 1)  -- 原子增加工作ID
    resultDataCenterId = dataCenterId  -- 继承当前数据中心ID

-- 情况3：当工作节点ID已满但数据中心ID未满时
elseif (dataCenterId ~= max) then
    -- 重置工作节点ID为0，数据中心ID自增1
    resultWorkId = 0  -- 工作节点ID归零
    resultDataCenterId = redis.call('hincrby', hashKey, dataCenterIdKey, 1)  -- 数据中心ID自增
    redis.call('hset', hashKey, workIdKey, '0')  -- 显式重置工作节点ID为0
end

-- 返回生成的ID对 {工作节点ID, 数据中心ID}
return { resultWorkId, resultDataCenterId }