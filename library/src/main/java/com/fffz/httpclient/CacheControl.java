package com.fffz.httpclient;

import android.support.annotation.IntDef;
import android.text.TextUtils;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Map;

/**
 * Created by sigma on 2018/7/4.
 */
public class CacheControl {

    public static CacheControl createDefault() {
        return new CacheControl.Builder().build();
    }

    public final boolean noCache;
    public final boolean noDiskStore;
    public final boolean noStore;
    public final boolean persistent;
    public final long maxAge;
    public final int readCacheWhen;
    public final RequestComparator requestComparator;
    public final ResponseComparator responseComparator;

    public CacheControl(Builder builder) {
        noCache = builder.noCache;
        noDiskStore = builder.noDiskStore;
        noStore = builder.noStore;
        persistent = builder.persistent;
        maxAge = builder.maxAge;
        readCacheWhen = builder.readCacheWhen;
        requestComparator = builder.requestComparator;
        responseComparator = builder.responseComparator;
    }

    public static class Builder {
        private boolean noCache;
        private boolean noDiskStore;
        private boolean noStore;
        private boolean persistent;
        private long maxAge = Integer.MAX_VALUE;
        private int readCacheWhen = ON_ERROR;
        private RequestComparator requestComparator;
        private ResponseComparator responseComparator;

        public Builder noCache() {
            noCache = true;
            return this;
        }

        public Builder noDiskStore() {
            noDiskStore = true;
            return this;
        }

        public Builder noStore() {
            noStore = true;
            return this;
        }

        public Builder persistent() {
            persistent = true;
            return this;
        }

        public Builder maxAge(long seconds) {
            maxAge = seconds;
            return this;
        }

        public Builder readCacheWhen(@ReadCacheWhen int when) {
            readCacheWhen = when;
            return this;
        }

        public Builder requestComparator(RequestComparator comparator) {
            this.requestComparator = comparator;
            return this;
        }

        public Builder responseComparator(ResponseComparator responseComparator) {
            this.responseComparator = responseComparator;
            return this;
        }

        public CacheControl build() {
            if (requestComparator == null) {
                requestComparator = new RequestComparator();
            }
            return new CacheControl(this);
        }
    }

    public static final int ON_ERROR = 0;
    public static final int AT_ONCE = 1;

    @IntDef({ON_ERROR, AT_ONCE})
    @Retention(RetentionPolicy.SOURCE)
    public @interface ReadCacheWhen {
    }

    public static class RequestComparator {
        public boolean compare(Map<String, ?> params, Map<String, String> cacheParams,
                               Map<String, ?> headers, Map<String, String> cacheHeaders) {
            if (params != null && params.size() > 0) {
                if (cacheParams == null || cacheParams.size() < params.size()) {
                    return false;
                }
                for (String key : params.keySet()) {
                    String value = String.valueOf(params.get(key));
                    String _value = cacheParams.get(key);
                    if (!TextUtils.equals(value, _value)) {
                        return false;
                    }
                }
            }
            if (headers != null && headers.size() > 0) {
                if (cacheHeaders == null || cacheHeaders.size() < headers.size()) {
                    return false;
                }
                for (String key : headers.keySet()) {
                    String value = String.valueOf(headers.get(key));
                    String _value = cacheHeaders.get(key);
                    if (!TextUtils.equals(value, _value)) {
                        return false;
                    }
                }
            }
            return true;
        }
    }

    public static class ResponseComparator<T> {
        public boolean compare(T data, T cacheData, Map<String, ?> headers, Map<String, String> cacheHeaders) {
            if (data == null && cacheData != null) {
                return false;
            }
            if (data != null && cacheData == null) {
                return false;
            }
            if (headers != null && headers.size() > 0) {
                if (cacheHeaders == null || cacheHeaders.size() < headers.size()) {
                    return false;
                }
                for (String key : headers.keySet()) {
                    String value = String.valueOf(headers.get(key));
                    String _value = cacheHeaders.get(key);
                    if (!TextUtils.equals(value, _value)) {
                        return false;
                    }
                }
            }
            return true;
        }
    }

}