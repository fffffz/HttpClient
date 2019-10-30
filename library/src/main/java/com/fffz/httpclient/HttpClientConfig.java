package com.fffz.httpclient;

import android.content.Context;

import java.util.concurrent.TimeUnit;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSession;

import okhttp3.OkHttpClient;

/**
 * Created by sigma on 2018/7/5.
 */
public class HttpClientConfig {

    private static HttpClientConfig sDefaultConfig;

    public final Context context;
    public final OkHttpClient okHttpClient;
    public final int memoryCacheSize;
    public final int diskCacheSize;

    public HttpClientConfig(Builder builder) {
        context = builder.context;
        if (builder.okHttpClient == null) {
            okHttpClient = createDefaultOkHttpClient();
        } else {
            okHttpClient = builder.okHttpClient;
        }
        memoryCacheSize = builder.memoryCacheSize;
        diskCacheSize = builder.diskCacheSize;
    }

    public static synchronized HttpClientConfig createDefault(Context context) {
        if (sDefaultConfig == null) {
            sDefaultConfig = new Builder(context.getApplicationContext()).build();
        }
        return sDefaultConfig;
    }

    private static OkHttpClient createDefaultOkHttpClient() {
        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .writeTimeout(15, TimeUnit.SECONDS)
                .hostnameVerifier(new HostnameVerifier() {
                    @Override
                    public boolean verify(String hostname, SSLSession session) {
                        return true;
                    }
                })
                .build();
        okHttpClient.dispatcher().setMaxRequests(64);
        okHttpClient.dispatcher().setMaxRequestsPerHost(5);
        return okHttpClient;
    }

    public static final class Builder {
        private Context context;
        private OkHttpClient okHttpClient;
        private int memoryCacheSize = 1024 * 1024 * 10;
        private int diskCacheSize = 1024 * 1024 * 100;

        public Builder(Context context) {
            this.context = context;
        }

        public Builder okHttpClient(OkHttpClient okHttpClient) {
            this.okHttpClient = okHttpClient;
            return this;
        }

        public Builder memoryCacheSize(int cacheSize) {
            this.memoryCacheSize = cacheSize;
            return this;
        }

        public Builder diskCacheSize(int cacheSize) {
            this.diskCacheSize = cacheSize;
            return this;
        }

        public HttpClientConfig build() {
            return new HttpClientConfig(this);
        }

    }

}