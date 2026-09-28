
--这段 Lua 脚本主要实现了检查票座可用性令牌是否足够，并在足够的情况下减少相应的令牌数量的功能。
--它接收 Redis 的键值对和参数，解析 JSON 数据，通过比较令牌值和所需数量来判断令牌是否足够，
--若不足则返回相关信息；若足够则在相关哈希表中减少对应的令牌数量
-- 将传入的第二个键值（通常是一个包含键名的字符串）赋值给变量inputString
local inputString = KEYS[2]
-- 将inputString赋值给actualKey，后续可能会对actualKey进行处理
local actualKey = inputString
-- 在actualKey中查找冒号（":"）的位置，返回冒号的索引，如果未找到则返回nil
local colonIndex = string.find(actualKey, ":")
-- 如果找到了冒号
if colonIndex ~= nil then
    -- 从冒号后的位置开始截取字符串，更新actualKey
    actualKey = string.sub(actualKey, colonIndex + 1)
end

-- 将传入的第一个参数（通常是一个JSON格式的字符串数组）赋值给jsonArrayStr
local jsonArrayStr = ARGV[1]
-- 将jsonArrayStr解析为Lua的表（table），赋值给jsonArray
local jsonArray = cjson.decode(jsonArrayStr)

-- 创建一个空表result，用于存储结果
local result = {}
-- 初始化一个布尔变量tokenIsNull，表示令牌是否为空，初始值为false
local tokenIsNull = false
-- 创建一个空表tokenIsNullSeatTypeCounts，用于存储令牌为空的座位类型及数量信息
local tokenIsNullSeatTypeCounts = {}

-- 遍历jsonArray数组
for index, jsonObj in ipairs(jsonArray) do
    -- 从当前json对象中获取seatType字段，并转换为数字类型，赋值给seatType
    local seatType = tonumber(jsonObj.seatType)
    -- 从当前json对象中获取count字段，并转换为数字类型，赋值给count
    local count = tonumber(jsonObj.count)
    -- 构建内部哈希键，格式为actualKey_seatType
    local actualInnerHashKey = actualKey .. "_" .. seatType
    -- 获取在第一个键（KEYS[1]）对应的哈希表中，actualInnerHashKey对应的票座可用性令牌值，并转换为数字类型
    local ticketSeatAvailabilityTokenValue = tonumber(redis.call('hget', KEYS[1], tostring(actualInnerHashKey)))
    -- 如果票座可用性令牌值小于count
    if ticketSeatAvailabilityTokenValue < count then
        -- 将tokenIsNull设为true，表示令牌为空
        tokenIsNull = true
        -- 将座位类型和数量的组合信息添加到tokenIsNullSeatTypeCounts表中
        table.insert(tokenIsNullSeatTypeCounts, seatType .. "_" .. count)
    end
end

-- 将tokenIsNull的值存入result表中
result['tokenIsNull'] = tokenIsNull
-- 如果tokenIsNull为true
if tokenIsNull then
    -- 将tokenIsNullSeatTypeCounts表存入result表中
    result['tokenIsNullSeatTypeCounts'] = tokenIsNullSeatTypeCounts
    -- 将result表编码为JSON字符串并返回，此时表示令牌不足
    return cjson.encode(result)
end

-- 将传入的第二个参数（通常是另一个JSON格式的字符串数组）赋值给alongJsonArrayStr
local alongJsonArrayStr = ARGV[2]
-- 将alongJsonArrayStr解析为Lua的表（table），赋值给alongJsonArray
local alongJsonArray = cjson.decode(alongJsonArrayStr)

-- 再次遍历jsonArray数组
for index, jsonObj in ipairs(jsonArray) do
    -- 从当前json对象中获取seatType字段，并转换为数字类型，赋值给seatType
    local seatType = tonumber(jsonObj.seatType)
    -- 从当前json对象中获取count字段，并转换为数字类型，赋值给count
    local count = tonumber(jsonObj.count)
    -- 遍历alongJsonArray数组
    for indexTwo, alongJsonObj in ipairs(alongJsonArray) do
        -- 从当前alongJson对象中获取startStation字段，并转换为字符串类型，赋值给startStation
        local startStation = tostring(alongJsonObj.startStation)
        -- 从当前alongJson对象中获取endStation字段，并转换为字符串类型，赋值给endStation
        local endStation = tostring(alongJsonObj.endStation)
        -- 构建内部哈希键，格式为startStation_endStation_seatType
        local actualInnerHashKey = startStation .. "_" .. endStation .. "_" .. seatType
        -- 在第一个键（KEYS[1]）对应的哈希表中，对actualInnerHashKey对应的数值减少count
        redis.call('hincrby', KEYS[1], tostring(actualInnerHashKey), -count)
    end
end

-- 将result表编码为JSON字符串并返回，此时表示令牌充足且已完成相应的令牌减少操作
return cjson.encode(result)