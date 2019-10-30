package com.fffz.httpclient.sample;

import android.app.Activity;
import android.os.Bundle;

import com.fffz.httpclient.HttpCallback;
import com.fffz.httpclient.HttpClient;
import com.fffz.httpclient.HttpClientConfig;

import org.json.JSONObject;

public class MainActivity extends Activity {

    private static final String TAG = MainActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        HttpClient.getInstance().init(HttpClientConfig.createDefault(this));
        HttpClient.getInstance()
                .url("http://samples.openweathermap.org/data/2.5/weather")
                .addHeader("aaa", "AAA")
                .addParam("q", "London,uk")
                .addParam("appid", "b6907d289e10d714a6e88b30761fae22")
                .tag(this)
                .get(new HttpCallback<Weather, String>() {
                    @Override
                    protected void onFailure(int code, String data) {

                    }

                    @Override
                    protected void onError(Exception e) {

                    }

                    @Override
                    protected void onSuccess(int code, Weather data) {

                    }
                });

        HttpClient.getInstance()
                .url("https://api.github.com/")
                .tag(this)
                .get(new HttpCallback<JSONObject, String>() {
                    @Override
                    protected void onFailure(int code, String data) {

                    }

                    @Override
                    protected void onError(Exception e) {

                    }

                    @Override
                    protected void onSuccess(int code, JSONObject data) {

                    }
                });
    }

    @Override
    protected void onDestroy() {
        HttpClient.getInstance().cancel(this);
        super.onDestroy();
    }
}