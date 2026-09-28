package com.guoxu.enmus;

/**
 * FlagEnum
 *
  * @description:
  * @date: 2025/11/4 15:23
  * @author: 执笔画棠
  * @return:

 * @author 执笔画棠
 * @version 2025/11/04 15:23
 **/
public enum FlagEnum {

    FALSE(0),
    TRUE(1);

    private final Integer flag;

    FlagEnum (Integer flag){
        this.flag=flag;
    }

    public Integer code(){
        return this.flag;
    }

    public String strCode(){
        return String.valueOf(this.flag);
    }

    @Override
    public String toString() {
        return this.flag.toString();
    }
}