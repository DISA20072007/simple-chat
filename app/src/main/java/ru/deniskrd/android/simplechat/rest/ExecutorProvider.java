package ru.deniskrd.android.simplechat.rest;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

public class ExecutorProvider {
    private static final String IDLE_THREAD_NAME = "RestClient-";
    private static int threadIndex = 0;

    private static ThreadFactory customThreadPoolFactory = r -> new Thread(() -> {
        Thread.currentThread().setPriority(Thread.MIN_PRIORITY);
        r.run();
    }, IDLE_THREAD_NAME + (threadIndex++));

    public static Executor defaultHttpExecutor() {
        return Executors.newCachedThreadPool(customThreadPoolFactory);
    }
}