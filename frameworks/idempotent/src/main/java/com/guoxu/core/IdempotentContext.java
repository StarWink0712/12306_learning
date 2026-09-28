package com.guoxu.core;

import cn.hutool.core.collection.CollUtil;

import java.util.HashMap;
import java.util.Map;

/**
 * IdempotentContext
 *
 * @author 执笔画棠
 * @date 2025/11/06 20:17
 **/
public final class IdempotentContext {

    //声明为final仿真被继承
    private static final ThreadLocal<Map<String,Object>> CONTEXT=new ThreadLocal<>();

    //获取当前线程的完整上下文
    public static Map<String,Object> get(){
        return CONTEXT.get();
    }

    /**
     * 根据键获取当前线程上下文Map中的值
     *
     * @param key 要获取的值的键
     * @return 键对应的 value，若Map为空或键不存在则返回null
     */
    public static Object getKey(String key){
        Map<String,Object> context=get();// 先获取当前线程的上下文Map
        if(CollUtil.isNotEmpty(context)){
            return context.get(key);// 从Map中获取指定键的值
        }
        return null;
    }
    /**
     * 根据键获取当前线程上下文Map中的值，并转为字符串
     *
     * @param key 要获取的值的键
     * @return 键对应的值的字符串形式，若值为null则返回null
     */
    public static String getString(String key) {
        Object actual = getKey(key); // 先通过getKey()获取原始值
        if (actual != null) { // 若原始值不为null
            return actual.toString(); // 转为字符串返回
        }
        return null; // 若原始值为null，返回null
    }

    /**
     * 往当前线程的上下文Map中添加键值对
     *
     * @param key 键
     * @param val 要存储的值
     */
    public static void put(String key, Object val) {
        Map<String, Object> context = get(); // 获取当前线程的上下文Map
        if (CollUtil.isEmpty(context)) { // 若Map为空（未初始化）
            context = new HashMap<>(); // 新建一个HashMap（Maps是工具类，简化集合创建）
        }
        context.put(key, val); // 往Map中添加键值对
        putContext(context); // 将更新后的Map重新设置到ThreadLocal中
    }

    /**
     * 将传入的Map合并到当前线程的上下文Map中
     *
     * @param context 要合并的Map
     */
    public static void putContext(Map<String, Object> context) {
        Map<String, Object> threadContext = CONTEXT.get(); // 获取当前线程已有的上下文Map
        if (CollUtil.isNotEmpty(threadContext)) { // 若已有Map非空
            threadContext.putAll(context); // 合并传入的Map（将传入的键值对添加到已有Map中）
            return; // 合并后直接返回，无需重新设置
        }
        CONTEXT.set(context); // 若已有Map为空，直接将传入的Map设置到ThreadLocal中
    }

    /**
     * 清理当前线程的上下文数据（必须在处理结束后调用，避免内存泄漏）
     */
    public static void clean() {
        CONTEXT.remove(); // 调用ThreadLocal的remove()方法，移除当前线程绑定的Map
    }

}
