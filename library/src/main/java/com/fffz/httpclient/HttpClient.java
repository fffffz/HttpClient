package com.fffz.httpclient;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;

import com.fffz.httpclient.internal.db._Request;
import com.google.gson.Gson;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.FormBody;
import okhttp3.Headers;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.internal.http.HttpMethod;
import okio.Buffer;

/**
 * Created by sigma on 2018/4/9.
 */
public class HttpClient<T extends HttpRequest> {

    private static final HttpClient INSTANCE = new HttpClient();

    public static HttpClient getInstance() {
        return INSTANCE;
    }

    private static final String TAG = HttpClient.class.getSimpleName();
    private static final MediaType MEDIA_TYPE_JSON = MediaType.parse("application/json");
    private static final MediaType MEDIA_TYPE_MULTIPART = MediaType.parse("multipart/form-data");

    private final Map<Object, ConcurrentHashSet<T>> tagRequestMap = new WeakHashMap<>();
    private final ConcurrentHashMap<HttpRequest, HttpRequest> toRetryRequests = new ConcurrentHashMap<>();
    private HttpClientConfig config;
    private OkHttpClient okHttpClient;
    private OkHttpClient contentOkHttpClient;
    private CacheManager cacheManager;
    private final Handler handler;

    protected HttpClient() {
        HandlerThread handlerThread = new HandlerThread("HttpClient Handler");
        handlerThread.start();
        handler = new Handler(handlerThread.getLooper());
    }

    public synchronized void init(HttpClientConfig config) {
        if (this.config == config) {
            return;
        }
        this.config = config;
        okHttpClient = config.okHttpClient;
        contentOkHttpClient = okHttpClient.newBuilder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .build();
        CacheManager.getInstance().init(config.context, config.memoryCacheSize, config.diskCacheSize);
        cacheManager = CacheManager.getInstance();
    }

    public HttpRequest.Builder tag(Object tag) {
        return new HttpRequest.Builder(this).tag(tag);
    }

    protected HttpRequest.Builder baseUrl(String baseUrl) {
        return new HttpRequest.Builder(this).baseUrl(baseUrl);
    }

    public HttpRequest.Builder url(String url) {
        return new HttpRequest.Builder(this).url(url);
    }

    private ExecutorService threadPool = Executors.newCachedThreadPool();

    public Response requestImmediately(T request) throws IOException {
        Call call = newCall(request, getOkHttpClient(request));
        return call.execute();
    }

    public OkHttpClient getOkHttpClient() {
        return okHttpClient;
    }

    public OkHttpClient getOkHttpClient(HttpRequest request) {
        if (request.fileContents.size() > 0 || request.bytesContents.size() > 0 || request.bitmapContents.size() > 0) {
            return contentOkHttpClient;
        }
        return okHttpClient;
    }

    void retry(final T request) {
        toRetryRequests.remove(request);
        if (request.callback != null) {
            request.callback.dispatchOnRestart(request.retryCount);
            request.retryCount++;
            request.callback.dispatchOnStart();
        }
        if (Looper.myLooper() == Looper.getMainLooper()) {
            threadPool.execute(new Runnable() {
                @Override
                public void run() {
                    requestInternal(request);
                }
            });
        } else {
            requestInternal(request);
        }
    }

    public void request(final T request) {
        if (request.callback != null) {
            request.retryCount = 0;
            request.callback.setRequest(request);
            request.callback.dispatchOnCreate();
            request.callback.dispatchOnStart();
        }
        if (Looper.myLooper() == Looper.getMainLooper()) {
            threadPool.execute(new Runnable() {
                @Override
                public void run() {
                    requestInternal(request);
                }
            });
        } else {
            requestInternal(request);
        }
    }

    private void requestInternal(final T request) {
        request.okHttpCallback = new OkHttpCallback(request);
        OkHttpClient okHttpClient = getOkHttpClient(request);
        Call call = newCall(request, okHttpClient);
        request.call = call;
        request.timeoutRunnable = new Runnable() {
            @Override
            public void run() {
                synchronized (request) {
                    if (request.isCanceled() || request.timeoutRunnable == null) {
                        return;
                    }
                    request.okHttpCallback.onFailure(request.call, new TimeoutException());
                }
            }
        };
        int timeout;
        if (okHttpClient == contentOkHttpClient) {
            timeout = Math.max(request.timeoutMs, 60000);
        } else {
            timeout = Math.max(request.timeoutMs, 500);
        }
        handler.postDelayed(request.timeoutRunnable, timeout);
        request.enqueueTime = System.currentTimeMillis();
        final CacheControl cacheControl = request.cacheControl;
        if (cacheControl != null && !cacheControl.noCache && cacheControl.readCacheWhen == CacheControl.AT_ONCE) {
            cached(request, cacheControl);
        }
//        if (BuildConfig.IS_DEBUG) {
//            String requestStr = request.toString();
//            if (request.jsonBody != null) {
//                requestStr = requestStr + " " + request.jsonBody;
//            }
//            Log.i(TAG, String.format("Send Request %s", requestStr));
//        }
        ConcurrentHashSet<T> requests = tagRequestMap.get(request.tag);
        if (requests == null) {
            requests = new ConcurrentHashSet<>();
            tagRequestMap.put(request.tag, requests);
        }
        requests.add(request);
        call.enqueue(request.okHttpCallback);
    }

    private void remove(HttpRequest request) {
        ConcurrentHashSet<T> requests = tagRequestMap.get(request.tag);
        if (requests == null) {
            return;
        }
        requests.remove(request);
        if (requests.size() == 0) {
            tagRequestMap.remove(request.tag);
        }
    }

    public Call newCall(HttpRequest request) {
        return newCall(request, getOkHttpClient(request));
    }

    private static Call newCall(HttpRequest request, OkHttpClient okHttpClient) {
        Request.Builder builder = new Request.Builder();
        if (HttpMethod.permitsRequestBody(request.method)) {
            if (request.fileContents.size() > 0 || request.bytesContents.size() > 0 || request.bitmapContents.size() > 0) {
                MultipartBody.Builder bodyBuilder = new MultipartBody.Builder().setType(MultipartBody.FORM);
                for (Map.Entry<String, HttpRequest.FileContent> entry : request.fileContents.entrySet()) {
                    HttpRequest.FileContent fileContent = entry.getValue();
                    RequestBody requestBody = RequestBody.create(MEDIA_TYPE_MULTIPART, fileContent.content);
                    if (fileContent.listener != null) {
                        requestBody = new ProgressRequestBody(requestBody, request, fileContent.listener);
                    }
                    bodyBuilder.addFormDataPart(entry.getKey(), fileContent.filename, requestBody);
                }
                for (Map.Entry<String, HttpRequest.BytesContent> entry : request.bytesContents.entrySet()) {
                    HttpRequest.BytesContent bytesContent = entry.getValue();
                    RequestBody requestBody = RequestBody.create(MEDIA_TYPE_MULTIPART, bytesContent.content);
                    if (bytesContent.listener != null) {
                        requestBody = new ProgressRequestBody(requestBody, request, bytesContent.listener);
                    }
                    bodyBuilder.addFormDataPart(entry.getKey(), bytesContent.filename, requestBody);
                }
                for (Map.Entry<String, HttpRequest.BitmapContent> entry : request.bitmapContents.entrySet()) {
                    HttpRequest.BitmapContent bitmapContent = entry.getValue();
                    byte[] bytes = Utils.getBitmapBytes(bitmapContent.content, bitmapContent.quality);
                    RequestBody requestBody = RequestBody.create(MEDIA_TYPE_MULTIPART, bytes);
                    if (bitmapContent.listener != null) {
                        requestBody = new ProgressRequestBody(requestBody, request, bitmapContent.listener);
                    }
                    bodyBuilder.addFormDataPart(entry.getKey(), bitmapContent.filename, requestBody);
                }
                for (Map.Entry<String, Object> entry : request.params.entrySet()) {
                    bodyBuilder.addFormDataPart(entry.getKey(), toString(entry.getValue()));
                }
                builder.post(bodyBuilder.build());
            } else if (request.jsonBody != null) {
                RequestBody body = RequestBody.create(MEDIA_TYPE_JSON, request.jsonBody);
                builder.post(body);
            } else {
                FormBody.Builder bodyBuilder = new FormBody.Builder();
                for (Map.Entry<String, Object> entry : request.params.entrySet()) {
                    bodyBuilder.add(entry.getKey(), toString(entry.getValue()));
                }
                builder.post(bodyBuilder.build());
            }
        } else {
            builder.get();
        }
        if (request.headers != null) {
            for (Map.Entry<String, Object> entry : request.headers.entrySet()) {
                builder.addHeader(entry.getKey(), toString(entry.getValue()));
            }
        }
        builder.url(request.url);
        Call call = okHttpClient.newCall(builder.build());
        return call;
    }

    public void cancel(Object tag) {
        ConcurrentHashSet<T> requests = tagRequestMap.get(tag);
        if (requests != null) {
            synchronized (tag) {
                for (T request : requests) {
                    request.cancel();
                    if (request.timeoutRunnable != null) {
                        handler.removeCallbacks(request.timeoutRunnable);
                        request.timeoutRunnable = null;
                    }
                    CacheControl cacheControl = request.cacheControl;
                    if (request.call != null && (cacheControl == null || (cacheControl.noDiskStore && cacheControl.noStore))) {
                        request.call.cancel();
                    }
                }
            }
            tagRequestMap.remove(tag);
        }
        Schedulers.cancel(tag);
    }

    public long getCacheSize() {
        return cacheManager.getCacheSize();
    }

    public void clearCache() {
        cacheManager.clear();
    }

    public void cancelPersistent(HttpRequest request) {
        cancelPersistent(request.cacheKey);
    }

    public void cancelPersistent(String cacheKey) {
        cacheManager.cancelPersistent(cacheKey);
    }

    class OkHttpCallback implements okhttp3.Callback {
        T request;

        OkHttpCallback(T request) {
            this.request = request;
        }

        @Override
        public void onFailure(Call call, final IOException e) {
            synchronized (request) {
                if (request.isCanceled() || request.timeoutRunnable == null) {
                    return;
                }
                handler.removeCallbacks(request.timeoutRunnable);
                request.timeoutRunnable = null;
                remove(request);
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                e.printStackTrace(pw);
//                if (BuildConfig.IS_DEBUG) {
//                    Request okRequest = call.request();
//                    Log.i(TAG, String.format("Error occurred %s %s %dms%n%s",
//                            okRequest.url(), requestBodyToString(okRequest.body()),
//                            System.currentTimeMillis() - request.enqueueTime, sw.toString()));
//                }
                final HttpCallback callback = request.callback;
                if (callback == null) {
                    return;
                }
                callback.setException(e);
                try {
                    RetryStrategy retryStrategy = request.retryStrategy;
                    if (retryStrategy != null) {
                        synchronized (request.tag) {
                            HttpRequest oldRequest = toRetryRequests.get(request);
                            if (oldRequest != null) {
                                oldRequest.retryStrategy.cancel();
                                toRetryRequests.remove(oldRequest);
                            }
                            toRetryRequests.put(request, request);
                            retryStrategy.handleOnError(HttpClient.this, request, e);
                        }
                    }
                    CacheControl cacheControl = request.cacheControl;
                    if (cacheControl != null &&
                            !cacheControl.noCache &&
                            cacheControl.readCacheWhen == CacheControl.ON_ERROR) {
                        if (cached(request, cacheControl)) {
                            return;
                        }
                    }
                    if (request.scheduler != null) {
                        request.scheduler.schedule(new SchedulerTask(request, new Runnable() {
                            @Override
                            public void run() {
                                callback.dispatchOnError(e);
                            }
                        }));
                    } else {
                        synchronized (request.tag) {
                            try {
                                callback.dispatchOnError(e);
                            } catch (Exception e1) {
                                e1.printStackTrace();
                            }
                        }
                    }
                } finally {
                    if (request.scheduler != null) {
                        request.scheduler.schedule(new SchedulerTask(request, new Runnable() {
                            @Override
                            public void run() {
                                callback.dispatchOnFinal();
                            }
                        }));
                    } else {
                        synchronized (request.tag) {
                            callback.dispatchOnFinal();
                        }
                    }
                }
            }
        }

        @Override
        public void onResponse(Call call, Response response) {
            synchronized (request) {
                String responseBody = null;
                try {
                    responseBody = response.body().string();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                int responseCode = response.code();
                Map<String, String> responseHeaders = new HashMap<>();
                Headers headers = response.headers();
                for (int i = 0; i < headers.size(); i++) {
                    responseHeaders.put(headers.name(i), headers.value(i));
                }
                if (request.isCanceled() || request.timeoutRunnable == null) {
                    store(request, responseCode, responseBody, responseHeaders);
                    return;
                }
                handler.removeCallbacks(request.timeoutRunnable);
                request.timeoutRunnable = null;
                remove(request);
//                if (BuildConfig.IS_DEBUG) {
//                    Request request = response.request();
//                    Log.i(TAG, String.format("Response for %s %s %dms%n%s",
//                            request.url(), requestBodyToString(request.body()),
//                            System.currentTimeMillis() - this.request.enqueueTime, responseBody));
//                }
                dispatchOnResponse(request, responseCode, responseBody, responseHeaders, false);
            }
        }
    }

    private boolean cached(T request, CacheControl cacheControl) {
        Map<String, Object> requestParams = new HashMap<>();
        requestParams.putAll(request.urlParams);
        requestParams.putAll(request.params);
        _Request _request = cacheManager.query(request.cacheKey, requestParams, request.headers, request.cacheControl);
        if (_request != null && System.currentTimeMillis() - _request.getUpdateTime() <= cacheControl.maxAge * 1000) {
            dispatchOnResponse(request, _request.getResponseCode(), _request.getResponseBody(), _request.getResponseHeaders(), true);
            return true;
        }
        return false;
    }

    private void dispatchOnResponse(final T request, int responseCode, String responseBody, Map<String, String> responseHeaders, boolean fromCache) {
        final HttpCallback callback = request.callback;
        if (callback == null) {
            store(request, responseCode, responseBody, responseHeaders);
            return;
        }
        callback.setResponseBody(responseBody);
        callback.setResponseCode(responseCode);
        final int code = callback.getResponseCode();
        callback.setResponseHeaders(responseHeaders);
        Scheduler scheduler = request.scheduler;
        try {
            Type genericSuperclass = callback.getClass().getGenericSuperclass();
            Type successGenericType;
            Type failureGenericType;
            while (genericSuperclass instanceof Class) {
                genericSuperclass = ((Class) genericSuperclass).getGenericSuperclass();
            }
            if (genericSuperclass == null) {
                successGenericType = String.class;
                failureGenericType = String.class;
            } else {
                ParameterizedType parameterizedType = (ParameterizedType) genericSuperclass;
                Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
                successGenericType = actualTypeArguments[0];
                if (successGenericType instanceof TypeVariable) {
                    successGenericType = ((TypeVariable) successGenericType).getBounds()[0];
                }
                failureGenericType = getFailureGenericType(parameterizedType);
            }
            if (callback.isSuccessful(responseBody)) {
                final Object data = toGenericType(responseBody, successGenericType);
                callback.setSuccessData(data);
                if (fromCache) {
                    callback.cached = true;
                    if (scheduler != null) {
                        scheduler.schedule(new SchedulerTask(request, new Runnable() {
                            @Override
                            public void run() {
                                callback.dispatchOnCache(code, data);
                            }
                        }));
                    } else {
                        synchronized (request.tag) {
                            try {
                                callback.dispatchOnCache(code, data);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }
                    return;
                }
                CacheControl cacheControl = request.cacheControl;
                if (cacheControl != null && callback.cached) {
                    _Request _request = cacheManager.get(request.cacheKey); //本次请求是否与上次相同，如果相同则不触发onSuccess
                    if (_request != null &&
                            responseCode == _request.getResponseCode() &&
                            TextUtils.equals(responseBody, _request.getResponseBody()) &&
                            (cacheControl.responseComparator == null ||
                                    cacheControl.responseComparator.compare(data, toGenericType(_request.getResponseBody(), successGenericType), responseHeaders, _request.getResponseHeaders()))
                    ) {
                        return;
                    }
                }
                store(request, responseCode, responseBody, responseHeaders);
                if (scheduler != null) {
                    scheduler.schedule(new SchedulerTask(request, new Runnable() {
                        @Override
                        public void run() {
                            callback.dispatchOnSuccess(code, data);
                        }
                    }));
                } else {
                    synchronized (request.tag) {
                        try {
                            callback.dispatchOnSuccess(code, data);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
                return;
            }
            if (fromCache) {
                return;
            }
            final Object data = toGenericType(responseBody, failureGenericType);
            callback.setFailureData(data);
            if (scheduler != null) {
                scheduler.schedule(new SchedulerTask(request, new Runnable() {
                    @Override
                    public void run() {
                        callback.dispatchOnFailure(code, data);
                    }
                }));
            } else {
                synchronized (request.tag) {
                    try {
                        callback.dispatchOnFailure(code, data);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        } catch (final Exception e) {
            callback.setException(e);
            if (fromCache) {
                return;
            }
            if (scheduler != null) {
                scheduler.schedule(new SchedulerTask(request, new Runnable() {
                    @Override
                    public void run() {
                        callback.dispatchOnError(e);
                    }
                }));
            } else {
                synchronized (request.tag) {
                    callback.dispatchOnError(e);
                }
            }
        } finally {
            if (fromCache) {
                return;
            }
            if (scheduler != null) {
                scheduler.schedule(new SchedulerTask(request, new Runnable() {
                    @Override
                    public void run() {
                        callback.dispatchOnFinal();
                    }
                }));
            } else {
                synchronized (request.tag) {
                    callback.dispatchOnFinal();
                }
            }
        }
    }

    private void store(T request, int responseCode, String responseBody, Map<String, String> responseHeaders) {
        CacheControl cacheControl = request.cacheControl;
        if (cacheControl == null || (cacheControl.noStore && cacheControl.noDiskStore)) {
            return;
        }
        Map<String, Object> requestParams = new HashMap<>();
        requestParams.putAll(request.urlParams);
        requestParams.putAll(request.params);
        cacheManager.update(request.cacheKey, requestParams, request.headers, responseCode, responseBody, responseHeaders, cacheControl);
    }

    private static Type getFailureGenericType(ParameterizedType parameterizedType) {
        Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
        if (actualTypeArguments.length == 2) {
            return actualTypeArguments[1];
        }
        Class clazz = (Class) parameterizedType.getRawType();
        Type genericSuperclass = clazz.getGenericSuperclass();
        parameterizedType = (ParameterizedType) genericSuperclass;
        return getFailureGenericType(parameterizedType);
    }

    static Object toGenericType(String string, Type genericType) {
        Object data = null;
        if (genericType == String.class) {
            data = string;
        } else if (genericType == JSONObject.class) {
            try {
                data = new JSONObject(string);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        } else {
            data = new Gson().fromJson(string, genericType);
        }
        return data;
    }

    static String requestBodyToString(RequestBody requestBody) {
        if (requestBody == null) {
            return "";
        }
        Buffer buffer = new Buffer();
        try {
            requestBody.writeTo(buffer);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return buffer.readUtf8();
    }

    public HttpClientConfig getConfig() {
        return config;
    }

    private static String toString(Object obj) {
        return obj == null ? "" : obj.toString();
    }

}