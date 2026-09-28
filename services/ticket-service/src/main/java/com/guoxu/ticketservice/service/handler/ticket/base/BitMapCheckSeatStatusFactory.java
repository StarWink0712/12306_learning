package com.guoxu.ticketservice.service.handler.ticket.base;

import com.guoxu.Singleton;

/**
 * BitMapCheckSeatStatusFactory
 * 座位通过 BitMap 检测抽象工厂
 * @author 执笔画棠
 * @date 2025/11/13 19:32
 **/
public abstract class BitMapCheckSeatStatusFactory {
    public static final String TRAIN_BUSINESS = "TRAIN_BUSINESS";
    public static final String TRAIN_FIRST = "TRAIN_FIRST";
    public static final String TRAIN_SECOND = "TRAIN_SECOND";

    /**
     * 获取座位检查方法实例
     *
     * @param mark 座位标识
     * @return 座位检查类
     */
    public static BitMapCheckSeat getInstance(String mark) {
        BitMapCheckSeat instance = null;
        switch (mark) {
            case TRAIN_BUSINESS -> {
                instance = Singleton.get(TRAIN_BUSINESS);
                if (instance == null) {
                    instance = new TrainBusinessCheckSeat();
                    Singleton.put(TRAIN_BUSINESS, instance);
                }
            }
            case TRAIN_FIRST -> {
                instance = Singleton.get(TRAIN_FIRST);
                if (instance == null) {
                    instance = new TrainFirstCheckSeat();
                    Singleton.put(TRAIN_FIRST, instance);
                }
            }
            case TRAIN_SECOND -> {
                instance = Singleton.get(TRAIN_SECOND);
                if (instance == null) {
                    instance = new TrainSecondCheckSeat();
                    Singleton.put(TRAIN_SECOND, instance);
                }
            }
        }
        return instance;
    }
}

