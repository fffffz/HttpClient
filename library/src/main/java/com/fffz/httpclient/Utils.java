package com.fffz.httpclient;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.text.TextUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;

class Utils {

    public static String fixHttpsUnderline(final String url) {
        if (TextUtils.isEmpty(url)) {
            return url;
        }
        String https = "https://";
        int httpsLen = https.length();
        if (https.regionMatches(true, 0, url, 0, httpsLen)) {
            String cropHttpsUrl = url.substring(httpsLen, url.length());
            int index = url.indexOf("/");
            if (index > 0) {
                String host = cropHttpsUrl.substring(0, index);
                if (host.contains("_")) {
                    return "http://" + cropHttpsUrl;
                }
            }
        }
        return url;
    }

    public static boolean isNetworkConnected(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo network = cm.getActiveNetworkInfo();
        if (network != null) {
            return network.isConnectedOrConnecting();
        }
        return false;
    }

    public static boolean equals(Map map, Map another) {
        return (map == null && another == null) || (map != null && another != null && map.equals(another));
    }

    public static byte[] getBitmapBytes(Bitmap bitmap, int quality) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, baos);
        byte[] bytes = baos.toByteArray();
        try {
            baos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return bytes;
    }

}
