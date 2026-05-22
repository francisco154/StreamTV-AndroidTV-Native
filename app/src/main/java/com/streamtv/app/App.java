package com.streamtv.app;

import android.app.Application;
import android.content.Context;

public class App extends Application {
    private static App instance;
    private static final String PREFS_NAME = "streamtv_prefs";
    private static final String KEY_LOGGED_IN = "logged_in";
    private static final String JSON_URL = "https://demotester-v2.vercel.app/api/export/categories.json";

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
    }

    public static App getInstance() {
        return instance;
    }

    public static String getJsonUrl() {
        return JSON_URL;
    }

    public boolean isLoggedIn() {
        return getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getBoolean(KEY_LOGGED_IN, false);
    }

    public void setLoggedIn(boolean loggedIn) {
        getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(KEY_LOGGED_IN, loggedIn)
                .apply();
    }
}
