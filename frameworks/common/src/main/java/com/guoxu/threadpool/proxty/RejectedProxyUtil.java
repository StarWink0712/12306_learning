// 声明当前类所在的包路径，用于组织代码结构
package com.guoxu.threadpool.proxty;

// 导入lombok的NoArgsConstructor注解，用于自动生成无参构造器
import com.guoxu.toolkit.ThreadUtil;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

// 导入Java反射包中的Proxy类，用于创建动态代理对象
import java.lang.reflect.Proxy;
// 导入链表阻塞队列，作为线程池的任务队列
import java.util.concurrent.LinkedBlockingQueue;
// 导入拒绝策略接口，定义线程池任务拒绝时的处理逻辑
import java.util.concurrent.RejectedExecutionHandler;
// 导入线程池核心类，用于创建和管理线程池
import java.util.concurrent.ThreadPoolExecutor;
// 导入时间单位枚举，用于线程池参数中的时间单位设置
import java.util.concurrent.TimeUnit;
// 导入原子长整型，用于线程安全地统计拒绝次数（避免并发问题）
import java.util.concurrent.atomic.AtomicLong;

/**
 * RejectedProxyUtil
 * 拒绝策略代理工具类
 * 作用：通过动态代理增强线程池的拒绝策略，实现对拒绝次数的统计等额外功能
 *
 * @author 执笔画棠
 * @date 2025/11/04 17:31
 **/
// lombok注解：生成无参构造器，访问级别为PRIVATE（私有）
// 目的：工具类通常不需要实例化，私有构造器防止外部通过new创建对象
@NoArgsConstructor(access = AccessLevel.PRIVATE)
// 声明最终类（不可被继承），作为拒绝策略代理的工具类
public final class RejectedProxyUtil {

    /**
     * 创建拒绝策略的代理对象
     * 作用：对原始拒绝策略进行增强（如统计拒绝次数），不修改原始拒绝策略逻辑
     *
     * @param rejectedExecutionHandler 原始的拒绝策略对象（被代理的目标对象）
     * @param rejectedNum 原子长整型变量，用于线程安全地记录拒绝策略的执行次数
     * @return 增强后的拒绝策略代理对象（实现了RejectedExecutionHandler接口）
     */
    public static RejectedExecutionHandler createProxy(RejectedExecutionHandler rejectedExecutionHandler,
                                                       AtomicLong rejectedNum) {
        // 通过Proxy类的newProxyInstance方法创建动态代理对象
        return (RejectedExecutionHandler) Proxy
                .newProxyInstance(
                        // 第一个参数：类加载器，使用目标对象的类加载器（保证类加载一致性）
                        rejectedExecutionHandler.getClass().getClassLoader(),
                        // 第二个参数：代理对象需要实现的接口数组（这里指定为RejectedExecutionHandler接口，保证代理对象可强转为该接口）
                        new Class[]{RejectedExecutionHandler.class},
                        // 第三个参数：调用处理器（自定义的RejectedProxyInvocationHandler），负责处理代理对象的方法调用
                        // 作用：在执行原始拒绝策略逻辑前后，添加统计拒绝次数等增强逻辑
                        new RejectedProxyInvocationHandler(rejectedExecutionHandler, rejectedNum)
                );
    }

    /**
     * 主方法：测试拒绝策略代理工具类的功能
     * 作用：模拟线程池任务被拒绝的场景，验证代理是否正确统计拒绝次数
     */
    public static void main(String[] args) {
        // 创建线程池对象
        // 参数说明：
        // 核心线程数：1（线程池保持的最小线程数）
        // 最大线程数：3（线程池允许的最大线程数）
        // 空闲线程存活时间：1024秒（超过核心线程数的线程，空闲超过此时长会被销毁）
        // 时间单位：秒（配合上一个参数使用）
        // 工作队列：容量为1的LinkedBlockingQueue（用于存放等待执行的任务）
        ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(1, 3, 1024, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(1));

        // 创建原始拒绝策略对象：AbortPolicy（线程池默认拒绝策略，直接抛出RejectedExecutionException）
        ThreadPoolExecutor.AbortPolicy abortPolicy = new ThreadPoolExecutor.AbortPolicy();

        // 创建原子长整型变量，用于统计拒绝次数（线程安全，多线程环境下计数准确）
        AtomicLong rejectedNum = new AtomicLong();

        // 通过工具类创建增强后的拒绝策略代理对象
        RejectedExecutionHandler proxyRejectedExecutionHandler = RejectedProxyUtil.createProxy(abortPolicy,
                rejectedNum);

        // 将代理后的拒绝策略设置到线程池中（替代原始拒绝策略）
        threadPoolExecutor.setRejectedExecutionHandler(proxyRejectedExecutionHandler);

        // 循环提交5个任务到线程池，触发拒绝策略
        // 原因：线程池核心线程1 + 最大线程3（可额外创建2个非核心线程） + 队列容量1 → 最大可处理1+2+1=4个任务
        // 提交5个任务时，第5个任务会被拒绝
        for (int i = 0; i < 5; i++) {
            try {
                // 向线程池提交任务：任务逻辑为让当前线程睡眠100000毫秒（约16分钟），模拟耗时任务
                // 目的：让提交的任务长时间占用线程，使线程池无法快速处理新任务，从而触发拒绝策略
                threadPoolExecutor.execute(() -> ThreadUtil.sleep(100000L));
            } catch (Exception ignored) {
                // 捕获拒绝策略抛出的异常（AbortPolicy会抛RejectedExecutionException）
                // 此处仅打印异常栈轨迹，不做额外处理
                ignored.printStackTrace();
            }
        }

        // 打印线程池拒绝策略的执行次数（预期为1次，因为第5个任务被拒绝）
        System.out.println("================ 线程池拒绝策略执行次数: " + rejectedNum.get());
    }
}