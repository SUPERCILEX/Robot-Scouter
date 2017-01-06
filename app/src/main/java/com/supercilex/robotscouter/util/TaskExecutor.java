package com.supercilex.robotscouter.util;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;

import java.util.concurrent.Callable;
import java.util.concurrent.Executor;

public class TaskExecutor implements Executor {
    public static <TResult> Task<TResult> execute(Callable<TResult> callable) {
        return Tasks.call(new TaskExecutor(), callable);
    }

    @Override
    public void execute(Runnable runnable) {
        new Thread(runnable).start();
    }
}
