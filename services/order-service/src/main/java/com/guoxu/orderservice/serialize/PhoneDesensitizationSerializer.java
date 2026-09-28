package com.guoxu.orderservice.serialize;

/**
 * PhoneDesensitizationSerializer
 *
 * @author 执笔画棠
 * @date 2025/11/10 20:15
 **/

import cn.hutool.core.util.DesensitizedUtil;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;

/**
 * 手机号脱敏反序列化
 */
// 定义一个公开的手机号脱敏序列化器类，继承自Jackson库的JsonSerializer<String>
// 用于在JSON序列化时对String类型的手机号进行自定义脱敏处理
public class PhoneDesensitizationSerializer extends JsonSerializer<String> {

    // 重写父类JsonSerializer的serialize方法，实现自定义的序列化逻辑
    @Override
    // 序列化方法：将原始手机号脱敏后写入JSON
    // 参数说明：
    // phone：待序列化的原始手机号字符串
    // jsonGenerator：用于生成JSON输出的工具对象
    // serializerProvider：提供序列化上下文信息的对象
    // 声明可能抛出IOException（IO异常，如写入JSON时发生错误）
    public void serialize(String phone, JsonGenerator jsonGenerator, SerializerProvider serializerProvider)
            throws IOException {
        // 调用脱敏工具类DesensitizedUtil的mobilePhone方法，对原始手机号进行脱敏处理
        // 将脱敏后的结果赋值给phoneDesensitization变量
        String phoneDesensitization = DesensitizedUtil.mobilePhone(phone);
        // 通过jsonGenerator工具，将脱敏后的手机号字符串写入JSON输出流，完成序列化
        jsonGenerator.writeString(phoneDesensitization);
    }
}
