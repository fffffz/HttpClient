package com.fffz.httpclient;

public abstract class ProgressListener {

    private boolean isCanceled;

    void cancel() {
        isCanceled = true;
    }

    public boolean isCanceled() {
        return isCanceled;
    }

    public abstract void onProgress(long progress, long max);

}
