package com.fffz.httpclient;

/**
 * Created by sigma on 2018/6/9.
 */
public class SchedulerTask implements Runnable {

    public final HttpRequest request;
    private final Runnable runnable;

    public SchedulerTask(HttpRequest request, Runnable runnable) {
        this.request = request;
        this.runnable = runnable;
    }

    @Override
    public void run() {
        synchronized (request.tag) {
            if (request.isCanceled()) {
                return;
            }
            runnable.run();
        }
    }

}