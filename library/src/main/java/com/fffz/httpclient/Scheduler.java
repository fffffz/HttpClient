package com.fffz.httpclient;

/**
 * Created by sigma on 2018/6/8.
 */
public interface Scheduler<T extends SchedulerTask> {

    void schedule(T task);

    void cancel(Object tag);

    void cancel(SchedulerTask task);
}
