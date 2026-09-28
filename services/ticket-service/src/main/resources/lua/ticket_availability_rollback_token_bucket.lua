--这段 Lua 脚本主要用于处理与票座可用性令牌相关的逻辑。它接收来自 Redis 的键值对和参数，
--解析 JSON 格式的数据，遍历这些数据，并根据特定条件在 Redis 的哈希表中增加相应的票座可用性令牌数量。
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
-- 将传入的第二个参数（通常是另一个JSON格式的字符串数组）赋值给alongJsonArrayStr
local alongJsonArrayStr = ARGV[2]
-- 将alongJsonArrayStr解析为Lua的表（table），赋值给alongJsonArray
local alongJsonArray = cjson.decode(alongJsonArrayStr)

-- 遍历jsonArray数组
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
        -- 获取在第一个键（KEYS[1]）对应的哈希表中，actualInnerHashKey对应的票座可用性令牌值，并转换为数字类型
        local ticketSeatAvailabilityTokenValue = tonumber(redis.call('hget', KEYS[1], tostring(actualInnerHashKey)))
        -- 如果票座可用性令牌值大于等于0
        if ticketSeatAvailabilityTokenValue >= 0 then
            -- 在第一个键（KEYS[1]）对应的哈希表中，对actualInnerHashKey对应的数值增加count
            redis.call('hincrby', KEYS[1], tostring(actualInnerHashKey), count)
        end
    end
end

-- 返回0，通常表示操作成功完成（在这种上下文中，返回值可能用于指示脚本的执行状态）
return 0