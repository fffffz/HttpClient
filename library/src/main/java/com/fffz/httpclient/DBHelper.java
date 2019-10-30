package com.fffz.httpclient;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.fffz.httpclient.internal.db._Request;
import com.fffz.httpclient.internal.db._RequestHeader;
import com.fffz.httpclient.internal.db._RequestParam;

import org.json.JSONObject;

import java.io.File;
import java.util.Map;

/**
 * Created by sigma on 2018/6/29.
 */
class DBHelper {

    private static final DBHelper INSTANCE = new DBHelper();

    static DBHelper getInstance() {
        return INSTANCE;
    }

    private InnerSQLiteOpenHelper innerSQLiteOpenHelper;
    private SQLiteDatabase readableDatabase;
    private SQLiteDatabase writableDatabase;
    private File dbFile;
    private int maxSize;

    private DBHelper() {
    }

    public synchronized void init(Context context, int maxSize) {
        this.maxSize = maxSize;
        if (innerSQLiteOpenHelper != null) {
            return;
        }
        innerSQLiteOpenHelper = new InnerSQLiteOpenHelper(context.getApplicationContext());
        dbFile = context.getDatabasePath(InnerSQLiteOpenHelper.DB_NAME);
    }

    private synchronized SQLiteDatabase getReadableDatabase() {
        if (readableDatabase == null) {
            readableDatabase = innerSQLiteOpenHelper.getReadableDatabase();
        }
        return readableDatabase;
    }

    private synchronized SQLiteDatabase getWritableDatabase() {
        if (writableDatabase == null) {
            writableDatabase = innerSQLiteOpenHelper.getWritableDatabase();
        }
        return writableDatabase;
    }

    public synchronized _Request query(String cacheKey) {
        _Request _request;
        Cursor cursor = getReadableDatabase().query(_Request.TABLE_NAME, null, _Request.CACHE_KEY + " = ? ", new String[]{cacheKey}, null, null, null);
        _request = _Request.convert(cursor);
        cursor.close();
        if (_request == null) {
            return null;
        }
        Cursor cursor2 = getReadableDatabase().query(_RequestParam.TABLE_NAME, null, _RequestParam.REQUEST_ID + " = ? ", new String[]{String.valueOf(_request.getId())}, null, null, null);
        Map<String, String> _params = _RequestParam.convert2Map(cursor2);
        _request.setRequestParams(_params);
        Cursor cursor3 = getReadableDatabase().query(_RequestHeader.TABLE_NAME, null, _RequestHeader.REQUEST_ID + " = ? ", new String[]{String.valueOf(_request.getId())}, null, null, null);
        Map<String, String> _headers = _RequestHeader.convert2Map(cursor3);
        _request.setRequestHeaders(_headers);
        cursor2.close();
        return _request;
    }

    public synchronized void update(_Request request) {
        ContentValues values = new ContentValues();
        values.put(_Request.CACHE_KEY, request.getCacheKey());
        values.put(_Request.RESPONSE_CODE, request.getResponseCode());
        values.put(_Request.RESPONSE_BODY, request.getResponseBody());
        values.put(_Request.UPDATE_TIME, request.getUpdateTime());
        JSONObject headers = new JSONObject(request.getResponseHeaders());
        values.put(_Request.RESPONSE_HEADERS, headers.toString());
        Cursor cursor = getReadableDatabase().query(_Request.TABLE_NAME, null, _Request.CACHE_KEY + " = ? ", new String[]{request.getCacheKey()}, null, null, null);
        long id;
        if (cursor.moveToFirst()) {
            id = cursor.getInt(cursor.getColumnIndex(_Request.ID));
            getWritableDatabase().delete(_RequestParam.TABLE_NAME, _RequestParam.REQUEST_ID + " = ?", new String[]{String.valueOf(id)});
            getWritableDatabase().delete(_RequestHeader.TABLE_NAME, _RequestHeader.REQUEST_ID + " = ?", new String[]{String.valueOf(id)});
            getWritableDatabase().update(_Request.TABLE_NAME, values, _Request.ID + " = ?", new String[]{String.valueOf(id)});
        } else {
            id = getWritableDatabase().insert(_Request.TABLE_NAME, null, values);
        }
        Map<String, String> requestParams = request.getRequestParams();
        if (requestParams != null) {
            for (Map.Entry<String, String> param : requestParams.entrySet()) {
                ContentValues values2 = new ContentValues();
                values2.put(_RequestParam.REQUEST_ID, id);
                values2.put(_RequestParam.NAME, param.getKey());
                values2.put(_RequestParam.VALUE, param.getValue());
                getWritableDatabase().insert(_RequestParam.TABLE_NAME, null, values2);
            }
        }
        Map<String, String> requestHeaders = request.getRequestHeaders();
        if (requestHeaders != null) {
            for (Map.Entry<String, String> header : requestHeaders.entrySet()) {
                ContentValues values2 = new ContentValues();
                values2.put(_RequestHeader.REQUEST_ID, id);
                values2.put(_RequestHeader.NAME, header.getKey());
                values2.put(_RequestHeader.VALUE, header.getValue());
                getWritableDatabase().insert(_RequestHeader.TABLE_NAME, null, values2);
            }
        }
        cursor.close();
        trimToSize();
    }

    public synchronized void updateTime(int id) {
        ContentValues values = new ContentValues();
        values.put(_Request.UPDATE_TIME, System.currentTimeMillis());
        getWritableDatabase().update(_Request.TABLE_NAME, values, _Request.ID + " = ?", new String[]{String.valueOf(id)});
    }

    public synchronized void delete(String cacheKey) {
        Cursor cursor = getReadableDatabase().query(_Request.TABLE_NAME, null, _Request.CACHE_KEY + " = ? ", new String[]{cacheKey}, null, null, null);
        if (cursor.moveToFirst()) {
            long id = cursor.getInt(cursor.getColumnIndex(_Request.ID));
            getWritableDatabase().delete(_Request.TABLE_NAME, _Request.ID + " = ?", new String[]{String.valueOf(id)});
            getWritableDatabase().delete(_RequestParam.TABLE_NAME, _RequestParam.REQUEST_ID + " = ?", new String[]{String.valueOf(id)});
            getWritableDatabase().delete(_RequestHeader.TABLE_NAME, _RequestHeader.REQUEST_ID + " = ?", new String[]{String.valueOf(id)});
        }
        cursor.close();
    }

    public synchronized long getSize() {
        return dbFile.length();
    }

    public synchronized void clear() {
        getWritableDatabase().delete(_Request.TABLE_NAME, null, null);
        getWritableDatabase().delete(_RequestParam.TABLE_NAME, null, null);
        getWritableDatabase().delete(_RequestHeader.TABLE_NAME, null, null);
    }

    public synchronized void cancelPersistent(String cacheKey) {
        ContentValues values = new ContentValues();
        values.put(_Request.PERSISTENT, 0);
        getWritableDatabase().update(_Request.TABLE_NAME, values, _Request.CACHE_KEY + " = ?", new String[]{cacheKey});
    }

    public synchronized void trimToSize() {
        boolean disallowDeletePersistent = true;
        while (dbFile.length() > maxSize) {
            Cursor cursor;
            if (disallowDeletePersistent) {
                cursor = getReadableDatabase().query(_Request.TABLE_NAME, new String[]{_Request.ID}, _Request.PERSISTENT + " = 0 ", null, null, null, _Request.UPDATE_TIME);
            } else {
                cursor = getReadableDatabase().query(_Request.TABLE_NAME, new String[]{_Request.ID}, null, null, null, null, _Request.UPDATE_TIME);
            }
            if (cursor.moveToFirst()) {
                int id = cursor.getInt(cursor.getColumnIndex(_Request.ID));
                getWritableDatabase().delete(_Request.TABLE_NAME, _Request.ID + " = ?", new String[]{String.valueOf(id)});
                getWritableDatabase().delete(_RequestParam.TABLE_NAME, _RequestParam.REQUEST_ID + " = ?", new String[]{String.valueOf(id)});
                getWritableDatabase().delete(_RequestHeader.TABLE_NAME, _RequestHeader.REQUEST_ID + " = ?", new String[]{String.valueOf(id)});
            } else if (disallowDeletePersistent) {
                disallowDeletePersistent = false;
            } else {
                clear();
                return;
            }
        }
    }

    private static class InnerSQLiteOpenHelper extends SQLiteOpenHelper {

        static final String DB_NAME = "com.ximalaya.ting.httpclient";
        static final int DB_VERSION = 1;

        public InnerSQLiteOpenHelper(Context context) {
            super(context, DB_NAME, null, DB_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL(_Request.CREATE_TABLE_SQL);
            db.execSQL(_RequestParam.CREATE_TABLE_SQL);
            db.execSQL(_RequestHeader.CREATE_TABLE_SQL);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        }
    }

}