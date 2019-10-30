package com.fffz.httpclient;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;

public interface RetryStrategy {

    void handleOnError(HttpClient httpClient, HttpRequest request, Exception e);

    void cancel();

    class RetryOnNetworkConnect implements RetryStrategy {

        private static volatile HandlerThread sHandlerThread = new HandlerThread("RetryOnNetworkConnect");

        static {
            sHandlerThread.start();
        }

        private static final long VALUE_NOT_SET = Long.MIN_VALUE;

        private Context context;
        private BroadcastReceiver receiver;
        /**
         * wait for network connection
         */
        private long timeout;
        private Handler handler;

        public RetryOnNetworkConnect() {
            this(VALUE_NOT_SET);
        }

        public RetryOnNetworkConnect(long timeout) {
            this.timeout = timeout;
            handler = new Handler(sHandlerThread.getLooper()) {
                @Override
                public void handleMessage(Message msg) {
                    cancel();
                }
            };
        }

        @Override
        public void handleOnError(final HttpClient httpClient, final HttpRequest request, Exception e) {
            context = httpClient.getConfig().context;
            if (Utils.isNetworkConnected(context) || receiver != null) {
                return;
            }
            receiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    if (!Utils.isNetworkConnected(context)) {
                        return;
                    }
                    httpClient.retry(request);
                    context.unregisterReceiver(receiver);
                    receiver = null;
                }
            };
            context.registerReceiver(receiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
            if (timeout != VALUE_NOT_SET) {
                handler.sendEmptyMessageDelayed(0, timeout);
            }
        }

        @Override
        public void cancel() {
            handler.removeCallbacksAndMessages(null);
            if (context == null || receiver == null) {
                return;
            }
            context.unregisterReceiver(receiver);
            receiver = null;
        }
    }

}