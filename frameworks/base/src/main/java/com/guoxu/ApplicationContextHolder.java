package com.guoxu;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import javax.swing.text.DefaultEditorKit;
import java.lang.annotation.Annotation;
import java.util.Map;

/**
 * ApplicationContextHolder 应用上下文持有者
 *
 * @author 执笔画棠
 * @date 2025/11/04 19:13
 **/
public class ApplicationContextHolder implements ApplicationContextAware {


    //应用上下文,即spring容器的入口，ioc容器
    private static ApplicationContext CONTEXT;
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        ApplicationContextHolder.CONTEXT=applicationContext;
    }


    public static <T> T getBean(Class<T> clazz){
        //从ioc容器中根据类型获取对应的bean
        return CONTEXT.getBean(clazz);
    }

    public static <T> T getBean(String name,Class<T> clazz){
        //从ioc容器中根据名称和类型获取对应的bean
        return CONTEXT.getBean(name,clazz);
    }

    public static Object getBean(String name){
        //从ioc容器中根据名称获取对应的bean
        return CONTEXT.getBean(name);
    }

    public static <T>Map<String ,T> getBeansOfType(Class<T> clazz){
        //从ioc容器中根据类型获取符合的所有bean
        return CONTEXT.getBeansOfType(clazz);
    }

    public static <A extends Annotation> A findAnnotationOnBean(String beanName,Class< A> annotionType){
        //查找bean是否有注解
        return CONTEXT.findAnnotationOnBean(beanName,annotionType);
    }

    //获取应用上下文实例
    public static ApplicationContext getInstance(){
        return CONTEXT;
    }
}
