package com.fffz.httpclient;

import java.io.IOException;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import okio.Buffer;
import okio.BufferedSink;
import okio.ForwardingSink;
import okio.Okio;
import okio.Sink;

class ProgressRequestBody extends RequestBody {

    private final RequestBody requestBody;
    private final HttpRequest request;
    private final ProgressListener listener;
    private BufferedSink bufferedSink;

    public ProgressRequestBody(RequestBody requestBody, HttpRequest request, ProgressListener listener) {
        this.requestBody = requestBody;
        this.request = request;
        this.listener = listener;
    }

    @Override
    public MediaType contentType() {
        return requestBody.contentType();
    }

    @Override
    public long contentLength() {
        try {
            return requestBody.contentLength();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return -1;
    }

    @Override
    public void writeTo(BufferedSink sink) {
        try {
            if (sink instanceof Buffer) {
                this.requestBody.writeTo(sink);
            } else {
                if (bufferedSink == null) {
                    CountingSink countingSink = new CountingSink(sink);
                    bufferedSink = Okio.buffer(countingSink);
                }
                this.requestBody.writeTo(bufferedSink);
                bufferedSink.flush();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private final class CountingSink extends ForwardingSink {
        private long writtenLength;
        private SchedulerTask task;

        public CountingSink(Sink delegate) {
            super(delegate);
        }

        @Override
        public void write(Buffer source, long byteCount) throws IOException {
            super.write(source, byteCount);
            writtenLength += byteCount;
            if (listener == null || listener.isCanceled()) {
                return;
            }
            if (task != null) {
                Schedulers.mainThread().cancel(task);
            }
            task = new SchedulerTask(request, new Runnable() {
                @Override
                public void run() {
                    listener.onProgress(writtenLength, contentLength());
                }
            });
            Schedulers.mainThread().schedule(task);
        }
    }

}