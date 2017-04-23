package com.supercilex.robotscouter.util;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;

import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public enum AsyncTaskExecutor implements Executor {
    INSTANCE;

    private ExecutorService mService = Executors.newCachedThreadPool();

    public static <TResult> Task<TResult> execute(Callable<TResult> callable) {
        return Tasks.call(INSTANCE, callable);
    }

    @Override
    public void execute(Runnable runnable) {
        mService.submit(runnable);
    }
}
