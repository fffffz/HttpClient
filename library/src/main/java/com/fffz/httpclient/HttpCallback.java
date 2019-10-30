package com.fffz.httpclient;

import java.util.Map;

/**
 * Created by sigma on 2018/4/10.
 */
public abstract class HttpCallback<T, F> {

    public static final int VALUE_NOT_SET = Integer.MIN_VALUE;

    protected HttpRequest request;
    protected int responseCode = VALUE_NOT_SET;
    protected String responseBody;
    protected Map<String, String> responseHeaders;
    protected T successData;
    protected F failureData;
    protected Exception exception;
    protected boolean cached;

    public <T extends HttpRequest> T getRequest() {
        return (T) request;
    }

    protected void setRequest(HttpRequest request) {
        this.request = request;
    }

    public int getResponseCode() {
        return responseCode;
    }

    protected void setResponseCode(int responseCode) {
        this.responseCode = responseCode;
    }

    public String getResponseBody() {
        return responseBody;
    }

    void setResponseBody(String responseBody) {
        this.responseBody = responseBody;
    }

    public Map<String, String> getResponseHeaders() {
        return responseHeaders;
    }

    void setResponseHeaders(Map<String, String> responseHeaders) {
        this.responseHeaders = responseHeaders;
    }

    public T getSuccessData() {
        return successData;
    }

    void setSuccessData(T successData) {
        this.successData = successData;
    }

    public F getFailureData() {
        return failureData;
    }

    void setFailureData(F failureData) {
        this.failureData = failureData;
    }

    public Exception getException() {
        return exception;
    }

    void setException(Exception exception) {
        this.exception = exception;
    }

    protected void dispatchOnCreate() {
        onCreate();
    }

    protected void onCreate() {
    }

    protected void dispatchOnRestart(int retryCount) {
        onRestart(retryCount);
    }

    protected void onRestart(int retryCount) {
    }

    protected void dispatchOnStart() {
        onStart();
    }

    protected void onStart() {
    }

    protected void dispatchOnCache(int code, T data) {
        onCache(code, data);
    }

    protected void onCache(int code, T data) {
    }

    protected void dispatchOnSuccess(int code, T data) {
        onSuccess(code, data);
    }

    protected abstract void onSuccess(int code, T data);

    protected void dispatchOnFailure(int code, F data) {
        onFailure(code, data);
    }

    protected abstract void onFailure(int code, F data);

    protected void dispatchOnError(Exception e) {
        onError(e);
    }

    protected abstract void onError(Exception e);

    protected void dispatchOnFinal() {
        onFinal();
    }

    protected void onFinal() {
    }

    public boolean isSuccessful(String responseBody) {
        return responseCode >= 200 && responseCode < 300;
    }

}