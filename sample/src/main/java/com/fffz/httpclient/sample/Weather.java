package com.fffz.httpclient.sample;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by sigma on 2018/4/11.
 */
public class Weather {
    private HashMap<String, Double> coord;
    private ArrayList<HashMap<String, Object>> weather;
    private long dt;

    public HashMap<String, Double> getCoord() {
        return coord;
    }

    public void setCoord(HashMap<String, Double> coord) {
        this.coord = coord;
    }

    public ArrayList<HashMap<String, Object>> getWeather() {
        return weather;
    }

    public void setWeather(ArrayList<HashMap<String, Object>> weather) {
        this.weather = weather;
    }

    public long getDt() {
        return dt;
    }

    public void setDt(long dt) {
        this.dt = dt;
    }
}
