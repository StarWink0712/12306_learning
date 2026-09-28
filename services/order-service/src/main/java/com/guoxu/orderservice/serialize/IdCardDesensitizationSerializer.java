package com.guoxu.orderservice.serialize;
import cn.hutool.core.util.DesensitizedUtil;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;

/**
 * IdCardDesensitizationSerializer
 * 身份证号脱敏反序列化
 * @author 执笔画棠
 * @date 2025/11/09 20:40
 **/
/**
 * 用hutool包的工具类，身份证号脱敏序列化器
 * 用于在JSON序列化过程中对身份证号进行脱敏处理，保护用户隐私信息
 * 继承自Jackson的JsonSerializer，专门处理String类型的身份证号字段
 */
public class IdCardDesensitizationSerializer extends JsonSerializer<String> {

    /**
     * 序列化方法 - 将身份证号进行脱敏处理后输出
     *
     * @param idCard 原始身份证号码字符串
     * @param jsonGenerator JSON生成器，用于写入处理后的数据
     * @param serializerProvider 序列化提供者，提供序列化相关服务
     * @throws IOException 当JSON生成过程中发生I/O异常时抛出
     *
     * 处理逻辑：
     * 1. 接收原始身份证号
     * 2. 调用脱敏工具对身份证号进行脱敏处理
     * 3. 将脱敏后的安全数据写入JSON输出
     */
    @Override
    public void serialize(String idCard, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {

        // 使用脱敏工具对身份证号进行脱敏处理
        // 参数说明：idCard-原始身份证号，4-保留前4位，4-保留后4位，中间部分用*号替换
        // 示例：原始"110101199001011234" -> 脱敏后"1101**********1234"
        String phoneDesensitization = DesensitizedUtil.idCardNum(idCard, 4, 4);

        // 将脱敏后的身份证号写入JSON生成器
        // 这样在API响应中返回的就是脱敏后的安全数据，而非原始敏感信息
        jsonGenerator.writeString(phoneDesensitization);
    }
}
