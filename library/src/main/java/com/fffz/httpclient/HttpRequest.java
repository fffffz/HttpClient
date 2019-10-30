package com.fffz.httpclient;

import android.graphics.Bitmap;
import android.text.TextUtils;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Response;

/**
 * Created by sigma on 2018/4/9.
 */
public class HttpRequest {

    public final String url;
    public final String method;
    public final Map<String, Object> headers;
    public final Map<String, Object> urlParams;
    public final Map<String, Object> params;
    public final Map<String, BytesContent> bytesContents;
    public final Map<String, FileContent> fileContents;
    public final Map<String, BitmapContent> bitmapContents;
    public final Object tag;
    public final HttpCallback callback;
    public final Scheduler scheduler;
    protected Call call;
    private boolean isCanceled;
    public final String cacheKey;
    public final CacheControl cacheControl;
    public final RetryStrategy retryStrategy;
    public final String jsonBody;
    public final int timeoutMs;
    long enqueueTime;
    int retryCount;
    HttpClient.OkHttpCallback okHttpCallback;
    Runnable timeoutRunnable;

    public HttpRequest(Builder builder) {
        this.method = builder.method;
        this.headers = builder.headers;
        this.urlParams = builder.urlParams;
        this.params = builder.params;
        this.bytesContents = builder.bytesContents;
        this.fileContents = builder.fileContents;
        this.bitmapContents = builder.bitmapContents;
        this.tag = builder.tag != null ? builder.tag : new Object();
        this.scheduler = builder.scheduler;
        this.callback = builder.callback;
        this.cacheControl = builder.cacheControl;
        this.retryStrategy = builder.retryStrategy;
        this.jsonBody = builder.jsonBody;
        this.timeoutMs = builder.timeoutMs;
        this.cacheKey = builder.cacheKey;
        String url = builder.url;
        if (method.equals("GET")) {
            url = getUrlWithParams(url, params, urlParams);
        } else {
            url = getUrlWithParams(url, urlParams);
        }
        if (!url.startsWith("http")) {
            url = builder.baseUrl + url;
        }
        this.url = url;
    }

    public void addHeader(String name, Object value) {
        if (value != null) {
            headers.put(name, value);
        }
    }

    public void addParam(String name, Object value) {
        if (value != null) {
            params.put(name, value);
        }
    }

    protected void cancel() {
        isCanceled = true;
        if (retryStrategy != null) {
            retryStrategy.cancel();
        }
        if (bytesContents != null) {
            for (BytesContent content : bytesContents.values()) {
                if (content.listener != null) {
                    content.listener.cancel();
                }
            }
            for (FileContent content : fileContents.values()) {
                if (content.listener != null) {
                    content.listener.cancel();
                }
            }
            for (BitmapContent content : bitmapContents.values()) {
                if (content.listener != null) {
                    content.listener.cancel();
                }
            }
        }
    }

    public boolean isCanceled() {
        return isCanceled;
    }

    @Override
    public String toString() {
        return url + " " + params + " " + headers;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof HttpRequest) {
            HttpRequest another = (HttpRequest) obj;
            return TextUtils.equals(method, another.method)
                    && Utils.equals(headers, another.headers)
                    && Utils.equals(urlParams, another.urlParams)
                    && Utils.equals(params, another.params)
                    && tag == another.tag
                    && ((callback == null && another.callback == null) || (callback != null && another.callback != null && callback.getClass() == another.callback.getClass()))
                    && TextUtils.equals(jsonBody, another.jsonBody)
                    && TextUtils.equals(cacheKey, another.cacheKey)
                    && TextUtils.equals(url, another.url);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return (method + headers + urlParams + params + tag + (callback == null ? null : callback.getClass()) + jsonBody + cacheKey + url).hashCode();
    }

    public static class Builder<T extends HttpClient> {
        protected T httpClient;
        protected String baseUrl = "";
        protected String cacheKey;
        protected String url;
        protected String method = "GET";
        protected Map<String, Object> headers = new HashMap<>();
        protected Map<String, Object> params = new HashMap<>();
        protected Map<String, Object> urlParams = new HashMap<>();
        protected Map<String, BytesContent> bytesContents = new HashMap<>();
        protected Map<String, FileContent> fileContents = new HashMap<>();
        protected Map<String, BitmapContent> bitmapContents = new HashMap<>();
        protected Object tag;
        protected HttpCallback callback;
        protected Scheduler scheduler;
        protected CacheControl cacheControl;
        protected RetryStrategy retryStrategy;
        protected String jsonBody;
        protected int timeoutMs = 15000;
        protected String[] cacheKeyParams;

        public Builder(T httpClient) {
            this.httpClient = httpClient;
        }

        public Builder baseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
            return this;
        }

        public Builder url(String url) {
            url = Utils.fixHttpsUnderline(url);
            if (TextUtils.isEmpty(cacheKey)) {
                if (!url.startsWith("http")) {
                    this.cacheKey = this.baseUrl + url;
                } else {
                    this.cacheKey = url;
                }
            }
            this.url = url;
            return this;
        }

        public Builder cacheKeyParams(String... params) {
            this.cacheKeyParams = params;
            return this;
        }

        public Builder cacheKey(String cacheKey) {
            this.cacheKey = cacheKey;
            return this;
        }

        public Builder headers(Map<String, ?> headers) {
            this.headers.clear();
            this.headers.putAll(headers);
            return this;
        }

        public Builder params(Map<String, ?> params) {
            this.params.clear();
            this.params.putAll(params);
            return this;
        }

        public Builder urlParams(Map<String, ?> params) {
            this.urlParams.clear();
            this.urlParams.putAll(params);
            return this;
        }

        public Builder addContent(String name, String filename, Bitmap bitmap) {
            return addContent(name, filename, bitmap, 100, null);
        }

        public Builder addContent(String name, String filename, Bitmap bitmap, int quality) {
            return addContent(name, filename, bitmap, quality, null);
        }

        public Builder addContent(String name, String filename, Bitmap bitmap, int quality, ProgressListener listener) {
            if (bitmap != null) {
                bitmapContents.put(name, new BitmapContent(filename, bitmap, quality, listener));
            }
            return this;
        }

        public Builder addContent(String name, String filename, byte[] content) {
            return addContent(name, filename, content, null);
        }

        public Builder addContent(String name, String filename, byte[] content, ProgressListener listener) {
            if (content != null) {
                bytesContents.put(name, new BytesContent(filename, content, listener));
            }
            return this;
        }

        public Builder addContent(String name, String filename, File file) {
            return addContent(name, filename, file, null);
        }

        public Builder addContent(String name, String filename, File file, ProgressListener listener) {
            if (file != null) {
                fileContents.put(name, new FileContent(filename, file, listener));
            }
            return this;
        }

        public Builder addHeader(String name, Object value) {
            if (value != null) {
                headers.put(name, value);
            }
            return this;
        }

        public Builder addParam(String name, Object value) {
            if (value != null) {
                params.put(name, value);
            }
            return this;
        }

        public Builder addUrlParam(String name, Object value) {
            if (value != null) {
                urlParams.put(name, value);
            }
            return this;
        }

        /**
         * HttpRequest's default tag is self
         */
        public Builder tag(Object tag) {
            this.tag = tag;
            return this;
        }

        public Builder callbackOn(Scheduler scheduler) {
            this.scheduler = scheduler;
            return this;
        }

        public Builder cacheControl(CacheControl cacheControl) {
            this.cacheControl = cacheControl;
            return this;
        }

        public Builder retryStrategy(RetryStrategy retryStrategy) {
            this.retryStrategy = retryStrategy;
            return this;
        }

        public Builder jsonBody(String jsonBody) {
            this.jsonBody = jsonBody;
            return this;
        }

        public Builder timeoutMs(int timeoutMs) {
            this.timeoutMs = timeoutMs;
            return this;
        }

        public HttpRequest get() {
            return get(null);
        }

        public HttpRequest get(HttpCallback callback) {
            method = "GET";
            this.callback = callback;
            generateCacheKey();
            HttpRequest request = new HttpRequest(this);
            httpClient.request(request);
            return request;
        }

        public HttpRequest post() {
            return post(null);
        }

        public HttpRequest post(HttpCallback callback) {
            method = "POST";
            this.callback = callback;
            generateCacheKey();
            HttpRequest request = new HttpRequest(this);
            httpClient.request(request);
            return request;
        }

        public Response getSync() throws IOException {
            method = "GET";
            generateCacheKey();
            HttpRequest request = new HttpRequest(this);
            return httpClient.requestImmediately(request);
        }

        public Response postSync() throws IOException {
            method = "POST";
            generateCacheKey();
            HttpRequest request = new HttpRequest(this);
            return httpClient.requestImmediately(request);
        }

        public String generateCacheKey() {
            if (TextUtils.isEmpty(cacheKey)) {
                return cacheKey;
            }
            StringBuilder sb = new StringBuilder(url);
            if (cacheKeyParams == null) {
                cacheKey = sb.toString();
                return cacheKey;
            }
            sb.append(" ");
            for (String paramKey : cacheKeyParams) {
                Object paramValue = urlParams.get(paramKey);
                if (paramValue == null) {
                    paramValue = params.get(paramKey);
                    if (paramValue == null) {
                        continue;
                    }
                }
                sb.append(paramKey).append("=").append(paramValue);
            }
            this.cacheKey = sb.toString();
            return cacheKey;
        }
    }

    protected static class BytesContent {
        public final String filename;
        public final byte[] content;
        public final ProgressListener listener;

        public BytesContent(String filename, byte[] content, ProgressListener listener) {
            this.filename = filename;
            this.content = content;
            this.listener = listener;
        }
    }

    protected static class FileContent {
        public final String filename;
        public final File content;
        public final ProgressListener listener;

        public FileContent(String filename, File content, ProgressListener listener) {
            this.filename = filename;
            this.content = content;
            this.listener = listener;
        }
    }

    protected static class BitmapContent {
        public final String filename;
        public final Bitmap content;
        public final int quality;
        public final ProgressListener listener;

        public BitmapContent(String filename, Bitmap content, int quality, ProgressListener listener) {
            this.filename = filename;
            this.content = content;
            this.quality = quality;
            this.listener = listener;
        }
    }

    private static String getUrlWithParams(String url, Map<String, Object>... params) {
        StringBuilder sb = new StringBuilder(url);
        if (url.contains("?")) {
            sb.append("&");
        } else {
            sb.append("?");
        }
        for (Map<String, Object> paramMap : params) {
            for (Map.Entry<String, Object> entry : paramMap.entrySet()) {
                String value = toString(entry.getValue());
                sb.append(entry.getKey()).append("=").append(value).append("&");
            }
        }
        return sb.substring(0, sb.length() - 1);
    }

    private static String toString(Object obj) {
        return obj == null ? "" : obj.toString();
    }

}