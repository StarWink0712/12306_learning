package com.guoxu.threadpool.support;

import lombok.Setter;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * TaskQueue 自定义任务队列
 *
 * @author 执笔画棠
 * @date 2025/11/04 17:45
 **/
// 自定义任务队列类，继承自LinkedBlockingQueue（链表结构的阻塞队列）
// 泛型声明：R是Runnable的子类，限定队列中只能存放可运行的任务
public class TaskQueue <R extends Runnable> extends LinkedBlockingQueue<Runnable> {
    // 关联的自定义线程池实例（EagerThreadPoolExecutor）
    // @Setter是Lombok注解，自动生成该字段的setter方法，用于设置关联的线程池
    @Setter
    private EagerThreadPoolExecutor executor;

    // 构造方法：创建指定容量的任务队列
    // 参数capacity：队列的最大容量（最多可存放的任务数量）
    public TaskQueue(int capacity) {
        // 调用父类LinkedBlockingQueue的构造方法，初始化队列容量
        super(capacity);
    }

    // 重写父类的offer方法（非阻塞入队，成功返回true，失败返回false）
    // 自定义入队逻辑，配合EagerThreadPoolExecutor实现"急切创建线程"的策略
    @Override
    public boolean offer(Runnable runnable){
        // 获取当前线程池中已创建的线程数量（包括核心线程和非核心线程）
        int currentPoolThreadSize = executor.getPoolSize();

        // 核心逻辑1：判断是否有空闲的核心线程
        // 已提交的任务数（submittedTaskCount） < 当前线程数 → 说明存在空闲线程（线程数多于正在处理的任务数）
        if(executor.getSubmittedTaskCount() < currentPoolThreadSize){
            // 此时将任务加入队列，让空闲线程去处理（无需创建新线程）
            return super.offer(runnable);
        }

        // 核心逻辑2：若没有空闲线程，判断是否还能创建新线程（未达最大线程数）
        if(currentPoolThreadSize < executor.getMaximumPoolSize()){
            // 返回false → 告诉线程池"入队失败"，促使线程池创建新线程处理该任务
            // （配合ThreadPoolExecutor的逻辑：offer失败时会尝试创建非核心线程，直到最大线程数）
            return false;
        }

        // 核心逻辑3：若已达最大线程数，只能将任务入队（若队列满则后续会触发拒绝策略）
        return super.offer(runnable);
    }

    // 重试将任务入队的方法（带超时参数）
    // 用于EagerThreadPoolExecutor中任务被拒绝时，重新尝试入队
    public boolean retryOffer(Runnable o, long timeout, TimeUnit unit) throws InterruptedException {
        // 若线程池已关闭，直接抛出拒绝异常（关闭后不接受新任务）
        if(executor.isShutdown()){
            throw new RejectedExecutionException("Executor is shutdown");
        }
        // 调用父类的带超时offer方法：尝试在指定时间内将任务入队
        // 若超时仍未入队则返回false，否则返回true
        return super.offer(o, timeout, unit);
    }
}
