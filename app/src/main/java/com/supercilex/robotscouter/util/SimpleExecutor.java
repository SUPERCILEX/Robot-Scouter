package com.supercilex.robotscouter.util;

import java.util.concurrent.Executor;

public class SimpleExecutor implements Executor {
    @Override
    public void execute(Runnable runnable) {
        new Thread(runnable).start();
    }
}
