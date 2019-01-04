package com.example.user.blugo;

import android.app.Application;
import android.content.Context;

/**
 * Created by user on 2016-07-06.
 */
public class App extends Application {
    private static Context appContext;

    @Override
    public void onCreate() {
        super.onCreate();
        appContext = this;
    }

    public static Context getAppContext() {
        return appContext;
    }
}
