package com.fffz.httpclient.internal.db;

import android.database.Cursor;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by sigma on 2018/6/29.
 */
public class _RequestHeader {

    public static final String TABLE_NAME = "header";
    public static final String ID = "id";
    public static final String REQUEST_ID = "request_id";
    public static final String NAME = "name";
    public static final String VALUE = "value";

    public static final String CREATE_TABLE_SQL = "CREATE TABLE IF NOT EXISTS " + TABLE_NAME +
            " (" + ID + " INTEGER PRIMARY KEY, " + REQUEST_ID + " INTEGER, "
            + NAME + " TEXT, " + VALUE + " TEXT)";

    private int id;
    private int requestId;
    private String name;
    private String value;

    public int getId() {
        return id;
    }

    public int getRequestId() {
        return requestId;
    }

    public String getName() {
        return name;
    }

    public String getValue() {
        return value;
    }

    public static Map<String, String> convert2Map(Cursor cursor) {
        try {
            Map<String, String> map = null;
            while (cursor.moveToNext()) {
                if (map == null) {
                    map = new HashMap<>();
                }
                String name = cursor.getString(cursor.getColumnIndex(NAME));
                String value = cursor.getString(cursor.getColumnIndex(VALUE));
                map.put(name, value);
            }
            return map;
        } finally {
            cursor.close();
        }
    }

}