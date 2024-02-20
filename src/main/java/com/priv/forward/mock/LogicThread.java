package com.priv.forward.mock;

/**
 * 业务线程
 */
public class LogicThread extends Thread {

    public static LogicThread getRpcCrossThread() {
        return new LogicThread();
    }

    public static LogicThread getEventThread() {
        return new LogicThread();
    }

    public static LogicThread getLogicThread(long ignoredUid) {
        return new LogicThread();
    }

    public void summit(Runnable runnable) {
        runnable.run();
    }
}
