package com.example.m3app;

import android.content.Context;
import android.content.SharedPreferences;

/** 本地配置持久化：监听端口（默认 8080） */
public final class Prefs {
    private static final String FILE = "deepseek_prefs";
    private static final String KEY_PORT = "port";
    public static final int DEFAULT_PORT = 8080;

    private Prefs() {}

    private static SharedPreferences sp(Context ctx) {
        return ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }

    public static int getPort(Context ctx) {
        int port = sp(ctx).getInt(KEY_PORT, DEFAULT_PORT);
        if (port < 1024 || port > 65535) {
            return DEFAULT_PORT;
        }
        return port;
    }

    public static void setPort(Context ctx, int port) {
        sp(ctx).edit().putInt(KEY_PORT, port).apply();
    }
}
