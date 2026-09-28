package com.guoxu.threadpool.support;

import org.springframework.cache.interceptor.CacheOperationInvoker;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * EagerThreadPoolExecutor 自定义特殊线程池
 *
 * @author 执笔画棠
 * @date 2025/11/04 17:44
 **/
// 自定义线程池类，继承自Java原生的ThreadPoolExecutor
// 相比原生线程池，该类实现了更"急切"的任务提交策略（如队列满时尝试重试提交，避免过早拒绝任务）
public class EagerThreadPoolExecutor extends ThreadPoolExecutor {


    // 自定义线程池的构造方法
    // 参数说明与父类ThreadPoolExecutor一致，用于初始化线程池核心参数
    // corePoolSize：核心线程数（线程池保持的最小线程数量）
    // maximumPoolSize：最大线程数（线程池允许创建的最大线程数量）
    // keepAliveTime：非核心线程的空闲存活时间（超过核心线程数的线程，空闲超过此时长会被销毁）
    // unit：keepAliveTime的时间单位（如秒、毫秒）
    // workQueue：任务队列（用于存放等待执行的任务，这里使用自定义的TaskQueue）
    // threadFactory：线程工厂（用于创建线程，可自定义线程名称、优先级等）
    // handler：拒绝策略（当线程池和队列都满时，处理新任务的策略）
    public EagerThreadPoolExecutor(int corePoolSize,
                                   int maximumPoolSize,
                                   long keepAliveTime,
                                   TimeUnit unit,
                                   TaskQueue<Runnable> workQueue,
                                   ThreadFactory threadFactory,
                                   RejectedExecutionHandler handler) {
        // 调用父类ThreadPoolExecutor的构造方法，初始化线程池基础配置
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory, handler);
    }

    // 用于统计"已提交到线程池"的任务总数（原子整数，保证多线程环境下计数的线程安全性）
    // 注意：该计数包含已执行、正在执行、等待执行的所有任务，任务执行完成后会减一
    private final AtomicInteger submittedTaskCount = new AtomicInteger(0);

    // 提供外部查询"已提交任务总数"的接口
    public int getSubmittedTaskCount() {
        return submittedTaskCount.get();
    }

    // 重写父类的afterExecute方法（线程池的钩子方法）
    // 该方法会在每个任务执行完成后被调用（无论任务正常执行还是抛出异常）
    @Override
    protected void afterExecute(Runnable r, Throwable t){
        // 任务执行完成后，将"已提交任务数"减一（保证计数准确性）
        submittedTaskCount.decrementAndGet();
    }

    // 重写父类的execute方法，自定义任务提交逻辑（核心增强点）
    // 作用：提交任务到线程池，并通过"重试入队"实现更急切的任务处理策略
    @Override
    public void execute(Runnable command){
        // 提交任务时，先将"已提交任务数"加一（记录新提交的任务）
        submittedTaskCount.incrementAndGet();

        try {
            // 调用父类ThreadPoolExecutor的execute方法，执行原生提交逻辑
            // （原生逻辑：先尝试用核心线程执行，核心线程满则入队，队列满则创建非核心线程，直到最大线程数，仍满则触发拒绝策略）
            super.execute(command);
        }catch (RejectedExecutionException ex){
            // 捕获"任务被拒绝"异常（通常是队列满且已达到最大线程数）
            // 将父类的任务队列强转为自定义的TaskQueue（因为构造方法中传入的是TaskQueue）
            TaskQueue<Runnable> taskQueue=(TaskQueue<Runnable>) super.getQueue();

            try {
                // 尝试通过TaskQueue的retryOffer方法重新提交任务（自定义的重试入队逻辑）
                // 参数：任务对象、超时时间0、时间单位毫秒（表示立即尝试，不等待）
                // 若retryOffer返回false，表示重试入队失败（队列仍满）
                if(!taskQueue.retryOffer(command,0,TimeUnit.MILLISECONDS)){
                    // 重试失败：将"已提交任务数"减一（因为任务最终未被接收）
                    submittedTaskCount.decrementAndGet();
                    // 抛出拒绝异常，说明队列确实已满
                    throw new RejectedExecutionException("queue capacity is full",ex);
                }
                // 若retryOffer返回true：表示任务已成功入队，此时submittedTaskCount无需调整（之前已加一）
            }catch (InterruptedException iex){
                // 捕获重试入队过程中的中断异常（如线程被中断）
                // 中断时任务未入队，将"已提交任务数"减一
                submittedTaskCount.decrementAndGet();
                // 包装中断异常并抛出
                throw new RejectedExecutionException(iex);
            }
        }catch (Exception ex){
            // 捕获其他未知异常（非拒绝异常）
            // 任务提交失败，将"已提交任务数"减一
            submittedTaskCount.decrementAndGet();
            // 抛出原始异常
            throw ex;
        }
    }
}
