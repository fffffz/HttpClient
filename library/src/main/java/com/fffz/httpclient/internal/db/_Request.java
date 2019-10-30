package com.fffz.httpclient.internal.db;

import android.database.Cursor;
import android.text.TextUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Created by sigma on 2018/6/29.
 */
public class _Request {

    public static final String TABLE_NAME = "request";
    public static final String ID = "id";
    public static final String CACHE_KEY = "cache_key";
    public static final String RESPONSE_CODE = "response_code";
    public static final String RESPONSE_BODY = "response_body";
    public static final String RESPONSE_HEADERS = "response_headers";
    public static final String PERSISTENT = "persistent";
    public static final String UPDATE_TIME = "update_time";
    public static final String CREATE_TABLE_SQL;

    static {
        StringBuilder sb = new StringBuilder("CREATE TABLE IF NOT EXISTS ").append(TABLE_NAME).append(" (")
                .append(ID).append(" INTEGER PRIMARY KEY, ")
                .append(CACHE_KEY).append(" TEXT, ");
        sb.append(RESPONSE_CODE).append(" INTEGER, ")
                .append(RESPONSE_BODY).append(" TEXT, ")
                .append(RESPONSE_HEADERS).append(" TEXT, ")
                .append(PERSISTENT).append(" INTEGER, ")
                .append(UPDATE_TIME).append(" TEXT)");
        CREATE_TABLE_SQL = sb.toString();
    }

    private int id;
    private String cacheKey;
    private Map<String, String> requestParams;
    private Map<String, String> requestHeaders;
    private int responseCode;
    private String responseBody;
    private Map<String, String> responseHeaders;
    private boolean persistent;
    private long updateTime;

    public static _Request convert(Cursor cursor) {
        try {
            if (!cursor.moveToFirst()) {
                return null;
            }
            _Request request = new _Request();
            request.id = cursor.getInt(cursor.getColumnIndex(ID));
            request.cacheKey = cursor.getString(cursor.getColumnIndex(CACHE_KEY));
            request.responseCode = cursor.getInt(cursor.getColumnIndex(RESPONSE_CODE));
            request.responseBody = cursor.getString(cursor.getColumnIndex(RESPONSE_BODY));
            request.persistent = cursor.getInt(cursor.getColumnIndex(PERSISTENT)) == 1;
            request.updateTime = cursor.getLong(cursor.getColumnIndex(UPDATE_TIME));
            String headers = cursor.getString(cursor.getColumnIndex(RESPONSE_HEADERS));
            if (!TextUtils.isEmpty(headers)) {
                try {
                    JSONObject jsonObject = new JSONObject(headers);
                    Iterator<String> iterator = jsonObject.keys();
                    while (iterator.hasNext()) {
                        String key = iterator.next();
                        String value = jsonObject.getString(key);
                        if (request.responseHeaders == null) {
                            request.responseHeaders = new HashMap<>();
                        }
                        request.responseHeaders.put(key, value);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            return request;
        } finally {
            cursor.close();
        }
    }

    public int getId() {
        return id;
    }

    public String getCacheKey() {
        return cacheKey;
    }

    public Map<String, String> getRequestParams() {
        return requestParams;
    }

    public Map<String, String> getRequestHeaders() {
        return requestHeaders;
    }

    public int getResponseCode() {
        return responseCode;
    }

    public String getResponseBody() {
        return responseBody;
    }

    public Map<String, String> getResponseHeaders() {
        return responseHeaders;
    }

    public long getUpdateTime() {
        return updateTime;
    }


    public void setCacheKey(String cacheKey) {
        this.cacheKey = cacheKey;
    }

    public void setResponseCode(int responseCode) {
        this.responseCode = responseCode;
    }

    public void setResponseBody(String responseBody) {
        this.responseBody = responseBody;
    }

    public void setResponseHeaders(Map<String, String> responseHeaders) {
        this.responseHeaders = responseHeaders;
    }

    public boolean isPersistent() {
        return persistent;
    }

    public void setPersistent(boolean persistent) {
        this.persistent = persistent;
    }

    public void setUpdateTime(long updateTime) {
        this.updateTime = updateTime;
    }

    public void setRequestParams(Map<String, ?> requestParams) {
        if (requestParams == null || requestParams.size() == 0) {
            return;
        }
        if (this.requestParams == null) {
            this.requestParams = new HashMap<>();
        }
        for (Map.Entry<String, ?> entry : requestParams.entrySet()) {
            this.requestParams.put(entry.getKey(), String.valueOf(entry.getValue()));
        }
    }

    public void setRequestHeaders(Map<String, ?> requestHeaders) {
        if (requestHeaders == null || requestHeaders.size() == 0) {
            return;
        }
        if (this.requestHeaders == null) {
            this.requestHeaders = new HashMap<>();
        }
        for (Map.Entry<String, ?> entry : requestHeaders.entrySet()) {
            this.requestHeaders.put(entry.getKey(), String.valueOf(entry.getValue()));
        }
    }

    public int getBytes() {
        int size = 20;
        if (cacheKey != null) {
            size += cacheKey.getBytes().length;
        }
        if (requestParams != null) {
            for (Map.Entry<String, String> entry : requestParams.entrySet()) {
                size += entry.getKey().getBytes().length;
                size += entry.getValue().getBytes().length;
            }
        }
        if (requestHeaders != null) {
            for (Map.Entry<String, String> entry : requestHeaders.entrySet()) {
                size += entry.getKey().getBytes().length;
                size += entry.getValue().getBytes().length;
            }
        }
        if (responseBody != null) {
            size += responseBody.getBytes().length;
        }
        if (responseHeaders != null) {
            for (Map.Entry<String, String> entry : responseHeaders.entrySet()) {
                size += entry.getKey().getBytes().length;
                size += entry.getValue().getBytes().length;
            }
        }
        return size;
    }

}