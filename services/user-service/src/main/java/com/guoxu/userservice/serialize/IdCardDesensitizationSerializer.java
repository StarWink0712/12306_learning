package com.guoxu.userservice.serialize;

import cn.hutool.core.util.DesensitizedUtil;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;

/**
 * IdCardDesensitizationSerializer
 * 身份证号脱敏反序列化
 * @author 执笔画棠
 * @date 2025/11/18 16:31
 **/
public class IdCardDesensitizationSerializer extends JsonSerializer<String> {\
    @Override
    public void serialize(String idCard, JsonGenerator jsonGenerator, SerializerProvider serializerProvider)
            throws IOException {
        String idCardDesensitization = DesensitizedUtil.idCardNum(idCard, 4, 4);
        jsonGenerator.writeString(idCardDesensitization);
    }
}
