package com.fffz.httpclient;

import android.content.Context;
import android.util.LruCache;

import com.fffz.httpclient.internal.db._Request;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by sigma on 2018/7/9.
 */
public class CacheManager {

    private static final CacheManager INSTANCE = new CacheManager();

    public static CacheManager getInstance() {
        return INSTANCE;
    }

    private CacheManager() {
    }

    private DBHelper dbHelper;
    private LruCache<String, _Request> lruCache;

    public void init(Context context, int memoryCacheSize, int diskCacheSize) {
        DBHelper.getInstance().init(context, diskCacheSize);
        dbHelper = DBHelper.getInstance();
        lruCache = new LruCache<String, _Request>(memoryCacheSize) {
            @Override
            protected int sizeOf(String key, _Request _request) {
                return _request.getBytes();
            }
        };
    }

    public _Request query(String cacheKey, Map<String, ?> params, Map<String, ?> headers, CacheControl cacheControl) {
        if (cacheControl.noCache) {
            return null;
        }
        _Request _request = lruCache.get(cacheKey);
        if (_request == null) {
            _request = dbHelper.query(cacheKey);
            if (_request == null) {
                return null;
            }
            lruCache.put(cacheKey, _request);
        }
        if (System.currentTimeMillis() - _request.getUpdateTime() > cacheControl.maxAge * 1000) {
            return null;
        }
        Map<String, Object> copyParams = params == null ? null : new HashMap<>(params);
        Map<String, String> copyRequestParams = _request.getRequestParams() == null ? null : new HashMap<>(_request.getRequestParams());
        Map<String, ?> copyHeaders = headers == null ? null : new HashMap<>(headers);
        Map<String, String> copyRequestHeaders = _request.getRequestHeaders() == null ? null : new HashMap<>(_request.getRequestHeaders());
        if (!cacheControl.requestComparator.compare(copyParams, copyRequestParams, copyHeaders, copyRequestHeaders)) {
            return null;
        }
        dbHelper.updateTime(_request.getId());
        return _request;
    }

    public void update(String cacheKey, Map<String, ?> requestParams, Map<String, ?> requestHeaders, int responseCode, String responseBody, Map<String, String> responseHeaders, CacheControl cacheControl) {
        if (cacheControl.noStore) {
            return;
        }
        _Request _request = lruCache.get(cacheKey);
        if (_request == null) {
            _request = new _Request();
            lruCache.put(cacheKey, _request);
        }
        _request.setCacheKey(cacheKey);
        _request.setRequestParams(requestParams);
        _request.setRequestHeaders(requestHeaders);
        _request.setResponseCode(responseCode);
        _request.setResponseBody(responseBody);
        _request.setResponseHeaders(responseHeaders);
        _request.setPersistent(cacheControl.persistent);
        _request.setUpdateTime(System.currentTimeMillis());
        if (cacheControl.noDiskStore) {
            return;
        }
        dbHelper.update(_request);
    }

    public _Request get(String cacheKey) {
        _Request _request = lruCache.get(cacheKey);
        if (_request == null) {
            _request = dbHelper.query(cacheKey);
            if (_request != null) {
                lruCache.put(cacheKey, _request);
            }
        }
        return _request;
    }

    public long getCacheSize() {
        return dbHelper.getSize();
    }

    public void invalidate(String cacheKey) {
        lruCache.remove(cacheKey);
        dbHelper.delete(cacheKey);
    }

    public void clear() {
        dbHelper.clear();
    }

    public void delete(String cacheKey) {
        dbHelper.delete(cacheKey);
    }

    public void cancelPersistent(String cacheKey) {
        dbHelper.cancelPersistent(cacheKey);
    }

}