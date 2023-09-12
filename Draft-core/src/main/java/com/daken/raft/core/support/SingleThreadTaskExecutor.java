package com.daken.raft.core.support;

import lombok.NonNull;

import java.util.concurrent.*;

/**
 * SingleThreadTaskExecutor
 */
public class SingleThreadTaskExecutor implements TaskExecutor {

    private final ExecutorService executorService;

    public SingleThreadTaskExecutor() {
        this(Executors.defaultThreadFactory());
    }

    public SingleThreadTaskExecutor(String name) {
        this(r -> new Thread(r, name));
    }

    private SingleThreadTaskExecutor(ThreadFactory threadFactory) {
        executorService = Executors.newSingleThreadExecutor(threadFactory);
    }

    @NonNull
    @Override
    public Future<?> submit(@NonNull Runnable task) {
        return executorService.submit(task);
    }

    @NonNull
    @Override
    public <V> Future<V> submit(@NonNull Callable<V> task) {
        return executorService.submit(task);
    }

    @Override
    public void shutdown() throws InterruptedException {
        executorService.shutdown();
        executorService.awaitTermination(1, TimeUnit.SECONDS);
    }
}
