package com.guoxu.toolkit;

import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.Collection;
import java.util.Map;
/**
 * Assert 断言工具类
 *
 * @author 执笔画棠
 * @date 2025/11/04 18:09
 **/
public class Assert {
    /**
     * 断言表达式为 true，否则抛出 IllegalArgumentException 异常。
     *
     * @param expression 断言表达式
     * @param message    异常消息
     */
    public static void isTrue(boolean expression, String message) {
        if (!expression) {
            throw new IllegalArgumentException(message);
        }
    }

    public static void isTrue(boolean expression) {
        isTrue(expression, "[Assertion failed] - this expression must be true");
    }

    /**
     * 断言对象为 null，否则抛出 IllegalArgumentException 异常。
     *
     * @param object  要断言的对象
     * @param message 异常消息
     */
    public static void isNull(Object object, String message) {
        if (object != null) {
            throw new IllegalArgumentException(message);
        }
    }

    /**
     * 断言对象为 null，否则抛出 IllegalArgumentException 异常。
     *
     * @param object 要断言的对象
     */
    public static void isNull(Object object) {
        isNull(object, "[Assertion failed] - the object argument must be null");
    }

    public static void notNull(Object object, String message) {
        if (object == null) {
            throw new IllegalArgumentException(message);
        }
    }

    public static void notNull(Object object) {
        notNull(object, "[Assertion failed] - this argument is required; it must not be null");
    }

    public static void notEmpty(Collection<?> collection, String message) {
        if (CollectionUtils.isEmpty(collection)) {
            throw new IllegalArgumentException(message);
        }
    }

    public static void notEmpty(Collection<?> collection) {
        notEmpty(collection,
                "[Assertion failed] - this collection must not be empty: it must contain at least 1 element");
    }

    public static void notEmpty(Map<?, ?> map, String message) {
        if (CollectionUtils.isEmpty(map)) {
            throw new IllegalArgumentException(message);
        }
    }

    public static void notEmpty(Map<?, ?> map) {
        notEmpty(map, "[Assertion failed] - this map must not be empty; it must contain at least one entry");
    }

    /**
     * 断言字符串不为空，否则抛出 IllegalArgumentException 异常。
     *
     * @param str     要断言的字符串
     * @param message 异常消息
     */
    public static void notEmpty(String str, String message) {
        if (StringUtils.isEmpty(str)) {
            throw new IllegalArgumentException(message);
        }
    }

    /**
     * 断言字符串不为空，否则抛出 IllegalArgumentException 异常。
     *
     * @param str 要断言的字符串
     */
    public static void notEmpty(String str) {
        if (StringUtils.isEmpty(str)) {
            notEmpty(str, "[Assertion failed] - this string must not be empty");
        }
    }

    /**
     * 断言字符串不为空，否则抛出 IllegalArgumentException 异常。
     *
     * @param str     要断言的字符串
     * @param message 异常消息
     */
    public static void notBlank(String str, String message) {
        if (org.apache.commons.lang3.StringUtils.isBlank(str)) {
            throw new IllegalArgumentException(message);
        }
    }

    public static void notBlank(String str) {
        notBlank(str, "[Assertion failed] - this string must not be blank");
    }

    /**
     * 断言字符串包含文本，否则抛出 IllegalArgumentException 异常。
     *
     * @param text    要断言的字符串
     * @param message 异常消息
     */
    public static void hasText(String text, String message) {
        if (!StringUtils.hasText(text)) {
            throw new IllegalArgumentException(message);
        }
    }

    public static void hasText(String text) {
        hasText(text,
                "[Assertion failed] - this String argument must have text; it must not be null, empty, or blank");
    }
}
