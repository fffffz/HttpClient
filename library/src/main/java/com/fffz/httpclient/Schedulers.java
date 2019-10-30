package com.fffz.httpclient;


import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import java.util.ArrayList;
import java.util.WeakHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Created by sigma on 2018/6/8.
 */
public class Schedulers {

    private static class MainThreadSchedulerHolder {
        static final Scheduler INSTANCE = new Scheduler() {
            Handler handler = new Handler(Looper.getMainLooper());

            @Override
            public void schedule(SchedulerTask task) {
                synchronized (task.request.tag) {
                    if (task.request.isCanceled()) {
                        return;
                    }
                    if (Looper.myLooper() == Looper.getMainLooper()) {
                        task.run();
                        return;
                    }
                    Message msg = Message.obtain(handler, task);
                    msg.obj = task.request.tag;
                    handler.sendMessage(msg);
                }
            }

            @Override
            public void cancel(Object tag) {
                synchronized (tag) {
                    handler.removeCallbacksAndMessages(tag);
                }
            }

            @Override
            public void cancel(SchedulerTask task) {
                synchronized (task.request.tag) {
                    handler.removeCallbacks(task);
                }
            }
        };
    }

    private static class IoSchedulerHolder {
        static final Scheduler INSTANCE = new Scheduler() {
            ExecutorService threadPool = new ThreadPoolExecutor(0, Integer.MAX_VALUE,
                    10L, TimeUnit.SECONDS,
                    new SynchronousQueue<Runnable>());
            WeakHashMap<Object, ArrayList<Future>> tagFuturesMap = new WeakHashMap<>();

            @Override
            public void schedule(SchedulerTask task) {
                synchronized (task.request.tag) {
                    if (task.request.isCanceled()) {
                        return;
                    }
                    Future future = threadPool.submit(task);
                    ArrayList<Future> futures = tagFuturesMap.get(task.request.tag);
                    if (futures == null) {
                        futures = new ArrayList<>();
                        tagFuturesMap.put(task.request.tag, futures);
                    }
                    futures.add(future);
                }
            }

            @Override
            public void cancel(Object tag) {
                synchronized (tag) {
                    ArrayList<Future> futures = tagFuturesMap.get(tag);
                    if (futures != null) {
                        for (Future future : futures) {
                            future.cancel(true);
                        }
                    }
                    tagFuturesMap.remove(tag);
                }
            }

            @Override
            public void cancel(SchedulerTask task) {
                cancel(task.request.tag);
            }
        };
    }

    public static Scheduler mainThread() {
        return MainThreadSchedulerHolder.INSTANCE;
    }

    public static Scheduler io() {
        return IoSchedulerHolder.INSTANCE;
    }

    public static void cancel(Object tag) {
        synchronized (tag) {
            mainThread().cancel(tag);
            io().cancel(tag);
        }
    }

}