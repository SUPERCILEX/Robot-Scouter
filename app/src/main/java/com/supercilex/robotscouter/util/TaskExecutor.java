package com.supercilex.robotscouter.util;

import android.support.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;

import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;

public class TaskExecutor<TResult> implements Executor, OnCompleteListener<TResult> {
    private static Map<Callable, TaskExecutor> sInstances = new ConcurrentHashMap<>();

    private Callable<TResult> mCallable;

    protected TaskExecutor(Callable<TResult> callable) {
        mCallable = callable;
    }

    public static <TResult> Task<TResult> execute(Callable<TResult> callable) {
        TaskExecutor<TResult> executor = new TaskExecutor<>(callable);
        sInstances.put(callable, executor);
        return Tasks.call(executor, callable).addOnCompleteListener(executor, executor);
    }

    public static Executor getCurrentExecutor(Callable callable) {
        return sInstances.get(callable);
    }

    @Override
    public void onComplete(@NonNull Task<TResult> task) {
        sInstances.remove(mCallable);
    }

    @Override
    public void execute(Runnable runnable) {
        new Thread(runnable).start();
    }
}
