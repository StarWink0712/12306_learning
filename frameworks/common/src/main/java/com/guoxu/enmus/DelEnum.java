package com.guoxu.enmus;

/**
 * DelEum
 * 删除标记枚举类,软删除机制
 * 表示数据在数据库中不会被真的删除，而是通过删除标记来表示数据已被删除
 * 0：表示数据未被删除
 * 1：表示数据已被删除
 * @author 执笔画棠
 * @version 2025/11/04 15:12
 **/
public enum DelEnum {

    NORMAL(0),

    DELETE(1);

    private final Integer statusCode;

    DelEnum(Integer statusCode) {
        this.statusCode = statusCode;
    }

    public Integer code(){
        return this.statusCode;
    }

    public  String strCode(){
        return String.valueOf(this.statusCode);
    }

    @Override
    public String toString() {
        return this.statusCode.toString();
    }

}