package com.guoxu.enmus;

import org.springframework.context.annotation.FullyQualifiedAnnotationBeanNameGenerator;

/**
 * StatusEnum
 * 表示实体的状态，包括成功和失败
 * @author 执笔画棠
 * @version 2025/11/04 15:26
 **/
public enum StatusEnum {
    SUCCESS(0),
    FAILE(1);

    private final Integer statusCode;
    StatusEnum(Integer statusCode) {
        this.statusCode = statusCode;
    }
     public Integer code(){
        return this.statusCode;
    }
     public String strCode(){
        return String.valueOf(this.statusCode);
    }
     @Override
     public String toString() {
        return this.statusCode.toString();
    }
}